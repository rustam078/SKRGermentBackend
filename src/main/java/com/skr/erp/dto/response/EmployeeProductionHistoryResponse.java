package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class EmployeeProductionHistoryResponse {

    private LocalDate productionDate;
    private String productName;
    private Integer quantity;
    private BigDecimal rate;
    private BigDecimal earnings;
}