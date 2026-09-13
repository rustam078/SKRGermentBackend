package com.skr.erp.service;

import com.skr.erp.common.constants.ProductSource;
import com.skr.erp.dto.request.CreatePieceCodeRequest;
import com.skr.erp.dto.request.CreateProductRateRequest;
import com.skr.erp.dto.request.CreateProductRequest;
import com.skr.erp.dto.response.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProductService {

    ProductResponse create(CreateProductRequest request);

    List<ProductResponse> getAll();

    ProductRateResponse addRate(
            UUID productId,
            CreateProductRateRequest request
    );

    List<ProductRateResponse> getRates(UUID productId);

    BigDecimal getRateByDate(
            UUID productId,
            LocalDate date);

    ProductResponse toggleStatus(UUID productId);
    void delete(UUID productId);
    ProductDetailsResponse getById(UUID productId);
    PieceCodeResponse createPieceCode(
            UUID productId,
            CreatePieceCodeRequest request);

    List<PieceCodeResponse> getPieceCodes(
            UUID productId);

    PieceCodeResponse togglePieceCodeStatus(
            UUID pieceCodeId);

    void deletePieceCode(
            UUID pieceCodeId);
    List<PieceCodeDropdownResponse> getActivePieceCodes(UUID productId);
}