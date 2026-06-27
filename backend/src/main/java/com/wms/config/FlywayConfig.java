package com.wms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Configuration
public class FlywayConfig {
    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(DataSource dataSource) {
        return flyway -> {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                log.info("FlywayConfig: Listing schema history before cleanup...");
                try (ResultSet rs = stmt.executeQuery("SELECT installed_rank, version, description, type, success FROM flyway_schema_history ORDER BY installed_rank")) {
                    while (rs.next()) {
                        log.info("FlywayConfig: rank={}, version={}, desc={}, type={}, success={}",
                                rs.getInt("installed_rank"),
                                rs.getString("version"),
                                rs.getString("description"),
                                rs.getString("type"),
                                rs.getBoolean("success"));
                    }
                } catch (Exception ex) {
                    log.warn("FlywayConfig: Could not query flyway_schema_history table: {}", ex.getMessage());
                }

                // Delete failed/corrupted entries
                int deletedCount = stmt.executeUpdate("DELETE FROM flyway_schema_history WHERE success = false");
                log.info("FlywayConfig: Cleaned up {} failed flyway schema history entries.", deletedCount);
                
            } catch (Exception e) {
                log.error("FlywayConfig: Database pre-migration cleanup failed: {}", e.getMessage(), e);
            }
            
            try {
                log.info("FlywayConfig: Running flyway.repair()...");
                flyway.repair();
                log.info("FlywayConfig: Flyway repair completed successfully.");
            } catch (Exception e) {
                log.error("FlywayConfig: Flyway repair failed: {}", e.getMessage(), e);
            }

            try {
                log.info("FlywayConfig: Running flyway.migrate()...");
                flyway.migrate();
                log.info("FlywayConfig: Flyway migrate completed successfully.");
            } catch (Exception e) {
                log.error("FlywayConfig: Flyway migrate failed: {}", e.getMessage(), e);
                throw e; // Rethrow to let Spring Boot know migration failed
            }
        };
    }
}
