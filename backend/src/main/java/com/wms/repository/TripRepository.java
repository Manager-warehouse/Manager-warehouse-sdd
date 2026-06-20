package com.wms.repository;

import com.wms.entity.Trip;
import com.wms.enums.TripStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    boolean existsByTripNumber(String tripNumber);

    @EntityGraph(attributePaths = {"warehouse", "vehicle", "driver", "driver.user", "dispatcher"})
    Optional<Trip> findWithWarehouseAndResourcesById(Long id);

    @EntityGraph(attributePaths = {"warehouse", "vehicle", "driver", "driver.user", "dispatcher"})
    @Query("""
            select t from Trip t
            where t.warehouse.id in :warehouseIds
              and (:status is null or t.status = :status)
            order by t.createdAt desc, t.id desc
            """)
    List<Trip> findByWarehouseIdInAndOptionalStatus(@Param("warehouseIds") Collection<Long> warehouseIds,
                                                    @Param("status") TripStatus status);

    @EntityGraph(attributePaths = {"warehouse", "vehicle", "driver", "driver.user", "dispatcher"})
    @Query("""
            select t from Trip t
            where t.id = :tripId
              and t.driver.user.id = :userId
            """)
    Optional<Trip> findAssignedDriverTrip(@Param("tripId") Long tripId, @Param("userId") Long userId);

    @Query("""
            select count(t) > 0 from Trip t
            where t.vehicle.id = :vehicleId
              and t.status in :statuses
              and (:excludedTripId is null or t.id <> :excludedTripId)
            """)
    boolean existsActiveVehicleAssignment(@Param("vehicleId") Long vehicleId,
                                          @Param("statuses") Collection<TripStatus> statuses,
                                          @Param("excludedTripId") Long excludedTripId);

    @Query("""
            select count(t) > 0 from Trip t
            where t.driver.id = :driverId
              and t.status in :statuses
              and (:excludedTripId is null or t.id <> :excludedTripId)
            """)
    boolean existsActiveDriverAssignment(@Param("driverId") Long driverId,
                                         @Param("statuses") Collection<TripStatus> statuses,
                                         @Param("excludedTripId") Long excludedTripId);
}
