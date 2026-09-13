package com.skr.erp.service;

import com.skr.erp.common.constants.ProductSource;
import com.skr.erp.common.constants.RateStatus;
import com.skr.erp.dto.request.CreatePieceCodeRequest;
import com.skr.erp.dto.request.CreateProductRateRequest;
import com.skr.erp.dto.request.CreateProductRequest;
import com.skr.erp.dto.response.*;
import com.skr.erp.entity.Product;
import com.skr.erp.entity.ProductPieceCode;
import com.skr.erp.entity.ProductRate;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductRateRepository productRateRepository;
    private final ProductionEntryDetailRepository productionEntryDetailRepository;
    private final ProductPieceCodeRepository productPieceCodeRepository;
    private final InvestmentItemRepository investmentItemRepository;

    @Override
    public ProductResponse create(CreateProductRequest request) {

        productRepository.findByNameIgnoreCase(request.getName())
                .ifPresent(product -> {
                    throw new RuntimeException("Product already exists");
                });

        Product product = new Product();

        product.setName(request.getName());
        product.setIconName(request.getIconName());
        product.setSource(ProductSource.MANUFACTURED);
        product.setDescription(request.getDescription());
        product.setActive(true);


        product = productRepository.save(product);

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .iconName(product.getIconName())
                .description(product.getDescription())
                .active(product.getActive())
                .build();
    }

    @Override
    public List<ProductResponse> getAll() {

        return productRepository.findAll()
                .stream()
                .map(product -> {

                    int totalCodes =
                            (int) productPieceCodeRepository
                                    .countByProductId(
                                            product.getId());

                    int activeCodes =
                            (int) productPieceCodeRepository
                                    .countByProductIdAndActiveTrue(
                                            product.getId());

                    int inactiveCodes =
                            (int) productPieceCodeRepository
                                    .countByProductIdAndActiveFalse(
                                            product.getId());

                    return ProductResponse.builder()
                            .id(product.getId())
                            .name(product.getName())
                            .iconName(product.getIconName())
                            .description(product.getDescription())
                            .active(product.getActive())
                            .source(product.getSource())
                            .totalPieceCodes(
                                    totalCodes)

                            .activePieceCodes(
                                    activeCodes)

                            .inactivePieceCodes(
                                    inactiveCodes)
                            .createdAt(product.getCreatedAt())
                            .updatedAt(product.getUpdatedAt())
                            .build();
                })
                .toList();
    }

    @Override
    public ProductRateResponse addRate(
            UUID productId,
            CreateProductRateRequest request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new BusinessException("Product not found"));

        if (!product.getActive()) {
            throw new BusinessException("Cannot add rate for inactive product");
        }

        productRateRepository.findByProductIdAndEffectiveFrom(
                        productId,
                        request.getEffectiveFrom()
                )
                .ifPresent(rate -> {
                    throw new BusinessException(
                            "Rate already exists for this date"
                    );
                });

        ProductRate rate = new ProductRate();

        rate.setProduct(product);
        rate.setRate(request.getRate());
        rate.setEffectiveFrom(request.getEffectiveFrom());

        rate = productRateRepository.save(rate);

        return ProductRateResponse.builder()
                .id(rate.getId())
                .productId(product.getId())
                .productName(product.getName())
                .rate(rate.getRate())
                .effectiveFrom(rate.getEffectiveFrom())
                .build();
    }

    @Override
    public List<ProductRateResponse> getRates(UUID productId) {

        List<ProductRate> rates =
                productRateRepository
                        .findByProductIdOrderByEffectiveFromDesc(productId);

        LocalDate today = LocalDate.now();

        ProductRate activeRate =
                productRateRepository
                        .findTopByProductIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                productId,
                                today
                        )
                        .orElse(null);

        LocalDate activeDate =
                activeRate != null
                        ? activeRate.getEffectiveFrom()
                        : null;

        return rates.stream()
                .map(rate -> ProductRateResponse.builder()
                        .id(rate.getId())
                        .productId(rate.getProduct().getId())
                        .productName(rate.getProduct().getName())
                        .rate(rate.getRate())
                        .effectiveFrom(rate.getEffectiveFrom())
                        .status(
                                activeDate == null
                                        ? RateStatus.SCHEDULED
                                        : determineStatus(
                                        rate,
                                        today,
                                        activeDate
                                )
                        )
                        .build())
                .toList();
    }

    @Override
    public BigDecimal getRateByDate(
            UUID productId,
            LocalDate date) {

        return productRateRepository
                .findTopByProductIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        productId,
                        date)
                .map(ProductRate::getRate)
                .orElse(BigDecimal.ZERO);
    }

