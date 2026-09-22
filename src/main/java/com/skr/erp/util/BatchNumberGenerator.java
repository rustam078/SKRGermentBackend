package com.skr.erp.util;

import com.skr.erp.repository.InventoryBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchNumberGenerator {
    private final InventoryBatchRepository inventoryBatchRepository;
    public String generate() {
        Long sequence = inventoryBatchRepository.getNextBatchSequence();
        return String.format("BT%06d", sequence);
    }
}