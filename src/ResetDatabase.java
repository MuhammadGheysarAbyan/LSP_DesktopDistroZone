import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ResetDatabase {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distrozone_db";
        String user = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password);
                Statement stmt = conn.createStatement()) {

            System.out.println("Connected to database. Clearing transaction history...");

            // Disable foreign key checks to allow truncation
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            // Clear tables
            stmt.executeUpdate("TRUNCATE TABLE detail_transaksi");
            System.out.println("- Truncated detail_transaksi");

            stmt.executeUpdate("TRUNCATE TABLE payment_proof");
            System.out.println("- Truncated payment_proof");

            stmt.executeUpdate("TRUNCATE TABLE transaksi");
            System.out.println("- Truncated transaksi");

            // Re-enable foreign key checks
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            System.out.println("Transaction history cleared successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
