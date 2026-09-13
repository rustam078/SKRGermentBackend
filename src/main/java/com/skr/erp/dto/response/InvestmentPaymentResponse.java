package com.skr.erp.dto.response;

import com.skr.erp.common.constants.PaymentMode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class InvestmentPaymentResponse {

    private UUID id;
    private LocalDate paymentDate;
    private PaymentMode mode;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
