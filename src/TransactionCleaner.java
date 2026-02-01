
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

public class TransactionCleaner {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distrozone_db";
        String user = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password);
                Statement stmt = conn.createStatement()) {

            System.out.println("🔄 Memulai pembersihan data transaksi...");

            // Disable foreign key checks to allow truncation
            stmt.execute("SET FOREIGN_KEY_CHECKS=0");

            // Truncate tables
            stmt.executeUpdate("TRUNCATE TABLE DETAIL_TRANSAKSI");
            System.out.println("✅ Tabel DETAIL_TRANSAKSI dibersihkan.");

            stmt.executeUpdate("TRUNCATE TABLE TRANSAKSI");
            System.out.println("✅ Tabel TRANSAKSI dibersihkan.");

            // Enable foreign key checks
            stmt.execute("SET FOREIGN_KEY_CHECKS=1");

            System.out.println("🎉 Selesai! Semua data transaksi dan pembayaran telah dihapus.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Gagal membersihkan data: " + e.getMessage());
        }
    }
}