//    @Override
//    public ProductRateResponse getCurrentRate(UUID productId) {
//
//        ProductRate rate =
//                productRateRepository
//                        .findTopByProductIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
//                                productId,
//                                LocalDate.now()
//                        )
//                        .orElseThrow(() ->
//                                new RuntimeException(
//                                        "No active rate found"
//                                ));
//
//        return ProductRateResponse.builder()
//                .id(rate.getId())
//                .productId(rate.getProduct().getId())
//                .productName(rate.getProduct().getName())
//                .rate(rate.getRate())
//                .effectiveFrom(rate.getEffectiveFrom())
//                .status(RateStatus.ACTIVE)
//                .build();
//    }

    private RateStatus determineStatus(
            ProductRate rate,
            LocalDate currentDate,
            LocalDate activeDate) {

        if (rate.getEffectiveFrom().isAfter(currentDate)) {
            return RateStatus.SCHEDULED;
        }

        if (rate.getEffectiveFrom().equals(activeDate)) {
            return RateStatus.ACTIVE;
        }

        return RateStatus.EXPIRED;
    }


    @Override
    public ProductResponse toggleStatus(UUID productId) {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new BusinessException(
                                "Product not found"));

        product.setActive(
                !product.getActive());

        product = productRepository.save(product);

        BigDecimal currentRate =
                productRateRepository
                        .findTopByProductIdOrderByEffectiveFromDesc(
                                product.getId())
                        .map(ProductRate::getRate)
                        .orElse(BigDecimal.ZERO);

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .iconName(product.getIconName())
                .active(product.getActive())
                .build();
    }

    @Override
    public void delete(UUID productId) {

        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Product not found"));

        boolean used =
                productionEntryDetailRepository
                        .existsByProductId(
                                productId);

        if (used) {

            throw new BusinessException(
                    "Product has production history and cannot be deleted. Please deactivate it instead.");
        }
        if (investmentItemRepository.existsByProductId(productId)) {

            throw new BusinessException(
                    "Product cannot be deleted because it has already been used in purchase invoices.");
        }

        productRepository.delete(product);
    }

    @Override
    public ProductDetailsResponse getById(
            UUID productId) {

        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Product not found"));

        List<ProductPieceCodeResponse> pieceCodes =
                productPieceCodeRepository
                        .findByProductId(productId)
                        .stream()
                        .map(pieceCode -> {

                            boolean used =
                                    productionEntryDetailRepository
                                            .existsByPieceCodeId(
                                                    pieceCode.getId());

                            return ProductPieceCodeResponse
                                    .builder()
                                    .id(pieceCode.getId())
                                    .code(pieceCode.getCode())
                                    .rate(pieceCode.getRate())
                                    .active(pieceCode.getActive())
                                    .used(used)
                                    .createdAt(pieceCode.getCreatedAt())
                                    .build();
                        })
                        .toList();

        return ProductDetailsResponse
                .builder()
                .id(product.getId())
                .name(product.getName())
                .source(product.getSource())
                .iconName(product.getIconName())
                .description(product.getDescription())
                .active(product.getActive())

                .totalPieceCodes(
                        (int) productPieceCodeRepository
                                .countByProductId(
                                        productId))

                .activePieceCodes(
                        (int) productPieceCodeRepository
                                .countByProductIdAndActiveTrue(
                                        productId))

                .inactivePieceCodes(
                        (int) productPieceCodeRepository
                                .countByProductIdAndActiveFalse(
                                        productId))

                .pieceCodes(pieceCodes)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())

                .build();
    }

    @Override
    public PieceCodeResponse createPieceCode(
            UUID productId,
            CreatePieceCodeRequest request) {

        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Product not found"));

        if (productPieceCodeRepository
                .existsByCodeIgnoreCase(
                        request.getCode())) {

            throw new BusinessException(
                    "Piece code already exists");
        }

        ProductPieceCode pieceCode =
                new ProductPieceCode();

        pieceCode.setProduct(product);
        pieceCode.setCode(
                request.getCode()
                        .trim()
                        .toUpperCase());

        pieceCode.setRate(
                request.getRate());

        pieceCode.setActive(true);

        pieceCode =
                productPieceCodeRepository
                        .save(pieceCode);

        return PieceCodeResponse.builder()
                .id(pieceCode.getId())
                .productId(product.getId())
                .productName(product.getName())
                .code(pieceCode.getCode())
                .rate(pieceCode.getRate())
                .active(pieceCode.getActive())
                .used(false)
                .createdAt(pieceCode.getCreatedAt())
                .build();
    }

    @Override
    public List<PieceCodeResponse> getPieceCodes(
            UUID productId) {

        return productPieceCodeRepository
                .findByProduct_Id(productId)
                .stream()
                .map(pieceCode -> {

                    boolean used =
                            productionEntryDetailRepository
                                    .existsByPieceCodeId(
                                            pieceCode.getId());

                    return PieceCodeResponse.builder()
                            .id(pieceCode.getId())
                            .productId(
                                    pieceCode.getProduct()
                                            .getId())
                            .productName(
                                    pieceCode.getProduct()
                                            .getName())
                            .code(
                                    pieceCode.getCode())
                            .rate(
                                    pieceCode.getRate())
                            .active(
                                    pieceCode.getActive())
                            .used(used)
                            .createdAt(pieceCode.getCreatedAt())
                            .build();
                })
                .toList();
    }

    @Override
    public PieceCodeResponse togglePieceCodeStatus(
            UUID pieceCodeId) {

        ProductPieceCode pieceCode =
                productPieceCodeRepository
                        .findById(pieceCodeId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Piece code not found"));

        pieceCode.setActive(
                !pieceCode.getActive());

        pieceCode =
                productPieceCodeRepository
                        .save(pieceCode);

        boolean used =
                productionEntryDetailRepository
                        .existsByPieceCodeId(
                                pieceCode.getId());

        return PieceCodeResponse.builder()
                .id(pieceCode.getId())
                .productId(
                        pieceCode.getProduct()
                                .getId())
                .productName(
                        pieceCode.getProduct()
                                .getName())
                .code(pieceCode.getCode())
                .rate(pieceCode.getRate())
                .active(pieceCode.getActive())
                .used(used)
                .createdAt(pieceCode.getCreatedAt())
                .build();
    }

    @Override
    public void deletePieceCode(
            UUID pieceCodeId) {

        ProductPieceCode pieceCode =
                productPieceCodeRepository
                        .findById(pieceCodeId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Piece code not found"));

        boolean used =
                productionEntryDetailRepository
                        .existsByPieceCodeId(
                                pieceCodeId);

        if (used) {

            throw new BusinessException(
                    "Piece code has production history. Deactivate it instead.");
        }

        productPieceCodeRepository
                .delete(pieceCode);
    }

    @Override
    public List<PieceCodeDropdownResponse>
    getActivePieceCodes(UUID productId) {

        return productPieceCodeRepository
                .findByProductIdAndActiveTrueOrderByCodeAsc(
                        productId)
                .stream()
                .map(pc ->
                        PieceCodeDropdownResponse
                                .builder()
                                .id(pc.getId())
                                .code(pc.getCode())
                                .rate(pc.getRate())
                                .build())
                .toList();
    }
}