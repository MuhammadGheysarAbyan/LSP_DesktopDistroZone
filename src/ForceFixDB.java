import java.sql.*;

public class ForceFixDB {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distrozone_db";
        String user = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password);
                Statement stmt = conn.createStatement()) {

            System.out.println("Force Fix: Adding nama_pelanggan column...");

            try {
                // Force Add Column
                String sql = "ALTER TABLE TRANSAKSI ADD COLUMN nama_pelanggan VARCHAR(100) DEFAULT 'Guest' AFTER customer_id";
                stmt.executeUpdate(sql);
                System.out.println("✅ SUCCESS: Column added.");
            } catch (SQLSyntaxErrorException e) {
                if (e.getMessage().contains("Duplicate column")) {
                    System.out.println("⚠️ Column already exists (Duplicate).");
                } else {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
