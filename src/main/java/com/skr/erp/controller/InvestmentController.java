package com.skr.erp.controller;

import com.skr.erp.common.constants.InvestmentType;
import com.skr.erp.common.response.CommonResponse;
import com.skr.erp.dto.request.CreateInvestmentRequest;
import com.skr.erp.dto.response.InvestmentDetailsResponse;
import com.skr.erp.dto.response.InvestmentResponse;
import com.skr.erp.service.InvestmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InvestmentController {

    private final InvestmentService investmentService;

    @PostMapping
    public CommonResponse<InvestmentResponse> create(
            @Valid
            @RequestBody
            CreateInvestmentRequest request) {

        return CommonResponse
                .<InvestmentResponse>builder()
                .success(true)
                .message("Investment created successfully")
                .data(investmentService.create(request))
                .build();
    }

    @GetMapping
    public CommonResponse<List<InvestmentResponse>> search(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false)
            UUID vendorId,
            @RequestParam(required = false)
            InvestmentType type) {

        return CommonResponse
                .<List<InvestmentResponse>>builder()
                .success(true)
                .message("Investments fetched successfully")
                .data(
                        investmentService.search(
                                vendorId,
                                fromDate,
                                toDate,
                                type
                                ))
                .build();
    }

    @GetMapping("/{id}")
    public CommonResponse<InvestmentDetailsResponse> getById(
            @PathVariable UUID id) {

        return CommonResponse
                .<InvestmentDetailsResponse>builder()
                .success(true)
                .message("Investment fetched successfully")
                .data(
                        investmentService.getById(id))
                .build();
    }

    @DeleteMapping("/{id}")
    public CommonResponse<Void> delete(
            @PathVariable UUID id) {

        investmentService.delete(id);

        return CommonResponse
                .<Void>builder()
                .success(true)
                .message("Investment deleted successfully")
                .build();
    }
}