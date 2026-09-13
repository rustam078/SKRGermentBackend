package com.skr.erp.repository;

import com.skr.erp.entity.InvestmentItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InvestmentItemRepository
        extends JpaRepository<InvestmentItem, UUID> {
    boolean existsByProductId(UUID productId);
}