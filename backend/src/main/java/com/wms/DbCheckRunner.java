package com.wms;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

@Component
public class DbCheckRunner implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public DbCheckRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("====== DB CHECK START ======");
        List<Map<String, Object>> warehouses = jdbcTemplate.queryForList("SELECT id, type, is_active FROM warehouses WHERE type = 'IN_TRANSIT'");
        System.out.println("IN_TRANSIT WAREHOUSES: " + warehouses);
        
        List<Map<String, Object>> flyway = jdbcTemplate.queryForList("SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5");
        System.out.println("FLYWAY HISTORY: " + flyway);
        System.out.println("====== DB CHECK END ======");
    }
}
