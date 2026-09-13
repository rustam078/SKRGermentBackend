package com.skr.erp.dto.response;

import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SalesDetailsResponse {

    private UUID saleId;
    private String invoiceNo;
    private LocalDate saleDate;
    private String customerName;
    private String customerMobile;
    private String customerEmail;
    private PaymentMode paymentMode;
    private String paymentProvider;
    private PaymentStatus paymentStatus;
    private String remarks;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal grandTotal;
    private BigDecimal totalProfit;
    private List<SalesItemResponse> items;
}
