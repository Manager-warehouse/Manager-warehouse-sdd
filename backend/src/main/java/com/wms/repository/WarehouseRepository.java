package com.wms.repository;

import com.wms.entity.Warehouse;
import com.wms.enums.WarehouseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    List<Warehouse> findByIsActive(Boolean isActive);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
    Optional<Warehouse> findFirstByTypeAndIsActiveTrue(WarehouseType type);
}
