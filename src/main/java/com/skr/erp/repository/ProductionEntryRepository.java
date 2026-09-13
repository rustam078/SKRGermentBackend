package com.skr.erp.repository;

import com.skr.erp.entity.ProductionEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ProductionEntryRepository
        extends JpaRepository<ProductionEntry, UUID>,
        JpaSpecificationExecutor<ProductionEntry> {
}