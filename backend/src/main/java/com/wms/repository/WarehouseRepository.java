package com.wms.repository;


import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.warehouse_location.WarehouseType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    List<Warehouse> findByIsActive(Boolean isActive);
    Optional<Warehouse> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
    Optional<Warehouse> findFirstByTypeAndIsActiveTrue(WarehouseType type);
}
