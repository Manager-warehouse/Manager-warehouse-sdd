package com.wms.repository;

import com.wms.entity.Trip;
import com.wms.enums.TripStatus;
import com.wms.enums.TripType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    boolean existsByTripNumber(String tripNumber);

    @Query("""
            select count(t) > 0
            from Trip t
            where t.driver.id = :driverId
              and t.status in :statuses
              and t.plannedStartAt < :plannedEndAt
              and t.plannedEndAt > :plannedStartAt
            """)
    boolean existsDriverScheduleOverlap(@Param("driverId") Long driverId,
                                        @Param("plannedStartAt") LocalDateTime plannedStartAt,
                                        @Param("plannedEndAt") LocalDateTime plannedEndAt,
                                        @Param("statuses") List<TripStatus> statuses);

    @Query("""
            select count(t) > 0
            from Trip t
            where t.driver.id = :driverId
              and t.status in :statuses
              and t.plannedStartAt < :plannedEndAt
              and t.plannedEndAt > :plannedStartAt
              and (:excludedTripId is null or t.id <> :excludedTripId)
            """)
    boolean existsDriverScheduleOverlapExcludingTrip(@Param("driverId") Long driverId,
                                                    @Param("plannedStartAt") LocalDateTime plannedStartAt,
                                                    @Param("plannedEndAt") LocalDateTime plannedEndAt,
                                                    @Param("statuses") List<TripStatus> statuses,
                                                    @Param("excludedTripId") Long excludedTripId);

    @Query("""
            select count(t) > 0
            from Trip t
            where t.vehicle.id = :vehicleId
              and t.status in :statuses
              and t.plannedStartAt < :plannedEndAt
              and t.plannedEndAt > :plannedStartAt
            """)
    boolean existsVehicleScheduleOverlap(@Param("vehicleId") Long vehicleId,
                                         @Param("plannedStartAt") LocalDateTime plannedStartAt,
                                         @Param("plannedEndAt") LocalDateTime plannedEndAt,
                                         @Param("statuses") List<TripStatus> statuses);

    @Query("""
            select count(t) > 0
            from Trip t
            where t.vehicle.id = :vehicleId
              and t.status in :statuses
              and t.plannedStartAt < :plannedEndAt
              and t.plannedEndAt > :plannedStartAt
              and (:excludedTripId is null or t.id <> :excludedTripId)
            """)
    boolean existsVehicleScheduleOverlapExcludingTrip(@Param("vehicleId") Long vehicleId,
                                                     @Param("plannedStartAt") LocalDateTime plannedStartAt,
                                                     @Param("plannedEndAt") LocalDateTime plannedEndAt,
                                                     @Param("statuses") List<TripStatus> statuses,
                                                     @Param("excludedTripId") Long excludedTripId);

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

    @EntityGraph(attributePaths = {"warehouse", "vehicle", "driver", "driver.user", "dispatcher"})
    @Query("""
            select t from Trip t
            where t.driver.user.id = :userId
            order by t.createdAt desc, t.id desc
            """)
    List<Trip> findAssignedDriverTrips(@Param("userId") Long userId);

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

    java.util.List<Trip> findByStatusAndCompletedAtBetween(TripStatus status, java.time.OffsetDateTime start, java.time.OffsetDateTime end);

    java.util.List<Trip> findByWarehouseIdAndStatusAndCompletedAtBetween(Long warehouseId, TripStatus status, java.time.OffsetDateTime start, java.time.OffsetDateTime end);
}

