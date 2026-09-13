package com.skr.erp.service;

import com.skr.erp.dto.request.CreateProductionRequest;
import com.skr.erp.dto.response.ProductionDetailsResponse;
import com.skr.erp.dto.response.ProductionResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProductionService {

    ProductionResponse create(
            CreateProductionRequest request);

    List<ProductionResponse> getAll();
    ProductionDetailsResponse getById(
            UUID productionId
    );
    List<ProductionResponse> search(
            LocalDate fromDate,
            LocalDate toDate,
            UUID employeeId,
            UUID productId);

    ProductionResponse update(
            UUID productionId,
            CreateProductionRequest request
    );

    void delete(UUID productionId);
    byte[] exportPdf(UUID productionId);
    byte[] exportExcel(UUID productionId);

}