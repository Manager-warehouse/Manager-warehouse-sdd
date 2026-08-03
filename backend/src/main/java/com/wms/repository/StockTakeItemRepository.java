package com.wms.repository;


import com.wms.entity.stock_counting.StockTakeItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository cho dòng hàng kiểm kê (stock_take_items).
 * Dùng bởi: StockTakeService
 */
@Repository
public interface StockTakeItemRepository extends JpaRepository<StockTakeItem, Long> {

    /** Load items kèm JOIN FETCH product, batch, location. Dùng bởi: buildResponse, executeApproval */
    @Query("SELECT i FROM StockTakeItem i " +
           "JOIN FETCH i.product " +
           "JOIN FETCH i.batch " +
           "JOIN FETCH i.location " +
           "WHERE i.stockTake.id = :stockTakeId")
    List<StockTakeItem> findByStockTakeIdWithDetails(@Param("stockTakeId") Long stockTakeId);

    /** Load items cơ bản (lazy). Dùng bởi: completeStockTake (refresh systemQty), lockLocations */
    List<StockTakeItem> findByStockTakeId(Long stockTakeId);

    /** Tìm item theo tổ hợp unique. Hiện chưa dùng trong service nhưng hỗ trợ lookup nếu cần */
    Optional<StockTakeItem> findByStockTakeIdAndProductIdAndBatchIdAndLocationId(
            Long stockTakeId, Long productId, Long batchId, Long locationId);

    /** Kiểm tra còn item nào chưa đếm (actual_qty = null). Dùng bởi: completeStockTake — chặn hoàn tất nếu chưa đủ */
    boolean existsByStockTakeIdAndActualQtyIsNull(Long stockTakeId);
}
