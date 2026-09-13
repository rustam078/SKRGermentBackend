package com.skr.erp.dto.response;

import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {

    private String invoiceNumber;
    private LocalDate date;
    private String customerName;
    private String customerMobile;
    private String customerEmail;
    private PaymentMode paymentMode;
    private String paymentProvider;
    private PaymentStatus paymentStatus;
    private List<InvoiceItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal grandTotal;
}
