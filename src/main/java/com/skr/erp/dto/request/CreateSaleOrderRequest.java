package com.skr.erp.dto.request;

import com.skr.erp.common.constants.PaymentMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateSaleOrderRequest {

    @NotBlank
    private String customerName;

    @NotBlank
    private String customerMobile;

    @Email
    private String customerEmail;

    @NotNull
    private PaymentMode paymentMode;

    private String paymentProvider;

    private String remarks;

    @NotEmpty
    private List<CreateSaleOrderItemRequest> items;
}