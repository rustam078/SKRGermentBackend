package com.skr.erp.qr;

import com.skr.erp.common.constants.ProductUnitStatus;
import com.skr.erp.entity.InventoryBatch;
import com.skr.erp.entity.Product;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.qr.dto.GenerateUnitsRequest;
import com.skr.erp.qr.dto.GenerateUnitsResponse;
import com.skr.erp.qr.dto.ProductUnitResponse;
import com.skr.erp.repository.InventoryBatchRepository;
import com.skr.erp.repository.ProductMaterialCostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class QrUnitServiceImpl implements QrUnitService {

    private final ProductUnitRepository productUnitRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final ProductMaterialCostRepository productMaterialCostRepository;

    @Override
    public GenerateUnitsResponse generateUnits(String batchNumber, GenerateUnitsRequest request) {

        InventoryBatch batch = inventoryBatchRepository.findByBatchNumber(batchNumber)
                .orElseThrow(() -> new BusinessException("Batch " + batchNumber + " not found."));

        Product product = batch.getProduct();

        BigDecimal salePrice = currentSalePrice(product.getId());
        if (salePrice == null) {
            throw new BusinessException(
                    "Set a sale price for " + product.getName() + " before generating QR labels.");
        }

        int received = batch.getQuantityReceived().intValue();
        long existing = productUnitRepository.countByBatchNumber(batchNumber);

        int toGenerate = request != null && request.getCount() != null
                ? request.getCount()
                : (int) (received - existing);

        if (toGenerate <= 0) {
            throw new BusinessException("All pieces of this batch are already labelled.");
        }
        if (existing + toGenerate > received) {
            throw new BusinessException(
                    "Cannot label more than " + received + " pieces in this batch ("
                            + existing + " already labelled).");
        }

        int startIndex = productUnitRepository.maxSerialIndex(batchNumber) + 1;

        List<ProductUnit> created = new ArrayList<>();
        for (int i = 0; i < toGenerate; i++) {
            int index = startIndex + i;
            ProductUnit unit = new ProductUnit();
            unit.setSerial(batchNumber + "-" + String.format("%03d", index));
            unit.setBatchNumber(batchNumber);
            unit.setProduct(product);
            unit.setPrintedPrice(salePrice);
            unit.setStatus(ProductUnitStatus.AVAILABLE);
            created.add(unit);
        }
        productUnitRepository.saveAll(created);

        return GenerateUnitsResponse.builder()
                .batchNumber(batchNumber)
                .productName(product.getName())
                .generatedCount(created.size())
                .totalUnits(productUnitRepository.countByBatchNumber(batchNumber))
                .units(created.stream().map(this::toResponse).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductUnitResponse> listUnits(String batchNumber) {
        return productUnitRepository.findByBatchNumberOrderBySerialAsc(batchNumber)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductUnitResponse scanLookup(String code) {
        String serial = QrPayload.extractSerial(code);
        ProductUnit unit = productUnitRepository.findBySerial(serial)
                .orElseThrow(() -> new BusinessException("Unknown QR code: " + serial));

        // Include the batch cost so the sale form can show profit for a scanned line.
        BigDecimal unitCost = inventoryBatchRepository.findByBatchNumber(unit.getBatchNumber())
                .map(InventoryBatch::getUnitCost)
                .orElse(null);

        return toResponse(unit, unitCost);
    }

    @Override
    public ProductUnitResponse voidUnit(String serial) {
        ProductUnit unit = productUnitRepository.findBySerial(serial)
                .orElseThrow(() -> new BusinessException("Unit " + serial + " not found."));
        if (unit.getStatus() == ProductUnitStatus.SOLD) {
            throw new BusinessException("Sold units cannot be voided.");
        }
        unit.setStatus(ProductUnitStatus.VOID);
        productUnitRepository.save(unit);
        return toResponse(unit);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateUnitsForSale(String batchNumber, List<String> serials) {
        if (serials == null || serials.isEmpty()) {
            return;
        }
        for (String serial : serials) {
            ProductUnit unit = productUnitRepository.findBySerial(serial)
                    .orElseThrow(() -> new BusinessException("Unknown unit: " + serial));
            if (!unit.getBatchNumber().equals(batchNumber)) {
                throw new BusinessException(
                        "Unit " + serial + " does not belong to batch " + batchNumber + ".");
            }
            if (unit.getStatus() != ProductUnitStatus.AVAILABLE) {
                throw new BusinessException(
                        "Unit " + serial + " is already " + unit.getStatus() + ".");
            }
        }
    }

    @Override
    public void consumeForSale(String batchNumber, List<String> serials, UUID saleOrderId) {
        if (serials == null || serials.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (String serial : serials) {
            ProductUnit unit = productUnitRepository.findBySerial(serial)
                    .orElseThrow(() -> new BusinessException("Unknown unit: " + serial));

            if (!unit.getBatchNumber().equals(batchNumber)) {
                throw new BusinessException(
                        "Unit " + serial + " does not belong to batch " + batchNumber + ".");
            }
            if (unit.getStatus() != ProductUnitStatus.AVAILABLE) {
                throw new BusinessException(
                        "Unit " + serial + " is already " + unit.getStatus() + ".");
            }

            unit.setStatus(ProductUnitStatus.SOLD);
            unit.setSaleOrderId(saleOrderId);
            unit.setSoldAt(now);
            productUnitRepository.save(unit);
        }
    }

    @Override
    public int removeAvailableUnits(String batchNumber, int count) {
        if (count <= 0) {
            return 0;
        }
        List<ProductUnit> available = productUnitRepository
                .findByBatchNumberAndStatusOrderBySerialDesc(batchNumber, ProductUnitStatus.AVAILABLE);
        if (available.isEmpty()) {
            return 0;
        }
        // Drop the newest AVAILABLE labels first; never touch SOLD/VOID units.
        List<ProductUnit> toRemove = available.subList(0, Math.min(count, available.size()));
        productUnitRepository.deleteAll(toRemove);
        return toRemove.size();
    }

    private BigDecimal currentSalePrice(UUID productId) {
        return productMaterialCostRepository
                .findTopByProductIdAndSalePriceNotNullAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        productId, LocalDate.now())
                .map(mc -> mc.getSalePrice())
                .orElse(null);
    }

    private ProductUnitResponse toResponse(ProductUnit unit) {
        return toResponse(unit, null);
    }

    private ProductUnitResponse toResponse(ProductUnit unit, BigDecimal unitCost) {
        return ProductUnitResponse.builder()
                .id(unit.getId())
                .serial(unit.getSerial())
                .batchNumber(unit.getBatchNumber())
                .productId(unit.getProduct().getId())
                .productName(unit.getProduct().getName())
                .printedPrice(unit.getPrintedPrice())
                .unitCost(unitCost)
                .status(unit.getStatus().name())
                .qrPayload(QrPayload.build(unit.getSerial(), unit.getProduct().getName(), unit.getPrintedPrice()))
                .build();
    }
}
