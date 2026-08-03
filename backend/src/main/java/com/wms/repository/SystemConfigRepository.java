package com.wms.repository;


import com.wms.entity.user_configuration.SystemConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository truy vấn cấu hình hệ thống theo key (Spec 001). */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    Optional<SystemConfig> findByConfigKey(String configKey);
}
