package com.skr.erp.service;

import com.skr.erp.common.constants.InventoryTransactionType;
import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.PaymentStatus;
import com.skr.erp.common.response.PageResponse;
import com.skr.erp.dto.request.CreateSaleOrderItemRequest;
import com.skr.erp.dto.request.CreateSaleOrderRequest;
import com.skr.erp.dto.response.*;
import com.skr.erp.entity.*;
import com.skr.erp.exception.BusinessException;
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

    private static final String SALES_INVOICE_TEMPLATE_KEY = "SALES_INVOICE_TEMPLATE";


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

        Set<UUID> productIds = new HashSet<>();

        for (CreateSaleOrderItemRequest item : request.getItems()) {

            if (!productIds.add(item.getProductId())) {
                throw new BadRequestException("Duplicate product selected.");
            }

            if (item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Quantity must be greater than zero.");
            }

            if (item.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Selling price cannot be negative.");
            }
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

        // ==========================
        // Process Products
        // ==========================

        for (CreateSaleOrderItemRequest item : request.getItems()) {

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() ->
                            new BusinessException("Product not found."));

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

            deductInventory(
                    order,
                    product,
                    item,
                    batches
            );

            subtotal = subtotal.add(
                    item.getSellingPrice()
                            .multiply(item.getQuantity())
            );
        }

        order.setSubtotal(subtotal);
        order.setDiscount(BigDecimal.ZERO);
        order.setGrandTotal(
                subtotal.subtract(order.getDiscount())
        );

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

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
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
                .discount(BigDecimal.ZERO)
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
                        .setScale(2, RoundingMode.HALF_UP)
        );

        salesOrderItemRepository.save(orderItem);
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
