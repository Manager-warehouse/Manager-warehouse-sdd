package com.wms.repository;

import com.wms.entity.WarehouseLocation;
import com.wms.enums.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Long> {
    List<WarehouseLocation> findByWarehouseId(Long warehouseId);
    List<WarehouseLocation> findByWarehouseIdAndType(Long warehouseId, LocationType type);
    List<WarehouseLocation> findByParentId(Long parentId);
    long countByParentIdAndIsActiveTrue(Long parentId);
    boolean existsByWarehouseIdAndCode(Long warehouseId, String code);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}
