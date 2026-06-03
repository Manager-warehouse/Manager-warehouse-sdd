package com.wms.repository;

import com.wms.entity.UserWarehouseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserWarehouseAssignmentRepository extends JpaRepository<UserWarehouseAssignment, Long> {
    
    @Query("SELECT u.warehouse.id FROM UserWarehouseAssignment u WHERE u.user.id = :userId")
    List<Long> findWarehouseIdsByUserId(@Param("userId") Long userId);
}
