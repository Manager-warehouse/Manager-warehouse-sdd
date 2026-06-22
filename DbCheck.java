import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbCheck {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require";
        try (Connection conn = DriverManager.getConnection(url, "postgres.jzniugklqehtghgzggiv", "Warehouse12345se12");
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Checking flyway:");
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5")) {
                while(rs.next()) {
                    System.out.println(rs.getString("version") + " " + rs.getString("description") + " " + rs.getBoolean("success"));
                }
            } catch (Exception e) { e.printStackTrace(); }

            System.out.println("\nChecking warehouses:");
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM warehouses WHERE type = 'IN_TRANSIT'")) {
                while(rs.next()) {
                    System.out.println("Warehouse: id=" + rs.getString("id") + " type=" + rs.getString("type") + " active=" + rs.getBoolean("is_active"));
                }
            } catch (Exception e) { e.printStackTrace(); }

            System.out.println("\nChecking warehouse locations:");
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM warehouse_locations WHERE type = 'BIN'")) {
                while(rs.next()) {
                    System.out.println("Location: id=" + rs.getString("id") + " warehouse_id=" + rs.getString("warehouse_id") + " type=" + rs.getString("type"));
                }
            } catch (Exception e) { e.printStackTrace(); }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
