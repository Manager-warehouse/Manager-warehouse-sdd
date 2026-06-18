package com.wms.repository;

import com.wms.entity.Trip;
import com.wms.enums.TripStatus;
import com.wms.enums.TripType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    boolean existsByTripNumber(String tripNumber);
    boolean existsByDriverIdAndPlannedDateAndStatusIn(Long driverId, LocalDate plannedDate, List<TripStatus> statuses);
    boolean existsByVehicleIdAndPlannedDateAndStatusIn(Long vehicleId, LocalDate plannedDate, List<TripStatus> statuses);
    long countByTransferIdAndTripType(Long transferId, TripType tripType);
}
