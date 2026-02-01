import java.sql.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class DiagnosisAndFix {
    public static void main(String[] args) {
        String logFile = "fix_results.txt";
        String url = "jdbc:mysql://localhost:3306/distrozone_db";
        String user = "root";
        String password = "";

        try (PrintWriter log = new PrintWriter(new FileWriter(logFile, true))) {
            log.println("=== START DIAGNOSIS: " + new java.util.Date() + " ===");

            try (Connection conn = DriverManager.getConnection(url, user, password);
                    Statement stmt = conn.createStatement()) {

                log.println("Database connected.");
                DatabaseMetaData md = conn.getMetaData();
                ResultSet rs = md.getColumns(null, null, "TRANSAKSI", "nama_pelanggan");

                if (rs.next()) {
                    log.println("✅ Column 'nama_pelanggan' ALREADY EXISTS.");
                } else {
                    log.println("⚠️ Column 'nama_pelanggan' MISSING. Attempting to add...");
                    try {
                        String sql = "ALTER TABLE TRANSAKSI ADD COLUMN nama_pelanggan VARCHAR(100) DEFAULT 'Guest' AFTER customer_id";
                        stmt.executeUpdate(sql);
                        log.println("✅ SUCCESS: Column added via ALTER TABLE.");
                    } catch (Exception ex) {
                        log.println("❌ ERROR Adding Column: " + ex.getMessage());
                        ex.printStackTrace(log);
                    }
                }

                // Double check
                ResultSet rs2 = md.getColumns(null, null, "TRANSAKSI", "nama_pelanggan");
                if (rs2.next()) {
                    log.println("✅ VERIFICATION: Column exists in metadata.");
                } else {
                    log.println("❌ VERIFICATION: Column still NOT FOUND in metadata.");
                }

                // Check if web consistency is needed?
                // Just log that we are done with DB.

            } catch (Exception e) {
                log.println("❌ FATAL CONNECTION ERROR: " + e.getMessage());
                e.printStackTrace(log);
            }
            log.println("=== END DIAGNOSIS ===");
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
}
