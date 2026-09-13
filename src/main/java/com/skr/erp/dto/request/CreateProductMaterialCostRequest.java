package com.skr.erp.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateProductMaterialCostRequest {

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal cost;

    // Selling price for the same effective date (optional; used for profit %).
    @DecimalMin("0.00")
    private BigDecimal salePrice;

    @NotNull
    private LocalDate effectiveFrom;

    private String remarks;
}