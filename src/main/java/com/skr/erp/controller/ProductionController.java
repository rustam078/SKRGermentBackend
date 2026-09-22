package com.skr.erp.controller;

import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.dto.request.CreateProductionRequest;
import com.skr.erp.dto.response.ProductionDetailsResponse;
import com.skr.erp.dto.response.ProductionResponse;
import com.skr.erp.service.ProductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/production")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductionController {

    private final ProductionService productionService;

    @PostMapping
    public CommonResponse<ProductionResponse> create(@Valid @RequestBody CreateProductionRequest request) {
        return CommonResponse.<ProductionResponse>builder().success(true)
                .message("Production entry created successfully")
                .data(productionService.create(request)).build();
    }


    @GetMapping
    public CommonResponse<List<ProductionResponse>> search(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID productId) {

        return CommonResponse.<List<ProductionResponse>>builder().success(true)
                .message("Production entries fetched successfully")
                .data(productionService.search(fromDate, toDate, employeeId, productId))
                .build();
    }

    @GetMapping("/{id}")
    public CommonResponse<ProductionDetailsResponse> getById(@PathVariable UUID id) {
        return CommonResponse.<ProductionDetailsResponse>builder().success(true)
                .message("Production details fetched successfully")
                .data(productionService.getById(id)).build();
    }

    @PutMapping("/{id}")
    public CommonResponse<ProductionResponse> update(@PathVariable UUID id, @Valid @RequestBody CreateProductionRequest request) {
        return CommonResponse.<ProductionResponse>builder().success(true).message("Production updated successfully")
                .data(productionService.update(id, request)).build();
    }

    @DeleteMapping("/{id}")
    public CommonResponse<Void> delete(@PathVariable UUID id) {
        productionService.delete(id);
        return CommonResponse.<Void>builder().success(true)
                .message("Production deleted successfully").build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID id) {
        byte[] pdf = productionService.exportPdf(id);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=production-report.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    @GetMapping("/{id}/excel")
    public ResponseEntity<byte[]> exportExcel(@PathVariable UUID id) {
        byte[] excel = productionService.exportExcel(id);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=production-report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}