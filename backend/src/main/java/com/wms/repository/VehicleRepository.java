package com.wms.repository;

import com.wms.entity.Vehicle;
import com.wms.enums.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByIsActive(Boolean isActive);
    List<Vehicle> findByStatusAndIsActive(VehicleStatus status, Boolean isActive);
    boolean existsByPlateNumber(String plateNumber);
    boolean existsByPlateNumberAndIdNot(String plateNumber, Long id);
}
