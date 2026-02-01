import java.sql.*;

public class MigrationMerek {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distrozone_db";
        String user = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password);
                Statement stmt = conn.createStatement()) {

            System.out.println("Starting Migration for MEREK table...");

            // 1. Create MEREK Table
            String createTable = "CREATE TABLE IF NOT EXISTS MEREK (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "nama_merek VARCHAR(50) NOT NULL UNIQUE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            stmt.execute(createTable);
            System.out.println("✅ Table MEREK created or already exists.");

            // 2. Populate from KAOS_MASTER
            System.out.println("Migrating distinct brands from KAOS_MASTER...");
            String migrateSql = "INSERT IGNORE INTO MEREK (nama_merek) " +
                    "SELECT DISTINCT merek FROM KAOS_MASTER WHERE merek IS NOT NULL AND merek != ''";
            int rows = stmt.executeUpdate(migrateSql);
            System.out.println("✅ Migrated " + rows + " existing brands.");

            // Check content
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM MEREK");
            if (rs.next()) {
                System.out.println("Total Brands in MEREK table: " + rs.getInt(1));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
