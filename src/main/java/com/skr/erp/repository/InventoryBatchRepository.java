package com.skr.erp.repository;

import com.skr.erp.entity.InventoryBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, UUID> {

    List<InventoryBatch> findByProductId(UUID productId);

    java.util.Optional<InventoryBatch> findByBatchNumber(String batchNumber);

    @Query(value = "SELECT nextval('inventory_batch_seq')", nativeQuery = true)
    Long getNextBatchSequence();

    @Query(value = """
                SELECT b FROM InventoryBatch b
                JOIN FETCH b.product p
            """, countQuery = """
                SELECT COUNT(b) FROM InventoryBatch b
            """)
    Page<InventoryBatch> findAllWithProduct(Pageable pageable);

    @Query("""
            SELECT b FROM InventoryBatch b
            JOIN FETCH b.product p
            WHERE p.id = :productId
            ORDER BY b.createdAt DESC
            """)
    List<InventoryBatch> findAllByProductId(@Param("productId") UUID productId);

    @Query("""
            SELECT b FROM InventoryBatch b
            JOIN FETCH b.product p
            WHERE p.id = :productId
            AND b.receivedDate >= :fromDate
            AND b.receivedDate <= :toDate
            ORDER BY b.createdAt DESC
            """)
    List<InventoryBatch> findAllByProductIdAndReceivedDateBetween(@Param("productId") UUID productId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM InventoryBatch b
            JOIN FETCH b.product
            WHERE b.product.id = :productId
            AND b.quantityAvailable > 0
            AND b.status = com.skr.erp.common.constants.InventoryBatchStatus.ACTIVE
            ORDER BY b.receivedDate ASC
            """)
    List<InventoryBatch> findAvailableBatchesForSale(UUID productId);

    @Query("""
            select b
            from InventoryBatch b
            join fetch b.product p
            order by p.name
            """)
    List<InventoryBatch> findAllBatchesWithProduct();

    @Query("""
            SELECT COALESCE(SUM(b.quantityAvailable), 0),
                   COALESCE(SUM(b.quantityAvailable * b.unitCost), 0)
            FROM InventoryBatch b
            WHERE b.status = com.skr.erp.common.constants.InventoryBatchStatus.ACTIVE
            """)
    List<Object[]> aggregateActiveInventory();
}
