package com.skr.erp.repository;

import com.skr.erp.entity.ProductionEntryDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProductionEntryDetailRepository
        extends JpaRepository<ProductionEntryDetail, UUID> {
    boolean existsByProductId(UUID productId);
    boolean existsByPieceCodeId(UUID pieceCodeId);

    @Query("""
            SELECT COUNT(DISTINCT d.productionEntry.id),
                   COALESCE(SUM(d.quantity), 0),
                   COALESCE(SUM(d.amountSnapshot), 0)
            FROM ProductionEntryDetail d
            WHERE d.productionEntry.productionDate >= :from
              AND d.productionEntry.productionDate <= :to
            """)
    List<Object[]> aggregateProductionBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT e.fullName,
                   COALESCE(SUM(d.quantity), 0),
                   COALESCE(SUM(d.amountSnapshot), 0)
            FROM ProductionEntryDetail d
            JOIN d.productionEntry pe
            JOIN pe.employee e
            WHERE pe.productionDate >= :from AND pe.productionDate <= :to
            GROUP BY e.id, e.fullName
            ORDER BY SUM(d.quantity) DESC
            """)
    List<Object[]> topEmployeesBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    @Query("""
            SELECT pe.productionDate, COALESCE(SUM(d.amountSnapshot), 0)
            FROM ProductionEntryDetail d
            JOIN d.productionEntry pe
            WHERE pe.productionDate >= :from AND pe.productionDate <= :to
            GROUP BY pe.productionDate
            ORDER BY pe.productionDate
            """)
    List<Object[]> wagesSeriesBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}