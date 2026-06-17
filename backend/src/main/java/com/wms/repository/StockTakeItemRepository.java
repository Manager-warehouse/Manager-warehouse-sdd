package com.wms.repository;

import com.wms.entity.StockTakeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockTakeItemRepository extends JpaRepository<StockTakeItem, Long> {

    @Query("SELECT i FROM StockTakeItem i " +
           "JOIN FETCH i.product " +
           "JOIN FETCH i.batch " +
           "JOIN FETCH i.location " +
           "WHERE i.stockTake.id = :stockTakeId")
    List<StockTakeItem> findByStockTakeIdWithDetails(@Param("stockTakeId") Long stockTakeId);

    List<StockTakeItem> findByStockTakeId(Long stockTakeId);

    Optional<StockTakeItem> findByStockTakeIdAndProductIdAndBatchIdAndLocationId(
            Long stockTakeId, Long productId, Long batchId, Long locationId);

    // Returns true if any item in the stocktake still has no actual count recorded
    boolean existsByStockTakeIdAndActualQtyIsNull(Long stockTakeId);
}
