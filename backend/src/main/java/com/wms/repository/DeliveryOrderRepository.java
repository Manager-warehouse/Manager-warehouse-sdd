package com.wms.repository;

import com.wms.entity.DeliveryOrder;
import com.wms.enums.DeliveryOrderStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {
    boolean existsByDoNumber(String doNumber);
    List<DeliveryOrder> findByDealerIdAndStatus(Long dealerId, DeliveryOrderStatus status);

    @EntityGraph(attributePaths = {"dealer", "warehouse", "createdBy"})
    @Query("select d from DeliveryOrder d where d.id = :id")
    Optional<DeliveryOrder> findWithDealerAndWarehouseById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"dealer", "warehouse", "createdBy"})
    @Query("""
            select d from DeliveryOrder d
            where d.id = :id
              and d.status in :statuses
            """)
    Optional<DeliveryOrder> findWithDealerAndWarehouseByIdAndStatusIn(@Param("id") Long id,
                                                                       @Param("statuses") Collection<DeliveryOrderStatus> statuses);

    @EntityGraph(attributePaths = {"dealer", "warehouse", "createdBy", "packedBy", "qcBy"})
    @Query("""
            select d from DeliveryOrder d
            where d.warehouse.id = :warehouseId
              and d.status in :statuses
            order by d.updatedAt desc
            """)
    List<DeliveryOrder> findDetailedByWarehouseIdAndStatusIn(@Param("warehouseId") Long warehouseId,
                                                             @Param("statuses") Collection<DeliveryOrderStatus> statuses);
}
