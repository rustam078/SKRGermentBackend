package com.skr.erp.entity;

import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "sales_order")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrder extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String invoiceNo;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerMobile;

    private String customerEmail;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tax;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grandTotal;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    private String paymentProvider;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private String remarks;
}