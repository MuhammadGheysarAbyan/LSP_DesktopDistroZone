import java.sql.*;

public class MigrationNamaPelanggan {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distrozone_db";
        String user = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password);
                Statement stmt = conn.createStatement()) {

            System.out.println("Starting Migration for nama_pelanggan column...");

            // Check if column exists
            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getColumns(null, null, "TRANSAKSI", "nama_pelanggan");

            if (rs.next()) {
                System.out.println("✅ Column nama_pelanggan already exists.");
            } else {
                String alterTable = "ALTER TABLE TRANSAKSI ADD COLUMN nama_pelanggan VARCHAR(100) DEFAULT NULL AFTER customer_id";
                stmt.execute(alterTable);
                System.out.println("✅ Column nama_pelanggan added successfully.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
