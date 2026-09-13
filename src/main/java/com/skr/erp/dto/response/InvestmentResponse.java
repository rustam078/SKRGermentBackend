package com.skr.erp.dto.response;

import com.skr.erp.common.constants.InvestmentType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class InvestmentResponse {

    private UUID id;

    private String referenceNumber;

    private String invoiceNumber;

    private UUID vendorId;

    private String vendorName;

    private InvestmentType investmentType;

    private LocalDate purchaseDate;

    private Integer itemCount;

    private BigDecimal grandTotal;
}