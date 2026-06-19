package com.wms.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableMethodSecurity
@EnableJpaRepositories(basePackages = "com.wms.repository")
@EntityScan(basePackages = "com.wms.entity")
public class JpaConfig {
}
