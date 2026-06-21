package com.wms.repository;

import com.wms.entity.Trip;
import com.wms.enums.TripStatus;
import com.wms.enums.TripType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    boolean existsByTripNumber(String tripNumber);
    long countByTransferIdAndTripType(Long transferId, TripType tripType);

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
            where t.vehicle.id = :vehicleId
              and t.status in :statuses
              and t.plannedStartAt < :plannedEndAt
              and t.plannedEndAt > :plannedStartAt
            """)
    boolean existsVehicleScheduleOverlap(@Param("vehicleId") Long vehicleId,
                                         @Param("plannedStartAt") LocalDateTime plannedStartAt,
                                         @Param("plannedEndAt") LocalDateTime plannedEndAt,
                                         @Param("statuses") List<TripStatus> statuses);
}
