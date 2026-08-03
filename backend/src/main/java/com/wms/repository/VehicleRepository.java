package com.wms.repository;


import com.wms.entity.fleet_management.Vehicle;
import com.wms.enums.fleet_management.VehicleStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByIsActive(Boolean isActive);
    List<Vehicle> findByWarehouseIdAndIsActiveTrue(Long warehouseId);
    List<Vehicle> findByStatusAndIsActive(VehicleStatus status, Boolean isActive);
    boolean existsByPlateNumber(String plateNumber);
    boolean existsByPlateNumberAndIdNot(String plateNumber, Long id);

    @EntityGraph(attributePaths = {"warehouse"})
    Optional<Vehicle> findWithWarehouseById(Long id);
}
