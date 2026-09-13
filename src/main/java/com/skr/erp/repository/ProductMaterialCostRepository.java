package com.skr.erp.repository;

import com.skr.erp.entity.ProductMaterialCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductMaterialCostRepository
        extends JpaRepository<ProductMaterialCost, UUID> {

    List<ProductMaterialCost>
    findByProductIdOrderByEffectiveFromDesc(
            UUID productId);

    Optional<ProductMaterialCost>
    findByProductIdAndEffectiveFrom(
            UUID productId,
            LocalDate effectiveFrom);

    Optional<ProductMaterialCost>
    findTopByProductIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            UUID productId,
            LocalDate effectiveFrom);

    boolean existsByProductId(
            UUID productId);

    @Query(value = """
SELECT *
FROM product_material_cost
WHERE product_id = :productId
ORDER BY effective_from DESC
""", nativeQuery = true)
    List<ProductMaterialCost> findAllByProduct(
            @Param("productId") UUID productId);
}