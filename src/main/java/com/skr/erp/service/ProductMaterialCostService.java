package com.skr.erp.service;

import com.skr.erp.dto.request.CreateProductMaterialCostRequest;
import com.skr.erp.dto.request.UpdateMaterialCostRequest;
import com.skr.erp.dto.response.ProductMaterialCostResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProductMaterialCostService {

    ProductMaterialCostResponse addMaterialCost(
            UUID productId,
            CreateProductMaterialCostRequest request);

    List<ProductMaterialCostResponse> getMaterialCosts(
            UUID productId);

    BigDecimal getMaterialCostByDate(
            UUID productId,
            LocalDate date);

    void delete(
            UUID id);
    ProductMaterialCostResponse update(
            UUID id,
            UpdateMaterialCostRequest request);
}