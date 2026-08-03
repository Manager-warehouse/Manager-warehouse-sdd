package com.wms.repository;


import com.wms.entity.stock_control.Batch;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    /**
     * Find a batch by its unique number.
     */
    Optional<Batch> findByBatchNumber(String batchNumber);

    boolean existsByBatchNumber(String batchNumber);

    Optional<Batch> findByBatchCode(String batchCode);

    boolean existsByBatchCode(String batchCode);

    /**
     * Find a batch using the household-goods batch resolution key:
     * product + warehouse + receivedDate (no grade, no expiry).
     * Used during receipt approval to resolve or create batch idempotently.
     */
    @Query("SELECT b FROM Batch b " +
           "WHERE b.product.id = :productId " +
           "AND b.warehouse.id = :warehouseId " +
           "AND b.receivedDate = :receivedDate")
    Optional<Batch> findByProductWarehouseAndReceivedDate(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            @Param("receivedDate") LocalDate receivedDate);
}
