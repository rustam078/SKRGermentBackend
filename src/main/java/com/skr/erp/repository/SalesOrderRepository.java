package com.skr.erp.repository;

import com.skr.erp.dto.response.SalesSummaryProjection;
import com.skr.erp.entity.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID>,
        JpaSpecificationExecutor<SalesOrder> {

    boolean existsByInvoiceNo(String invoiceNumber);

    Page<SalesOrder> findByCustomerMobileOrderByCreatedAtDesc(
            String customerMobile,
            Pageable pageable);

    @Query("""
    SELECT
        COUNT(s) AS totalSales,
        COALESCE(SUM(s.grandTotal), 0) AS totalRevenue
    FROM SalesOrder s
    WHERE s.createdAt >= :start
      AND s.createdAt < :end
""")
    SalesSummaryProjection countAndRevenueBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );@Query("""
    SELECT
        COUNT(s) AS totalSales,
        COALESCE(SUM(s.grandTotal), 0) AS totalRevenue
    FROM SalesOrder s
""")
    SalesSummaryProjection countAndTotalRevenue();

    // ── Dashboard aggregations ───────────────────────────
    @Query("""
    SELECT COUNT(s), COALESCE(SUM(s.grandTotal), 0), COALESCE(SUM(s.discount), 0)
    FROM SalesOrder s
    WHERE s.createdAt >= :start AND s.createdAt < :end
""")
    List<Object[]> aggregateSalesBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
    SELECT CAST(s.createdAt AS date), COALESCE(SUM(s.grandTotal), 0), COUNT(s)
    FROM SalesOrder s
    WHERE s.createdAt >= :start AND s.createdAt < :end
    GROUP BY CAST(s.createdAt AS date)
    ORDER BY CAST(s.createdAt AS date)
""")
    List<Object[]> revenueSeriesBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
    SELECT s.paymentMode, COUNT(s), COALESCE(SUM(s.grandTotal), 0)
    FROM SalesOrder s
    WHERE s.createdAt >= :start AND s.createdAt < :end
    GROUP BY s.paymentMode
""")
    List<Object[]> paymentBreakdownBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT s FROM SalesOrder s ORDER BY s.createdAt DESC")
    List<SalesOrder> findRecent(Pageable pageable);
}
