package com.wms.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConstraintFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseConstraintFixer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        System.out.println(">>> DatabaseConstraintFixer: Starting constraint updates...");
        try {
            // Drop constraint cũ (nếu có dưới các tên khác nhau)
            jdbcTemplate.execute("ALTER TABLE public.audit_logs DROP CONSTRAINT IF EXISTS chk_audit_logs_action");
            jdbcTemplate.execute("ALTER TABLE public.audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check");
            
            // Add constraint mới chứa VIEW_REPORT và các hành động điều chuyển mới
            jdbcTemplate.execute("ALTER TABLE public.audit_logs ADD CONSTRAINT chk_audit_logs_action " +
                    "CHECK (action IN ('LOGIN','LOGOUT','CREATE','UPDATE','STATUS_CHANGE','APPROVE','REJECT','CANCEL','SOFT_DELETE','ASSIGN','UNASSIGN','VIEW_REPORT'," +
                    "'TRANSFER_REQUEST_CREATE','TRANSFER_REQUEST_UPDATE','TRANSFER_REQUEST_SUBMIT','TRANSFER_REQUEST_CEO_APPROVE','TRANSFER_REQUEST_CEO_REJECT','TRANSFER_REQUEST_CONVERT'))");
            
            System.out.println(">>> DatabaseConstraintFixer: chk_audit_logs_action constraint updated successfully to include VIEW_REPORT and TRANSFER_REQUEST_* actions.");
        } catch (Exception e) {
            System.err.println(">>> DatabaseConstraintFixer: Failed to update audit_logs constraint: " + e.getMessage());
            // Không rethrow để tránh làm sập ứng dụng khởi động
        }
    }
}
