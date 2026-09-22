package com.skr.erp.service;

import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.PaymentStatus;
import com.skr.erp.common.response.PageResponse;
import com.skr.erp.dto.request.CreateSaleOrderRequest;
import com.skr.erp.dto.response.*;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SalesService {

    CreateSaleOrderResponse createSale(CreateSaleOrderRequest request) throws BadRequestException;

    PageResponse<SalesListResponse> getSales(
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            PaymentMode paymentMode,
            PaymentStatus paymentStatus,
            Pageable pageable);

    SalesSummaryResponse getSalesSummary(
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            PaymentMode paymentMode,
            PaymentStatus paymentStatus);

    SalesDetailsResponse getSaleDetails(UUID saleId);

    InvoiceResponse getInvoice(UUID saleId);

    byte[] exportInvoicePdf(UUID saleId);

    SalesDashboardResponse getDashboard();

    List<TopSellingProductResponse> getTopProducts();

    List<RecentSaleResponse> getRecentSales();

    PageResponse<CustomerSaleResponse> getCustomerSales(UUID customerId, Pageable pageable);

    PageResponse<ProductSaleHistoryResponse> getProductSales(UUID productId, Pageable pageable);
    PageResponse<CustomerResponse> getCustomers(
            String mobile,
            Pageable pageable
    );
}
