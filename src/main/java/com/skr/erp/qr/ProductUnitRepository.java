package com.skr.erp.qr;

import com.skr.erp.common.constants.ProductUnitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductUnitRepository extends JpaRepository<ProductUnit, UUID> {

    Optional<ProductUnit> findBySerial(String serial);

    List<ProductUnit> findByBatchNumberOrderBySerialAsc(String batchNumber);

    /** AVAILABLE units of a batch, newest serial first — used to trim labels when stock is reduced. */
    List<ProductUnit> findByBatchNumberAndStatusOrderBySerialDesc(String batchNumber, ProductUnitStatus status);

    long countByBatchNumber(String batchNumber);

    long countByBatchNumberAndStatus(String batchNumber, ProductUnitStatus status);

    /** Highest serial suffix already used for a batch, so generation never collides. */
    @Query(value = """
        SELECT COALESCE(MAX(CAST(split_part(serial, '-', 2) AS INTEGER)), 0)
        FROM product_unit
        WHERE batch_number = :batchNumber
        """, nativeQuery = true)
    int maxSerialIndex(@Param("batchNumber") String batchNumber);
}
