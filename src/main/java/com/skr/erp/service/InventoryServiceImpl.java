package com.skr.erp.service;

import com.skr.erp.common.constants.InventoryBatchStatus;
import com.skr.erp.common.constants.ProductSource;
import com.skr.erp.common.response.PageResponse;
import com.skr.erp.dto.response.InventoryBatchResponse;
import com.skr.erp.dto.response.InventoryResponse;
import com.skr.erp.dto.response.LowStockAlertResponse;
import com.skr.erp.dto.response.ProductInventoryDetailResponse;
import com.skr.erp.entity.*;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.InventoryBatchRepository;
import com.skr.erp.repository.ProductMaterialCostRepository;
import com.skr.erp.repository.ProductRepository;
import com.skr.erp.repository.SystemSettingRepository;
import com.skr.erp.util.BatchNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryBatchRepository inventoryBatchRepository;
    private final BatchNumberGenerator generateBatchNumber;
    private final ProductMaterialCostRepository productMaterialCostRepository;
    private final SystemSettingRepository repository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void createPurchaseBatch(
            InvestmentItem investmentItem) {

        InventoryBatch batch = new InventoryBatch();
        batch.setBatchNumber(generateBatchNumber.generate());
        batch.setProduct(investmentItem.getProduct());
        batch.setSource(ProductSource.PURCHASED.name());
        batch.setSourceId(investmentItem.getId());
        batch.setReceivedDate(
                investmentItem.getInvestment().getPurchaseDate());
        batch.setQuantityReceived(
                investmentItem.getQuantity());

        batch.setQuantityAvailable(
                investmentItem.getQuantity());

        batch.setUnitCost(
                investmentItem.getRate());

        batch.setTotalCost(investmentItem.getTotalAmount());

        batch.setStatus(InventoryBatchStatus.ACTIVE);

        batch.setRemarks("Purchase Entry");

        inventoryBatchRepository.save(batch);
    }

    @Override
    @Transactional
    public void createProductionBatch(
            ProductionEntryDetail productionDetail) {

        InventoryBatch batch = new InventoryBatch();

        batch.setBatchNumber(generateBatchNumber.generate());

        batch.setProduct(productionDetail.getProduct());

        batch.setSource(ProductSource.MANUFACTURED.name());

        batch.setSourceId(
                productionDetail.getId());

        batch.setReceivedDate(
                productionDetail.getProductionEntry()
                        .getProductionDate());

        batch.setQuantityReceived(BigDecimal.valueOf(productionDetail.getQuantity()));

        batch.setQuantityAvailable(BigDecimal.valueOf(productionDetail.getQuantity()));

        // Temporary
        BigDecimal materialCost =
                getMaterialCost(productionDetail.getProduct());

        batch.setUnitCost(materialCost);

        batch.setTotalCost(
                materialCost.multiply(
                        BigDecimal.valueOf(
                                productionDetail.getQuantity())));

        batch.setStatus(
                InventoryBatchStatus.ACTIVE);

        batch.setRemarks("Production Entry");

        inventoryBatchRepository.save(batch);
    }


    public PageResponse<InventoryResponse> getInventory(Pageable pageable) {

        Page<InventoryBatch> inventoryBatches = inventoryBatchRepository.findAllWithProduct(pageable);

        Map<UUID, InventoryResponse> map = new LinkedHashMap<>();

        for (InventoryBatch b :inventoryBatches) {

            Product p = b.getProduct();

            InventoryResponse res = map.get(p.getId());

            if (res == null) {
                res = new InventoryResponse();
                res.setProductId(p.getId());
                res.setProductName(p.getName());
                res.setSource(p.getSource());
                res.setCurrentStock(BigDecimal.ZERO);
                res.setStockValue(BigDecimal.ZERO);

                map.put(p.getId(), res);
            }

            res.setCurrentStock(
                    res.getCurrentStock().add(b.getQuantityAvailable())
            );

            res.setStockValue(
                    res.getStockValue().add(
                            b.getQuantityAvailable().multiply(b.getUnitCost())
                    )
            );
        }

        List<InventoryResponse> list = map.values()
                .stream()
                .map(i -> {
                    i.setCurrentStock(scale2(i.getCurrentStock()));
                    i.setStockValue(scale2(i.getStockValue()));
                    i.setAverageCost(
                            i.getCurrentStock().compareTo(BigDecimal.ZERO) == 0
                                    ? BigDecimal.ZERO
                                    : scale2(i.getStockValue()
                                    .divide(i.getCurrentStock(), 2, RoundingMode.HALF_UP))
                    );
                    // Current effective selling price (if configured). Null means "not set",
                    // so the sale form can fall back to average cost.
                    i.setSellingPrice(getCurrentSalePrice(i.getProductId()));
                    return i;
                })
                .toList();

        return PageResponse.<InventoryResponse>builder()
                .content(list)
                .page(inventoryBatches.getNumber())
                .size(inventoryBatches.getSize())
                .totalElements(inventoryBatches.getTotalElements())
                .totalPages(inventoryBatches.getTotalPages())
                .last(inventoryBatches.isLast())
                .build();
    }

    private BigDecimal scale2(BigDecimal val) {
        return val == null
                ? BigDecimal.ZERO
                : val.setScale(2, RoundingMode.HALF_UP);
    }
    private BigDecimal getMaterialCost(Product product) {

        ProductMaterialCost cost =
                productMaterialCostRepository
                        .findTopByProductIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                product.getId(),
                                LocalDate.now())
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Material cost not configured for product: "
                                                + product.getName()));

        return cost.getCost();
    }

    // Current selling price = the sale_price on the latest pricing-history row
    // effective on or before today. Null-safe: returns null when none is set so
    // the sale form can fall back to average cost.
    private BigDecimal getCurrentSalePrice(UUID productId) {

        return productMaterialCostRepository
                .findTopByProductIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        productId,
                        LocalDate.now())
                .map(mc -> mc.getSalePrice() == null ? null : scale2(mc.getSalePrice()))
                .orElse(null);
    }


    public ProductInventoryDetailResponse getProductBatchDetails(
            UUID productId,
            LocalDate fromDate,
            LocalDate toDate) {

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException("fromDate cannot be after toDate");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found"));

        List<InventoryBatch> batches;
        if (fromDate == null && toDate == null) {
            batches = inventoryBatchRepository.findAllByProductId(productId);
        } else {
            LocalDate from = fromDate != null ? fromDate : LocalDate.of(1900, 1, 1);
            LocalDate to = toDate != null ? toDate : LocalDate.of(9999, 12, 31);
            batches = inventoryBatchRepository.findAllByProductIdAndReceivedDateBetween(
                    productId, from, to);
        }

        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalSold = BigDecimal.ZERO;
        BigDecimal quantityAvailable = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;

        List<InventoryBatchResponse> batchResponses = new ArrayList<>();

        for (InventoryBatch b : batches) {

            BigDecimal batchValue = b.getQuantityAvailable().multiply(b.getUnitCost());
            totalQuantity = totalQuantity.add(b.getQuantityReceived());
            quantityAvailable = quantityAvailable.add(b.getQuantityAvailable());
            totalValue = totalValue.add(batchValue);

            batchResponses.add(
                    InventoryBatchResponse.builder()
                            .batchId(b.getId())
                            .batchNumber(b.getBatchNumber())
                            .source(ProductSource.valueOf(b.getSource()))
                            .receivedDate(b.getReceivedDate())
                            .totalQuantity(b.getQuantityReceived())
                            .quantityAvailable(b.getQuantityAvailable())
                            .unitCost(b.getUnitCost())
                            .batchValue(batchValue)
                            .status(b.getStatus().name())
                            .build()
            );
        }
        totalSold = totalQuantity.subtract(quantityAvailable);
        return ProductInventoryDetailResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .source(product.getSource())
                .totalQuantity(totalQuantity)
                .totalSold(totalSold)
                .quantityAvailable(quantityAvailable)
                .totalValue(totalValue)
                .averageCost(
                        quantityAvailable.compareTo(BigDecimal.ZERO) == 0
                                ? BigDecimal.ZERO
                                : totalValue.divide(quantityAvailable, 2, RoundingMode.HALF_UP)
                )
                .batches(batchResponses)
                .build();
    }

    @Override
    @Transactional
    public InventoryBatchResponse adjustBatchStock(
            UUID batchId,
            com.skr.erp.dto.request.AdjustBatchStockRequest request) {

        InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("Batch not found"));

        BigDecimal qty = request.getQuantity();
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Quantity must be greater than zero");
        }

        String dir = request.getDirection() == null ? "" : request.getDirection().trim().toUpperCase();
        boolean increase = dir.equals("INCREASE");
        boolean decrease = dir.equals("DECREASE");
        if (!increase && !decrease) {
            throw new BusinessException("Direction must be INCREASE or DECREASE");
        }

        BigDecimal delta = increase ? qty : qty.negate();

        // Adjust available AND received by the same delta so "sold" (received - available)
        // stays consistent and available can never exceed received or drop below zero.
        BigDecimal newAvailable = batch.getQuantityAvailable().add(delta);
        if (newAvailable.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    "Cannot remove " + qty.stripTrailingZeros().toPlainString()
                            + " units — only " + batch.getQuantityAvailable().stripTrailingZeros().toPlainString()
                            + " available in this batch");
        }

        BigDecimal newReceived = batch.getQuantityReceived().add(delta);

        batch.setQuantityReceived(newReceived);
        batch.setQuantityAvailable(newAvailable);
        batch.setTotalCost(newReceived.multiply(batch.getUnitCost()));
        batch.setStatus(
                newAvailable.compareTo(BigDecimal.ZERO) > 0
                        ? InventoryBatchStatus.ACTIVE
                        : InventoryBatchStatus.SOLD);

        // Lightweight audit note kept on the batch (capped to the column length).
        String note = "Stock " + (increase ? "+" : "-") + qty.stripTrailingZeros().toPlainString()
                + (request.getRemarks() != null && !request.getRemarks().isBlank()
                        ? " (" + request.getRemarks().trim() + ")" : "");
        String combined = note + (batch.getRemarks() != null && !batch.getRemarks().isBlank()
                ? " | " + batch.getRemarks() : "");
        if (combined.length() > 500) {
            combined = combined.substring(0, 500);
        }
        batch.setRemarks(combined);

        inventoryBatchRepository.save(batch);

        BigDecimal batchValue = newAvailable.multiply(batch.getUnitCost());
        return InventoryBatchResponse.builder()
                .batchId(batch.getId())
                .batchNumber(batch.getBatchNumber())
                .source(ProductSource.valueOf(batch.getSource()))
                .receivedDate(batch.getReceivedDate())
                .totalQuantity(newReceived)
                .quantityAvailable(newAvailable)
                .unitCost(batch.getUnitCost())
                .batchValue(batchValue)
                .status(batch.getStatus().name())
                .build();
    }

    @Override
    @Transactional
    public InventoryResponse adjustProductStock(
            UUID productId,
            com.skr.erp.dto.request.AdjustBatchStockRequest request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found"));

        BigDecimal qty = request.getQuantity();
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Quantity must be greater than zero");
        }
        String dir = request.getDirection() == null ? "" : request.getDirection().trim().toUpperCase();
        boolean increase = dir.equals("INCREASE");
        boolean decrease = dir.equals("DECREASE");
        if (!increase && !decrease) {
            throw new BusinessException("Direction must be INCREASE or DECREASE");
        }

        if (increase) {
            // Add the whole quantity to the most recent batch (reactivate if it was closed).
            List<InventoryBatch> batches = inventoryBatchRepository.findAllByProductId(productId);
            if (batches.isEmpty()) {
                throw new BusinessException(
                        "No batch exists for this product yet — record a purchase or production first.");
            }
            InventoryBatch target = batches.stream()
                    .filter(b -> b.getStatus() == InventoryBatchStatus.ACTIVE)
                    .findFirst()
                    .orElse(batches.get(0)); // findAllByProductId is ordered newest first
            target.setQuantityReceived(target.getQuantityReceived().add(qty));
            target.setQuantityAvailable(target.getQuantityAvailable().add(qty));
            target.setTotalCost(target.getQuantityReceived().multiply(target.getUnitCost()));
            target.setStatus(InventoryBatchStatus.ACTIVE);
            inventoryBatchRepository.save(target);
        } else {
            // Remove FIFO (oldest first) across available batches; keep received in sync so "sold" stays correct.
            List<InventoryBatch> batches = inventoryBatchRepository.findAvailableBatchesForSale(productId);
            BigDecimal totalAvailable = batches.stream()
                    .map(InventoryBatch::getQuantityAvailable)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (qty.compareTo(totalAvailable) > 0) {
                throw new BusinessException(
                        "Cannot remove " + qty.stripTrailingZeros().toPlainString()
                                + " units — only " + totalAvailable.stripTrailingZeros().toPlainString()
                                + " in stock for this product");
            }
            BigDecimal remaining = qty;
            for (InventoryBatch b : batches) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal take = b.getQuantityAvailable().min(remaining);
                b.setQuantityAvailable(b.getQuantityAvailable().subtract(take));
                b.setQuantityReceived(b.getQuantityReceived().subtract(take));
                b.setTotalCost(b.getQuantityReceived().multiply(b.getUnitCost()));
                if (b.getQuantityAvailable().compareTo(BigDecimal.ZERO) == 0) {
                    b.setStatus(InventoryBatchStatus.SOLD);
                }
                inventoryBatchRepository.save(b);
                remaining = remaining.subtract(take);
            }
        }

        // Return the freshly aggregated product row for this product.
        List<InventoryBatch> all = inventoryBatchRepository.findAllByProductId(productId);
        BigDecimal stock = BigDecimal.ZERO;
        BigDecimal value = BigDecimal.ZERO;
        for (InventoryBatch b : all) {
            stock = stock.add(b.getQuantityAvailable());
            value = value.add(b.getQuantityAvailable().multiply(b.getUnitCost()));
        }
        InventoryResponse res = new InventoryResponse();
        res.setProductId(product.getId());
        res.setProductName(product.getName());
        res.setSource(product.getSource());
        res.setCurrentStock(scale2(stock));
        res.setStockValue(scale2(value));
        res.setAverageCost(
                stock.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : scale2(value.divide(stock, 2, RoundingMode.HALF_UP)));
        return res;
    }

    public List<LowStockAlertResponse> getLowStockAlerts() {
        SystemSetting setting = repository.findBySettingKey("LOW_STOCK_THRESHOLD")
                .orElseThrow(() ->
                        new BusinessException("Setting not found."));
        BigDecimal threshold  = new BigDecimal(setting.getSettingValue());
        List<InventoryBatch> batches = inventoryBatchRepository.findAllBatchesWithProduct();

        Map<UUID, LowStockAlertResponse> map = new LinkedHashMap<>();

        for (InventoryBatch batch : batches) {

            Product product = batch.getProduct();

            LowStockAlertResponse response =
                    map.computeIfAbsent(
                            product.getId(),
                            id -> LowStockAlertResponse.builder()
                                    .productId(product.getId())
                                    .productName(product.getName())
                                    .source(product.getSource())
                                    .currentStock(BigDecimal.ZERO)
                                    .threshold(threshold)
                                    .build());

            response.setCurrentStock(
                    response.getCurrentStock()
                            .add(batch.getQuantityAvailable()));
        }

        return map.values()
                .stream()
                .filter(p -> p.getCurrentStock().compareTo(threshold) <= 0)
                .sorted(Comparator.comparing(LowStockAlertResponse::getCurrentStock))
                .toList();
    }
}