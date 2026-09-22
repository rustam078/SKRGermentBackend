package com.skr.erp.controller;

import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.common.response.PageResponse;
import com.skr.erp.dto.request.AdjustBatchStockRequest;
import com.skr.erp.dto.response.InventoryBatchResponse;
import com.skr.erp.dto.response.InventoryResponse;
import com.skr.erp.dto.response.LowStockAlertResponse;
import com.skr.erp.dto.response.ProductInventoryDetailResponse;
import com.skr.erp.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<?> getInventory(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<InventoryResponse> inventory = inventoryService.getInventory(pageable);
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/product/{productId}/batches")
    public ResponseEntity<ProductInventoryDetailResponse> getProductBatches(@PathVariable UUID productId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(inventoryService.getProductBatchDetails(productId, fromDate, toDate));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<LowStockAlertResponse>> getLowStockAlerts() {
        return ResponseEntity.ok(inventoryService.getLowStockAlerts());
    }

    @PatchMapping("/batches/{batchId}/adjust")
    public ResponseEntity<CommonResponse<InventoryBatchResponse>> adjustBatchStock(@PathVariable UUID batchId, @Valid @RequestBody AdjustBatchStockRequest request) {
        InventoryBatchResponse updated = inventoryService.adjustBatchStock(batchId, request);
        return ResponseEntity.ok(CommonResponse.<InventoryBatchResponse>builder().success(true)
                .message("Batch stock adjusted successfully")
                .data(updated).build());
    }

    @PatchMapping("/product/{productId}/adjust")
    public ResponseEntity<CommonResponse<InventoryResponse>> adjustProductStock(@PathVariable UUID productId, @Valid @RequestBody AdjustBatchStockRequest request) {
        InventoryResponse updated = inventoryService.adjustProductStock(productId, request);
        return ResponseEntity.ok(CommonResponse.<InventoryResponse>builder().success(true)
                .message("Stock adjusted successfully")
                .data(updated).build());
    }
}