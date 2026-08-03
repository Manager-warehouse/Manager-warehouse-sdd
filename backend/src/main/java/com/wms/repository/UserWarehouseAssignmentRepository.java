package com.wms.repository;


import com.wms.entity.access_control.User;
import com.wms.entity.access_control.UserWarehouseAssignment;
import com.wms.enums.access_control.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Repository quản lý phân công kho cho user (Spec 001). */
@Repository
public interface UserWarehouseAssignmentRepository extends JpaRepository<UserWarehouseAssignment, Long> {
    
    @Query("SELECT u.warehouse.id FROM UserWarehouseAssignment u WHERE u.user.id = :userId")
    List<Long> findWarehouseIdsByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserWarehouseAssignment u WHERE u.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT u.user FROM UserWarehouseAssignment u WHERE u.warehouse.id = :warehouseId AND u.user.role = com.wms.enums.access_control.UserRole.WAREHOUSE_MANAGER")
    List<User> findWarehouseManagersByWarehouseId(@Param("warehouseId") Long warehouseId);
}


