package com.skr.erp.service;

import com.skr.erp.common.constants.InvestmentType;
import com.skr.erp.dto.request.AddPaymentRequest;
import com.skr.erp.dto.request.CreateInvestmentRequest;
import com.skr.erp.dto.response.InvestmentDetailsResponse;
import com.skr.erp.dto.response.InvestmentPaymentResponse;
import com.skr.erp.dto.response.InvestmentResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InvestmentService {

    InvestmentResponse create(
            CreateInvestmentRequest request);

    public List<InvestmentResponse> search(
            UUID vendorId,
            LocalDate fromDate,
            LocalDate toDate,
            InvestmentType type,
            String search);

    InvestmentDetailsResponse getById(
            UUID id);

    void delete(
            UUID id);

    // ── Payments ──────────────────────────────────
    InvestmentDetailsResponse addPayment(UUID investmentId, AddPaymentRequest request);

    List<InvestmentPaymentResponse> getPayments(UUID investmentId);

    // ── Invoice PDF ───────────────────────────────
    byte[] exportInvoicePdf(UUID id);
}