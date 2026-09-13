package com.skr.erp.service;

import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.dto.request.CreateProductionRequest;
import com.skr.erp.dto.request.ProductionItemRequest;
import com.skr.erp.dto.response.ProductionDetailsResponse;
import com.skr.erp.dto.response.ProductionItemResponse;
import com.skr.erp.dto.response.ProductionResponse;
import com.skr.erp.entity.*;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.pdf.ProductionPdfGenerator;
import com.skr.erp.repository.*;
import com.skr.erp.specification.ProductionSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductionServiceImpl implements ProductionService {

    private final ProductionEntryRepository productionEntryRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;
    private final ProductRateRepository productRateRepository;
    private final ProductPieceCodeRepository productPieceCodeRepository;
    private final InventoryService inventoryService;
    private final SystemSettingRepository systemSettingRepository;

    private static final String PRODUCTION_PDF_TEMPLATE_KEY = "PRODUCTION_PDF_TEMPLATE";

    @Override
    public ProductionResponse create(
            CreateProductionRequest request) {

        Employee employee =
                employeeRepository
                        .findById(request.getEmployeeId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Employee not found"));

        if (!employee.getActive()) {
            throw new BusinessException(
                    "Employee is inactive");
        }

        ProductionEntry productionEntry =
                new ProductionEntry();

        productionEntry.setEmployee(employee);
        productionEntry.setProductionDate(
                request.getProductionDate());
        productionEntry.setRemarks(
                request.getRemarks());

        for (ProductionItemRequest item :
                request.getItems()) {

            Product product =
                    productRepository
                            .findById(item.getProductId())
                            .orElseThrow(() ->
                                    new BusinessException(
                                            "Product not found"));

            ProductPieceCode pieceCode =
                    productPieceCodeRepository
                            .findById(
                                    item.getPieceCodeId())
                            .orElseThrow(() ->
                                    new BusinessException(
                                            "Piece code not found"));

            if (!pieceCode.getActive()) {

                throw new BusinessException(
                        "Piece code inactive");
            }

            ProductionEntryDetail detail =
                    new ProductionEntryDetail();

            detail.setProductionEntry(
                    productionEntry);

            detail.setProduct(product);

            detail.setPieceCode(
                    pieceCode);

            detail.setQuantity(
                    item.getQuantity());

            BigDecimal rate =
                    pieceCode.getRate();

            detail.setRateSnapshot(
                    rate);

            detail.setAmountSnapshot(
                    rate.multiply(
                            BigDecimal.valueOf(
                                    item.getQuantity()
                            )
                    )
            );

            detail.setPieceCodeSnapshot(
                    pieceCode.getCode());

            detail.setProductNameSnapshot(
                    product.getName());

            productionEntry
                    .getDetails()
                    .add(detail);

        }

        productionEntry =
                productionEntryRepository
                        .save(productionEntry);

        createInventoryBatch(productionEntry);

        return map(productionEntry);
    }

    private void createInventoryBatch(
            ProductionEntry productionEntry) {

        for (ProductionEntryDetail detail : productionEntry.getDetails()) {

            inventoryService.createProductionBatch(detail);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductionResponse> getAll() {

        return productionEntryRepository
                .findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductionDetailsResponse getById(
            UUID productionId) {

        ProductionEntry productionEntry =
                productionEntryRepository
                        .findById(productionId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Production entry not found"));

        int totalQuantity =
                productionEntry.getDetails()
                        .stream()
                        .mapToInt(
                                ProductionEntryDetail::getQuantity)
                        .sum();

        BigDecimal totalAmount =
                productionEntry.getDetails()
                        .stream()
                        .map(ProductionEntryDetail::getAmountSnapshot)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return ProductionDetailsResponse.builder()
                .id(productionEntry.getId())
                .employeeName(
                        productionEntry.getEmployee()
                                .getFullName())
                .productionDate(
                        productionEntry.getProductionDate())
                .remarks(
                        productionEntry.getRemarks())
                .productCount(
                        productionEntry.getDetails()
                                .size())
                .totalQuantity(
                        totalQuantity)
                .totalAmount(
                        totalAmount)
                .items(
                        productionEntry.getDetails()
                                .stream()
                                .map(this::toItemResponse)
                                .toList())
                .build();
    }

    @Override
    public ProductionResponse update(
            UUID productionId,
            CreateProductionRequest request) {

        ProductionEntry productionEntry =
                productionEntryRepository
                        .findById(productionId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Production entry not found"));

        Employee employee =
                employeeRepository
                        .findById(request.getEmployeeId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Employee not found"));

        if (!employee.getActive()) {
            throw new BusinessException(
                    "Employee is inactive");
        }

        productionEntry.setEmployee(employee);

        productionEntry.setProductionDate(
                request.getProductionDate());

        productionEntry.setRemarks(
                request.getRemarks());

        productionEntry.getDetails().clear();

        for (ProductionItemRequest item :
                request.getItems()) {

            Product product =
                    productRepository
                            .findById(item.getProductId())
                            .orElseThrow(() ->
                                    new BusinessException(
                                            "Product not found"));

            BigDecimal rate =
                    productRateRepository
                            .findTopByProductIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                    product.getId(),
                                    request.getProductionDate()
                            )
                            .map(ProductRate::getRate)
                            .orElse(BigDecimal.ZERO);

            ProductionEntryDetail detail =
                    new ProductionEntryDetail();

            detail.setProductionEntry(
                    productionEntry);

            detail.setProduct(product);

            detail.setQuantity(
                    item.getQuantity());

            detail.setRateSnapshot(
                    rate);

            detail.setAmountSnapshot(
                    rate.multiply(
                            BigDecimal.valueOf(
                                    item.getQuantity()
                            )
                    )
            );

            detail.setProductNameSnapshot(
                    product.getName()
            );

            productionEntry
                    .getDetails()
                    .add(detail);
        }

        productionEntry =
                productionEntryRepository
                        .save(productionEntry);

        return map(productionEntry);

    }

    @Override
    public void delete(
            UUID productionId) {

        ProductionEntry productionEntry =
                productionEntryRepository
                        .findById(productionId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Production entry not found"));

        productionEntryRepository.delete(
                productionEntry);
    }


    @Override
    @Transactional(readOnly = true)
    public List<ProductionResponse> search(
            LocalDate fromDate,
            LocalDate toDate,
            UUID employeeId,
            UUID productId) {

        return productionEntryRepository.findAll()
                .stream()

                .filter(entry ->
                        fromDate == null ||
                                !entry.getProductionDate()
                                        .isBefore(fromDate))

                .filter(entry ->
                        toDate == null ||
                                !entry.getProductionDate()
                                        .isAfter(toDate))

                .filter(entry ->
                        employeeId == null ||
                                entry.getEmployee()
                                        .getId()
                                        .equals(employeeId))

                .filter(entry ->
                        productId == null ||
                                entry.getDetails()
                                        .stream()
                                        .anyMatch(detail ->
                                                detail.getProduct()
                                                        .getId()
                                                        .equals(productId)))

                .map(this::map)

                .toList();
    }

    @Override
    public byte[] exportPdf(
            UUID productionId) {

        ProductionDetailsResponse response = getById(productionId);

        // Fetch the HTML template from system settings (by name), fill placeholders, render to PDF.
        String template = systemSettingRepository
                .findBySettingKey(PRODUCTION_PDF_TEMPLATE_KEY)
                .map(com.skr.erp.entity.SystemSetting::getSettingValue)
                .orElseThrow(() -> new BusinessException(
                        "PDF template not configured: " + PRODUCTION_PDF_TEMPLATE_KEY));

        String html = fillProductionTemplate(template, response);
        return com.skr.erp.pdf.HtmlToPdfGenerator.render(html);
    }

    @Override
    public byte[] exportExcel(UUID productionId) {
        ProductionDetailsResponse response = getById(productionId);
        return com.skr.erp.pdf.ProductionExcelGenerator.generate(response);
    }

    private String fillProductionTemplate(String template, ProductionDetailsResponse d) {
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale("en", "IN"));

        StringBuilder rows = new StringBuilder();
        if (d.getItems() != null) {
            for (ProductionItemResponse item : d.getItems()) {
                rows.append("<tr>")
                        .append("<td>").append(esc(item.getProductName())).append("</td>")
                        .append("<td>").append(esc(item.getPieceCode() != null ? item.getPieceCode() : "-")).append("</td>")
                        .append("<td class=\"right\">").append(item.getQuantity() != null ? item.getQuantity() : 0).append("</td>")
                        .append("<td class=\"right\">Rs. ").append(item.getRate() != null ? nf.format(item.getRate()) : "0").append("</td>")
                        .append("<td class=\"right\">Rs. ").append(item.getAmount() != null ? nf.format(item.getAmount()) : "0").append("</td>")
                        .append("</tr>");
            }
        }

        String remarks = (d.getRemarks() == null || d.getRemarks().isBlank()) ? "-" : d.getRemarks();
        String generatedOn = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a"));

        return template
                .replace("{{refId}}", esc(d.getId() != null ? d.getId().toString() : "-"))
                .replace("{{productionDate}}", d.getProductionDate() != null
                        ? d.getProductionDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : "-")
                .replace("{{employeeName}}", esc(d.getEmployeeName()))
                .replace("{{remarks}}", esc(remarks))
                .replace("{{productCount}}", String.valueOf(d.getProductCount() != null ? d.getProductCount() : 0))
                .replace("{{totalQuantity}}", nf.format(d.getTotalQuantity() != null ? d.getTotalQuantity() : 0))
                .replace("{{totalAmount}}", d.getTotalAmount() != null ? nf.format(d.getTotalAmount()) : "0")
                .replace("{{itemRows}}", rows.toString())
                .replace("{{generatedOn}}", generatedOn);
    }

    // Escape dynamic values so the filled template stays valid XHTML for the renderer.
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }


    private ProductionResponse map(
            ProductionEntry productionEntry) {

        int totalQuantity =
                productionEntry.getDetails()
                        .stream()
                        .mapToInt(
                                ProductionEntryDetail::getQuantity)
                        .sum();

        BigDecimal totalAmount =
                productionEntry.getDetails()
                        .stream()
                        .map(detail ->
                                detail.getAmountSnapshot() == null
                                        ? BigDecimal.ZERO
                                        : detail.getAmountSnapshot()
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return ProductionResponse.builder()
                .id(productionEntry.getId())
                .employeeId(
                        productionEntry.getEmployee().getId())
                .employeeName(
                        productionEntry.getEmployee().getFullName())
                .productionDate(
                        productionEntry.getProductionDate())
                .remarks(
                        productionEntry.getRemarks())

                .productCount(
                        productionEntry.getDetails().size())

                .totalQuantity(
                        totalQuantity)

                .totalAmount(
                        totalAmount)

                .items(
                        productionEntry.getDetails()
                                .stream()
                                .map(this::toItemResponse)
                                .toList())

                .build();
    }

    private ProductionItemResponse toItemResponse(ProductionEntryDetail detail) {

        return ProductionItemResponse.builder()
                .productId(
                        detail.getProduct().getId())
                .productName(
                        detail.getProductNameSnapshot())
                .quantity(
                        detail.getQuantity())
                .rate(
                        detail.getRateSnapshot())
                .amount(
                        detail.getAmountSnapshot())
                .pieceCodeId(
                        detail.getPieceCode() != null
                                ? detail.getPieceCode().getId()
                                : null)

                .pieceCode(
                        detail.getPieceCodeSnapshot())
                .build();
    }


}