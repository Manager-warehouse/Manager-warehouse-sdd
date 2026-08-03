package com.wms.repository.driver_management;


import com.wms.entity.driver_management.Driver;
import com.wms.enums.driver_management.DriverStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findByIsActive(Boolean isActive);
    List<Driver> findByStatusAndIsActive(DriverStatus status, Boolean isActive);
    boolean existsByLicenseNumber(String licenseNumber);
    boolean existsByLicenseNumberAndIdNot(String licenseNumber, Long id);
    Optional<Driver> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    boolean existsByUserIdAndIdNot(Long userId, Long id);

    @EntityGraph(attributePaths = {"warehouse", "user"})
    Optional<Driver> findWithWarehouseAndUserById(Long id);
}
