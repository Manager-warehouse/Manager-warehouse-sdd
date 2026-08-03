package com.wms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {
    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("FlywayConfig: Starting Flyway migration...");
            try {
                log.info("FlywayConfig: Repairing Flyway schema history...");
                flyway.repair();
                log.info("FlywayConfig: Running Flyway migration...");
                flyway.migrate();
                log.info("FlywayConfig: Flyway migration completed successfully.");
            } catch (Exception e) {
                throw new IllegalStateException("Flyway migration failed", e);
            }
        };
    }
}
