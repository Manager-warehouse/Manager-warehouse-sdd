package com.wms.repository;


import com.wms.entity.audit_trail.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository truy vấn nhật ký hoạt động — hỗ trợ phân trang, lọc theo kho/thời gian (Spec 001). */
@Repository
public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
}
