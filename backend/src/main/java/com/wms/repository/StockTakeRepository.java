package com.wms.repository;

import com.wms.entity.StockTake;
import com.wms.enums.StockTakeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockTakeRepository extends JpaRepository<StockTake, Long> {

    List<StockTake> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);

    List<StockTake> findByWarehouseIdAndStatusOrderByCreatedAtDesc(Long warehouseId, StockTakeStatus status);

    @Query("SELECT st FROM StockTake st " +
           "LEFT JOIN FETCH st.warehouse " +
           "LEFT JOIN FETCH st.conductedBy " +
           "LEFT JOIN FETCH st.accountingPeriod " +
           "WHERE st.id = :id")
    Optional<StockTake> findByIdWithDetails(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT st FROM StockTake st WHERE st.id = :id")
    Optional<StockTake> findByIdForUpdate(@Param("id") Long id);
}
