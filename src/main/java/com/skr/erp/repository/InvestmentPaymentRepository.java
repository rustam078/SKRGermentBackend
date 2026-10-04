package com.skr.erp.repository;

import com.skr.erp.entity.InvestmentPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InvestmentPaymentRepository extends JpaRepository<InvestmentPayment, UUID> {

    List<InvestmentPayment> findByInvestmentIdOrderByPaymentDateDescCreatedAtDesc(UUID investmentId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM InvestmentPayment p WHERE p.investment.id = :id")
    BigDecimal sumByInvestment(@Param("id") UUID id);

    // Total paid per investment in one query (for the list view).
    @Query("SELECT p.investment.id, COALESCE(SUM(p.amount), 0) FROM InvestmentPayment p GROUP BY p.investment.id")
    List<Object[]> paidSums();

    // All payments for a vendor (newest first), with the invoice loaded for the history view.
    @Query("SELECT p FROM InvestmentPayment p JOIN FETCH p.investment i WHERE i.vendor.id = :vendorId ORDER BY p.createdAt DESC")
    List<InvestmentPayment> findByVendorId(@Param("vendorId") UUID vendorId);
}
