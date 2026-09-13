package com.skr.erp.service;

import com.skr.erp.common.response.PageResponse;
import com.skr.erp.dto.response.InventoryResponse;
import com.skr.erp.dto.response.LowStockAlertResponse;
import com.skr.erp.dto.response.ProductInventoryDetailResponse;
import com.skr.erp.entity.InvestmentItem;
import com.skr.erp.entity.ProductionEntryDetail;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InventoryService {

    void createPurchaseBatch(InvestmentItem investmentItem);

    void createProductionBatch(ProductionEntryDetail productionDetail);

    public PageResponse<InventoryResponse> getInventory(Pageable pageable);

    ProductInventoryDetailResponse getProductBatchDetails(
            UUID productId,
            LocalDate fromDate,
            LocalDate toDate);

    List<LowStockAlertResponse> getLowStockAlerts();

    com.skr.erp.dto.response.InventoryBatchResponse adjustBatchStock(
            UUID batchId,
            com.skr.erp.dto.request.AdjustBatchStockRequest request);

    InventoryResponse adjustProductStock(
            UUID productId,
            com.skr.erp.dto.request.AdjustBatchStockRequest request);
}