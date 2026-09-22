package com.skr.erp.service;

import com.skr.erp.common.constants.InventoryBatchStatus;
import com.skr.erp.common.constants.InventoryTransactionType;
import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.PaymentStatus;
import com.skr.erp.common.response.PageResponse;
import com.skr.erp.dto.request.CreateSaleOrderItemRequest;
import com.skr.erp.dto.request.CreateSaleOrderRequest;
import com.skr.erp.dto.response.*;
import com.skr.erp.entity.*;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.exception.ReconciliationRequiredException;
import com.skr.erp.repository.*;
import com.skr.erp.specification.SalesSpecification;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements  SalesService{

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final com.skr.erp.qr.QrUnitService qrUnitService;

    private static final String SALES_INVOICE_TEMPLATE_KEY = "SALES_INVOICE_TEMPLATE";
    private static final String SALES_GST_ENABLED_KEY = "SALES_GST_ENABLED";
    private static final String DEFAULT_GST_PERCENT_KEY = "DEFAULT_GST_PERCENT";

    /**
     * GST on the net amount (subtotal - discount) when SALES_GST_ENABLED is on;
     * zero otherwise. The percent is read live from DEFAULT_GST_PERCENT.
     */
    private BigDecimal computeGst(BigDecimal net) {
        boolean enabled = systemSettingRepository.findBySettingKey(SALES_GST_ENABLED_KEY)
                .map(s -> "true".equalsIgnoreCase(s.getSettingValue()))
                .orElse(false);
        if (!enabled || net.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal percent = systemSettingRepository.findBySettingKey(DEFAULT_GST_PERCENT_KEY)
                .map(s -> {
                    try {
                        return new BigDecimal(s.getSettingValue());
                    } catch (NumberFormatException e) {
                        return BigDecimal.ZERO;
                    }
                })
                .orElse(BigDecimal.ZERO);
        return net.multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }


    @Override
    @Transactional
    public CreateSaleOrderResponse createSale(CreateSaleOrderRequest request)
            throws BadRequestException {

        // ==========================
        // Customer
        // ==========================

        Customer customer = customerRepository
                .findByMobile(request.getCustomerMobile())
                .orElseGet(() -> customerRepository.save(
                        Customer.builder()
                                .name(request.getCustomerName())
                                .mobile(request.getCustomerMobile())
                                .email(request.getCustomerEmail())
                                .build()
                ));

        // Optional customer update
        customer.setName(request.getCustomerName());
        customer.setEmail(request.getCustomerEmail());

        // ==========================
        // Validate duplicate products
        // ==========================

        // A line is unique per (product + batch). Same product from two different
        // scanned batches is allowed (two lines); the same product+batch twice is not.
        Set<String> lineKeys = new HashSet<>();

        for (CreateSaleOrderItemRequest item : request.getItems()) {

            String lineKey = item.getProductId() + "|" + (item.getBatchNumber() == null ? "" : item.getBatchNumber());
            if (!lineKeys.add(lineKey)) {
                throw new BadRequestException("Duplicate product selected.");
            }

            if (item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Quantity must be greater than zero.");
            }

            if (item.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Selling price cannot be negative.");
            }

            // Discount cannot exceed the line's gross (sellingPrice * quantity).
            BigDecimal lineGross = item.getSellingPrice().multiply(item.getQuantity());
            if (nz(item.getDiscount()).compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Discount cannot be negative.");
            }
            if (nz(item.getDiscount()).compareTo(lineGross) > 0) {
                throw new BadRequestException("Discount cannot exceed the line amount.");
            }

            // Scanned lines must carry one serial per unit sold.
            if (item.getBatchNumber() != null) {
                int serialCount = item.getSerials() == null ? 0 : item.getSerials().size();
                if (serialCount != item.getQuantity().intValueExact()) {
                    throw new BadRequestException(
                            "Scanned line for batch " + item.getBatchNumber()
                                    + " must have one serial per unit.");
                }
            }
        }

        // ==========================
        // Reconciliation pre-check (scanned lines)
        // ==========================
        // Validate every scanned QR up front (unknown / wrong-batch / already SOLD-VOID fail hard —
        // NO reconciliation for used tags). Then, batch-wise, find batches whose system stock is
        // short of the scanned quantity (FIFO already consumed it). Any such batch not yet approved
        // by the user aborts with ReconciliationRequiredException so the UI can ask once per batch.
        java.util.Set<String> approvedBatches = request.getReconcileBatches() != null
                ? new java.util.HashSet<>(request.getReconcileBatches())
                : java.util.Collections.emptySet();
        List<ReconcileBatchInfo> needsReconcile = new java.util.ArrayList<>();
        for (CreateSaleOrderItemRequest item : request.getItems()) {
            if (item.getBatchNumber() == null) continue;
            InventoryBatch batch = inventoryBatchRepository.findByBatchNumber(item.getBatchNumber())
                    .orElseThrow(() -> new BusinessException("Batch " + item.getBatchNumber() + " not found."));
            qrUnitService.validateUnitsForSale(item.getBatchNumber(), item.getSerials());
            BigDecimal available = batch.getQuantityAvailable();
            if (available.compareTo(item.getQuantity()) < 0 && !approvedBatches.contains(item.getBatchNumber())) {
                needsReconcile.add(ReconcileBatchInfo.builder()
                        .batchNumber(item.getBatchNumber())
                        .productName(batch.getProduct() != null ? batch.getProduct().getName() : "")
                        .available(available)
                        .requested(item.getQuantity())
                        .deficit(item.getQuantity().subtract(available))
                        .build());
            }
        }
        if (!needsReconcile.isEmpty()) {
            throw new ReconciliationRequiredException(needsReconcile);
        }

        // ==========================
        // Create Order
        // ==========================

        SalesOrder order = SalesOrder.builder()
                .invoiceNo(nextInvoiceNumber())
                .customerName(customer.getName())
                .customerMobile(customer.getMobile())
                .customerEmail(customer.getEmail())
                .paymentMode(request.getPaymentMode())
                .paymentProvider(request.getPaymentProvider())
                .paymentStatus(PaymentStatus.PAID)
                .remarks(request.getRemarks())
                .tax(BigDecimal.ZERO)
                .subtotal(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .grandTotal(BigDecimal.ZERO)
                .build();

        salesOrderRepository.save(order);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        // ==========================
        // Process Products
        // ==========================

        for (CreateSaleOrderItemRequest item : request.getItems()) {

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() ->
                            new BusinessException("Product not found."));

            if (item.getBatchNumber() != null) {
                // Scanned line: deduct exactly this batch and mark its units SOLD.
                deductSpecificBatch(order, product, item);
            } else {
                // Typed line: FIFO deduction across the product's available batches.
                List<InventoryBatch> batches =
                        inventoryBatchRepository.findAvailableBatchesForSale(product.getId());

                if (batches.isEmpty()) {
                    throw new BusinessException(product.getName() + " is out of stock.");
                }

                BigDecimal totalAvailable = batches.stream()
                        .map(InventoryBatch::getQuantityAvailable)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalAvailable.compareTo(item.getQuantity()) < 0) {
                    throw new BusinessException(
                            product.getName() + " has only " + totalAvailable + " available."
                    );
                }

                deductInventory(order, product, item, batches);
            }

            subtotal = subtotal.add(
                    item.getSellingPrice()
                            .multiply(item.getQuantity())
            );
            totalDiscount = totalDiscount.add(nz(item.getDiscount()));
        }

        // Net amount after discount, then optional GST on top (controlled by settings).
        BigDecimal net = subtotal.subtract(totalDiscount);
        BigDecimal tax = computeGst(net);

        order.setSubtotal(subtotal);
        order.setDiscount(totalDiscount);
        order.setTax(tax);
        order.setGrandTotal(net.add(tax));

        salesOrderRepository.save(order);

        return CreateSaleOrderResponse.builder()
                .saleOrderId(order.getId())
                .invoiceNumber(order.getInvoiceNo())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .grandTotal(order.getGrandTotal())
                .paymentStatus(order.getPaymentStatus())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SalesListResponse> getSales(
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            PaymentMode paymentMode,
            PaymentStatus paymentStatus,
            Pageable pageable) {

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException("fromDate cannot be after toDate");
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<SalesOrder> salesPage = salesOrderRepository.findAll(
                SalesSpecification.filter(
                        search, fromDate, toDate, paymentMode, paymentStatus),
                sortedPageable
        );

        List<SalesListResponse> content = salesPage.getContent().stream()
                .map(this::toSalesListResponse)
                .toList();

        return PageResponse.<SalesListResponse>builder()
                .content(content)
                .page(salesPage.getNumber())
                .size(salesPage.getSize())
                .totalElements(salesPage.getTotalElements())
                .totalPages(salesPage.getTotalPages())
                .last(salesPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesSummaryResponse getSalesSummary(
            String search, LocalDate fromDate, LocalDate toDate,
            PaymentMode paymentMode, PaymentStatus paymentStatus) {

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException("fromDate cannot be after toDate");
        }

        // Same filter as the list, so the header totals always match the rows.
        List<SalesOrder> orders = salesOrderRepository.findAll(
                SalesSpecification.filter(search, fromDate, toDate, paymentMode, paymentStatus));

        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        for (SalesOrder o : orders) {
            revenue = revenue.add(o.getGrandTotal() != null ? o.getGrandTotal() : BigDecimal.ZERO);
            discount = discount.add(o.getDiscount() != null ? o.getDiscount() : BigDecimal.ZERO);
        }
        return SalesSummaryResponse.builder()
                .count(orders.size())
                .revenue(revenue)
                .discount(discount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesDetailsResponse getSaleDetails(UUID saleId) {
        SalesOrder order = getSalesOrderOrThrow(saleId);
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderIdWithProduct(saleId);
        return toSalesDetailsResponse(order, items);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID saleId) {
        SalesOrder order = getSalesOrderOrThrow(saleId);
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderIdWithProduct(saleId);
        return toInvoiceResponse(order, items);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportInvoicePdf(UUID saleId) {
        InvoiceResponse inv = getInvoice(saleId);
        String template = systemSettingRepository
                .findBySettingKey(SALES_INVOICE_TEMPLATE_KEY)
                .map(SystemSetting::getSettingValue)
                .orElseThrow(() -> new BusinessException(
                        "Invoice template not configured: " + SALES_INVOICE_TEMPLATE_KEY));
        String html = fillInvoiceTemplate(template, inv);
        return com.skr.erp.pdf.HtmlToPdfGenerator.render(html);
    }

    private String fillInvoiceTemplate(String template, InvoiceResponse inv) {
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale("en", "IN"));

        StringBuilder rows = new StringBuilder();
        if (inv.getItems() != null) {
            for (InvoiceItemResponse it : inv.getItems()) {
                rows.append("<tr>")
                        .append("<td>").append(esc(it.getProductName())).append("</td>")
                        .append("<td class=\"right\">").append(fmtQty(it.getQuantity())).append("</td>")
                        .append("<td class=\"right\">Rs. ").append(money(nf, it.getSellingPrice())).append("</td>")
                        .append("<td class=\"right\">Rs. ").append(money(nf, it.getDiscount())).append("</td>")
                        .append("<td class=\"right\">Rs. ").append(money(nf, it.getLineTotal())).append("</td>")
                        .append("</tr>");
            }
        }

        String status = inv.getPaymentStatus() != null ? inv.getPaymentStatus().name() : "-";
        String statusClass = "PAID".equals(status) ? "badge-paid" : "badge-other";
        String generatedOn = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a"));
        String email = (inv.getCustomerEmail() == null || inv.getCustomerEmail().isBlank()) ? "" : inv.getCustomerEmail();
        String provider = (inv.getPaymentProvider() == null || inv.getPaymentProvider().isBlank()) ? "" : inv.getPaymentProvider();

        return template
                .replace("{{companyName}}", esc(setting("COMPANY_NAME", "SKR Garment")))
                .replace("{{companyAddress}}", esc(setting("COMPANY_ADDRESS", "")))
                .replace("{{companyContact}}", esc(setting("COMPANY_CONTACT", "")))
                .replace("{{companyGstin}}", esc(setting("COMPANY_GSTIN", "-")))
                .replace("{{invoiceNumber}}", esc(inv.getInvoiceNumber()))
                .replace("{{date}}", inv.getDate() != null
                        ? inv.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : "-")
                .replace("{{customerName}}", esc(inv.getCustomerName()))
                .replace("{{customerMobile}}", esc(inv.getCustomerMobile()))
                .replace("{{customerEmail}}", esc(email))
                .replace("{{paymentMode}}", inv.getPaymentMode() != null ? inv.getPaymentMode().name() : "-")
                .replace("{{statusClass}}", statusClass)
                .replace("{{paymentStatus}}", status)
                .replace("{{paymentProvider}}", esc(provider))
                .replace("{{itemRows}}", rows.toString())
                .replace("{{subtotal}}", money(nf, inv.getSubtotal()))
                .replace("{{discount}}", money(nf, inv.getDiscount()))
                .replace("{{tax}}", money(nf, inv.getTax()))
                .replace("{{grandTotal}}", money(nf, inv.getGrandTotal()))
                .replace("{{generatedOn}}", generatedOn);
    }

    private static String money(java.text.NumberFormat nf, BigDecimal b) {
        return b != null ? nf.format(b) : "0";
    }

    private static String fmtQty(BigDecimal q) {
        return q != null ? q.stripTrailingZeros().toPlainString() : "0";
    }

    /** Null-safe BigDecimal — treats a missing value as zero. */
    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /** Live system-setting value, or the fallback when missing/blank. */
    private String setting(String key, String fallback) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .filter(s -> s != null && !s.isBlank())
                .orElse(fallback);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesDashboardResponse getDashboard() {

        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);

        SalesSummaryProjection todayStats =
                salesOrderRepository.countAndRevenueBetween(
                        today.atStartOfDay(),
                        today.plusDays(1).atStartOfDay()
                );

        SalesSummaryProjection monthStats =
                salesOrderRepository.countAndRevenueBetween(
                        firstDayOfMonth.atStartOfDay(),
                        firstDayOfMonth.plusMonths(1).atStartOfDay()
                );

        SalesSummaryProjection totalStats =
                salesOrderRepository.countAndTotalRevenue();

        BigDecimal totalProfit =
                Optional.ofNullable(
                                salesOrderItemRepository.calculateTotalProfit())
                        .orElse(BigDecimal.ZERO);

        return SalesDashboardResponse.builder()
                .todaySales(todayStats.getTotalSales())
                .todayRevenue(todayStats.getTotalRevenue())
                .monthSales(monthStats.getTotalSales())
                .monthRevenue(monthStats.getTotalRevenue())
                .totalSales(totalStats.getTotalSales())
                .totalRevenue(totalStats.getTotalRevenue())
                .totalProfit(totalProfit)
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public List<TopSellingProductResponse> getTopProducts() {
        return salesOrderItemRepository
                .findTopSellingProducts(PageRequest.of(0, 10))
                .stream()
                .map(row -> TopSellingProductResponse.builder()
                        .productId((UUID) row[0])
                        .productName((String) row[1])
                        .totalQuantitySold((BigDecimal) row[2])
                        .totalRevenue((BigDecimal) row[3])
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentSaleResponse> getRecentSales() {
        return salesOrderRepository
                .findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(this::toRecentSaleResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerSaleResponse> getCustomerSales(
            UUID customerId,
            Pageable pageable) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("Customer not found."));

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<SalesOrder> salesPage = salesOrderRepository
                .findByCustomerMobileOrderByCreatedAtDesc(
                        customer.getMobile(),
                        sortedPageable
                );

        List<CustomerSaleResponse> content = salesPage.getContent().stream()
                .map(this::toCustomerSaleResponse)
                .toList();

        return PageResponse.<CustomerSaleResponse>builder()
                .content(content)
                .page(salesPage.getNumber())
                .size(salesPage.getSize())
                .totalElements(salesPage.getTotalElements())
                .totalPages(salesPage.getTotalPages())
                .last(salesPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSaleHistoryResponse> getProductSales(
            UUID productId,
            Pageable pageable) {

        productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found."));

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<SalesOrderItem> itemsPage = salesOrderItemRepository
                .findByProductId(productId, sortedPageable);

        List<ProductSaleHistoryResponse> content = itemsPage.getContent().stream()
                .map(this::toProductSaleHistoryResponse)
                .toList();

        return PageResponse.<ProductSaleHistoryResponse>builder()
                .content(content)
                .page(itemsPage.getNumber())
                .size(itemsPage.getSize())
                .totalElements(itemsPage.getTotalElements())
                .totalPages(itemsPage.getTotalPages())
                .last(itemsPage.isLast())
                .build();
    }

    private SalesOrder getSalesOrderOrThrow(UUID saleId) {
        return salesOrderRepository.findById(saleId)
                .orElseThrow(() -> new BusinessException("Sale not found."));
    }

    private SalesListResponse toSalesListResponse(SalesOrder order) {
        return SalesListResponse.builder()
                .saleId(order.getId())
                .invoiceNo(order.getInvoiceNo())
                .customerName(order.getCustomerName())
                .customerMobile(order.getCustomerMobile())
                .grandTotal(order.getGrandTotal())
                .paymentMode(order.getPaymentMode())
                .paymentStatus(order.getPaymentStatus())
                .saleDate(order.getCreatedAt().toLocalDate())
                .build();
    }

    private SalesDetailsResponse toSalesDetailsResponse(
            SalesOrder order,
            List<SalesOrderItem> items) {

        List<SalesItemResponse> itemResponses = items.stream()
                .map(this::toSalesItemResponse)
                .toList();

        BigDecimal totalProfit = itemResponses.stream()
                .map(SalesItemResponse::getProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SalesDetailsResponse.builder()
                .saleId(order.getId())
                .invoiceNo(order.getInvoiceNo())
                .saleDate(order.getCreatedAt().toLocalDate())
                .customerName(order.getCustomerName())
                .customerMobile(order.getCustomerMobile())
                .customerEmail(order.getCustomerEmail())
                .paymentMode(order.getPaymentMode())
                .paymentProvider(order.getPaymentProvider())
                .paymentStatus(order.getPaymentStatus())
                .remarks(order.getRemarks())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .tax(order.getTax())
                .grandTotal(order.getGrandTotal())
                .totalProfit(totalProfit)
                .items(itemResponses)
                .build();
    }

    private SalesItemResponse toSalesItemResponse(SalesOrderItem item) {
        return SalesItemResponse.builder()
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .sellingPrice(item.getSellingPrice())
                .unitCost(item.getUnitPrice())
                .discount(item.getDiscount())
                .lineTotal(item.getLineTotal())
                .profit(calculateItemProfit(item))
                .build();
    }

    private InvoiceResponse toInvoiceResponse(
            SalesOrder order,
            List<SalesOrderItem> items) {

        List<InvoiceItemResponse> invoiceItems = items.stream()
                .map(item -> InvoiceItemResponse.builder()
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .sellingPrice(item.getSellingPrice())
                        .discount(item.getDiscount())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();

        return InvoiceResponse.builder()
                .invoiceNumber(order.getInvoiceNo())
                .date(order.getCreatedAt().toLocalDate())
                .customerName(order.getCustomerName())
                .customerMobile(order.getCustomerMobile())
                .customerEmail(order.getCustomerEmail())
                .paymentMode(order.getPaymentMode())
                .paymentProvider(order.getPaymentProvider())
                .paymentStatus(order.getPaymentStatus())
                .items(invoiceItems)
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .tax(order.getTax())
                .grandTotal(order.getGrandTotal())
                .build();
    }

    private RecentSaleResponse toRecentSaleResponse(SalesOrder order) {
        return RecentSaleResponse.builder()
                .invoiceNo(order.getInvoiceNo())
                .customerName(order.getCustomerName())
                .grandTotal(order.getGrandTotal())
                .paymentStatus(order.getPaymentStatus())
                .saleDate(order.getCreatedAt().toLocalDate())
                .build();
    }

    private CustomerSaleResponse toCustomerSaleResponse(SalesOrder order) {
        return CustomerSaleResponse.builder()
                .invoiceNo(order.getInvoiceNo())
                .saleDate(order.getCreatedAt().toLocalDate())
                .grandTotal(order.getGrandTotal())
                .paymentStatus(order.getPaymentStatus())
                .build();
    }

    private ProductSaleHistoryResponse toProductSaleHistoryResponse(SalesOrderItem item) {
        SalesOrder order = item.getSalesOrder();

        return ProductSaleHistoryResponse.builder()
                .invoiceNo(order.getInvoiceNo())
                .saleDate(order.getCreatedAt().toLocalDate())
                .customerName(order.getCustomerName())
                .quantitySold(item.getQuantity())
                .sellingPrice(item.getSellingPrice())
                .profit(calculateItemProfit(item))
                .build();
    }

    private BigDecimal calculateItemProfit(SalesOrderItem item) {
        BigDecimal totalCost = item.getUnitPrice().multiply(item.getQuantity());
        return item.getLineTotal().subtract(totalCost);
    }

    private void deductInventory(
            SalesOrder order,
            Product product,
            CreateSaleOrderItemRequest item,
            List<InventoryBatch> batches) {

        BigDecimal remainingQty = item.getQuantity();
        BigDecimal totalCost = BigDecimal.ZERO;

        // Create Order Item
        SalesOrderItem orderItem = SalesOrderItem.builder()
                .salesOrder(order)
                .product(product)
                .quantity(item.getQuantity())
                .discount(nz(item.getDiscount()))
                .lineTotal(BigDecimal.ZERO)
                .unitPrice(BigDecimal.ZERO) // Will be updated after calculating average cost
                .sellingPrice(item.getSellingPrice())
                .build();

        // Save first to generate ID
        orderItem = salesOrderItemRepository.save(orderItem);

        for (InventoryBatch batch : batches) {

            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal availableQty = batch.getQuantityAvailable();

            if (availableQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal deductQty = remainingQty.min(availableQty);

            // Deduct inventory
            BigDecimal remainingInBatch = availableQty.subtract(deductQty);
            batch.setQuantityAvailable(remainingInBatch);

            // Mark the batch SOLD once it is fully consumed.
            if (remainingInBatch.compareTo(BigDecimal.ZERO) == 0) {
                batch.setStatus(com.skr.erp.common.constants.InventoryBatchStatus.SOLD);
            }

            totalCost = totalCost.add(
                    deductQty.multiply(batch.getUnitCost())
            );

            // Inventory Transaction
            InventoryTransaction transaction = InventoryTransaction.builder()
                    .salesOrderItem(orderItem)
                    .batch(batch)
                    .product(product)
                    .transactionType(InventoryTransactionType.SALE)
                    .quantity(deductQty)
                    .unitCost(batch.getUnitCost())
                    .build();

            inventoryTransactionRepository.save(transaction);

            remainingQty = remainingQty.subtract(deductQty);
        }

        if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(
                    product.getName() + " stock not enough."
            );
        }

        // Average Cost
        BigDecimal averageCost = totalCost.divide(
                item.getQuantity(),
                2,
                RoundingMode.HALF_UP
        );

        orderItem.setUnitPrice(averageCost);

        orderItem.setLineTotal(
                item.getSellingPrice()
                        .multiply(item.getQuantity())
                        .subtract(nz(item.getDiscount()))
                        .setScale(2, RoundingMode.HALF_UP)
        );

        salesOrderItemRepository.save(orderItem);
    }

    /**
     * Deduct one specific batch for a scanned line and mark its units SOLD.
     * Unlike FIFO, the exact batch on the QR tag is consumed.
     */
    private void deductSpecificBatch(
            SalesOrder order,
            Product product,
            CreateSaleOrderItemRequest item) {

        InventoryBatch batch = inventoryBatchRepository.findByBatchNumber(item.getBatchNumber())
                .orElseThrow(() -> new BusinessException("Batch " + item.getBatchNumber() + " not found."));

        if (!batch.getProduct().getId().equals(product.getId())) {
            throw new BusinessException(
                    "Batch " + item.getBatchNumber() + " does not belong to " + product.getName() + ".");
        }

        BigDecimal needed = item.getQuantity();

        // Order item first so the reconciliation & sale transactions can reference it.
        SalesOrderItem orderItem = SalesOrderItem.builder()
                .salesOrder(order)
                .product(product)
                .quantity(needed)
                .discount(nz(item.getDiscount()))
                .unitPrice(batch.getUnitCost())
                .sellingPrice(item.getSellingPrice())
                .batchNumber(item.getBatchNumber())
                .lineTotal(item.getSellingPrice()
                        .multiply(needed)
                        .subtract(nz(item.getDiscount()))
                        .setScale(2, RoundingMode.HALF_UP))
                .build();

        orderItem = salesOrderItemRepository.save(orderItem);

        // FIFO may have already drained this batch's system stock even though the scanned
        // QR is still physically available. The reconciliation was approved upstream
        // (createSale threw ReconciliationRequiredException otherwise), so top the batch
        // up by the deficit with an audited RECONCILIATION transaction, then sell normally.
        BigDecimal available = batch.getQuantityAvailable();
        if (available.compareTo(needed) < 0) {
            BigDecimal deficit = needed.subtract(available);
            InventoryTransaction reconciliation = InventoryTransaction.builder()
                    .salesOrderItem(orderItem)
                    .batch(batch)
                    .product(product)
                    .transactionType(InventoryTransactionType.RECONCILIATION)
                    .quantity(deficit)
                    .unitCost(batch.getUnitCost())
                    .reason("Physical QR stock reconciliation")
                    .build();
            inventoryTransactionRepository.save(reconciliation);
            batch.setQuantityAvailable(available.add(deficit));
            available = batch.getQuantityAvailable();
        }

        // Mark the scanned units SOLD (validates each serial is AVAILABLE in this batch).
        qrUnitService.consumeForSale(item.getBatchNumber(), item.getSerials(), order.getId());

        BigDecimal remainingInBatch = available.subtract(needed);
        batch.setQuantityAvailable(remainingInBatch);
        batch.setStatus(remainingInBatch.compareTo(BigDecimal.ZERO) == 0
                ? InventoryBatchStatus.SOLD
                : InventoryBatchStatus.ACTIVE);

        InventoryTransaction transaction = InventoryTransaction.builder()
                .salesOrderItem(orderItem)
                .batch(batch)
                .product(product)
                .transactionType(InventoryTransactionType.SALE)
                .quantity(needed)
                .unitCost(batch.getUnitCost())
                .build();

        inventoryTransactionRepository.save(transaction);
    }

    private String nextInvoiceNumber() {
        long count = salesOrderRepository.count() + 1;
        return "INV-" + String.format("%06d", count);
    }


    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getCustomers(
            String mobile,
            Pageable pageable) {

        Page<Customer> customers;

        if (mobile != null && !mobile.isBlank()) {
            customers = customerRepository.findByMobileContainingIgnoreCase(
                    mobile,
                    pageable
            );
        } else {
            customers = customerRepository.findAll(pageable);
        }

        List<CustomerResponse> content = customers.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<CustomerResponse>builder()
                .content(content)
                .page(customers.getNumber())
                .size(customers.getSize())
                .totalElements(customers.getTotalElements())
                .totalPages(customers.getTotalPages())
                .last(customers.isLast())
                .build();
    }

    private CustomerResponse toResponse(Customer customer) {

        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .mobile(customer.getMobile())
                .email(customer.getEmail())
                .build();
    }
}
