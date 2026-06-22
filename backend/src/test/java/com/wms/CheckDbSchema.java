package com.wms;

import org.junit.jupiter.api.Test;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckDbSchema {
    @Test
    public void check() throws Exception {
        String url = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require";
        String user = "postgres.jzniugklqehtghgzggiv";
        String password = "Warehouse12345se12";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             PrintWriter out = new PrintWriter(new FileWriter("check_db_schema.txt"))) {
            
            out.println("--- Constraints on audit_logs ---");
            ResultSet rs = stmt.executeQuery(
                "SELECT con.conname, pg_get_constraintdef(con.oid) " +
                "FROM pg_constraint con " +
                "JOIN pg_class rel ON rel.oid = con.conrelid " +
                "JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace " +
                "WHERE rel.relname = 'audit_logs'"
            );
            while (rs.next()) {
                out.println(rs.getString(1) + " | " + rs.getString(2));
            }
            
            out.println("--- Flyway Schema History ---");
            ResultSet rs2 = stmt.executeQuery(
                "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC"
            );
            while (rs2.next()) {
                out.println(rs2.getString(1) + " | " + rs2.getString(2) + " | " + rs2.getBoolean(3));
            }
        }
    }
}
