package com.skr.erp.controller;

import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.PaymentStatus;
import com.skr.erp.common.response.PageResponse;
import com.skr.erp.dto.request.CreateSaleOrderRequest;
import com.skr.erp.dto.response.*;
import com.skr.erp.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SalesController {

    private final SalesService salesService;

    @GetMapping
    public ResponseEntity<PageResponse<SalesListResponse>> getSales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(required = false) PaymentMode paymentMode,
            @RequestParam(required = false) PaymentStatus paymentStatus) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                salesService.getSales(
                        search, fromDate, toDate, paymentMode, paymentStatus, pageable)
        );
    }

    @GetMapping("/summary")
    public ResponseEntity<SalesSummaryResponse> getSalesSummary(
            @RequestParam(required = false) String search,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(required = false) PaymentMode paymentMode,
            @RequestParam(required = false) PaymentStatus paymentStatus) {

        return ResponseEntity.ok(
                salesService.getSalesSummary(search, fromDate, toDate, paymentMode, paymentStatus));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<SalesDashboardResponse> getDashboard() {
        return ResponseEntity.ok(salesService.getDashboard());
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<TopSellingProductResponse>> getTopProducts() {
        return ResponseEntity.ok(salesService.getTopProducts());
    }

    @GetMapping("/recent")
    public ResponseEntity<List<RecentSaleResponse>> getRecentSales() {
        return ResponseEntity.ok(salesService.getRecentSales());
    }

    @PostMapping
    public ResponseEntity<CreateSaleOrderResponse> createSale(
            @Valid @RequestBody CreateSaleOrderRequest request) throws BadRequestException {

        CreateSaleOrderResponse response = salesService.createSale(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{saleId}")
    public ResponseEntity<SalesDetailsResponse> getSaleDetails(
            @PathVariable UUID saleId) {
        return ResponseEntity.ok(salesService.getSaleDetails(saleId));
    }

    @GetMapping("/{saleId}/invoice")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @PathVariable UUID saleId) {
        return ResponseEntity.ok(salesService.getInvoice(saleId));
    }

    @GetMapping("/{saleId}/invoice/pdf")
    public ResponseEntity<byte[]> getInvoicePdf(
            @PathVariable UUID saleId) {

        byte[] pdf = salesService.exportInvoicePdf(saleId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=invoice-" + saleId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
