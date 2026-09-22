package com.skr.erp.repository;

import com.skr.erp.entity.ProductPieceCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductPieceCodeRepository extends JpaRepository<ProductPieceCode, UUID> {

    List<ProductPieceCode> findByProductIdAndActiveTrue(UUID productId);

    boolean existsByCode(String code);

    long countByProductId(UUID productId);

    long countByProductIdAndActiveTrue(UUID productId);

    long countByProductIdAndActiveFalse(UUID productId);

    List<ProductPieceCode> findByProductId(UUID productId);

    boolean existsByCodeIgnoreCase(String code);

    List<ProductPieceCode> findByProduct_Id(UUID productId);

    List<ProductPieceCode> findByProductIdAndActiveTrueOrderByCodeAsc(UUID productId);

}