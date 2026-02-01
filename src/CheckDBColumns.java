import java.sql.*;

public class CheckDBColumns {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distrozone_db";
        String user = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData md = conn.getMetaData();
            System.out.println("Connected to: " + md.getURL());
            ResultSet rs = md.getColumns(null, null, "transaksi", "%");

            System.out.println("Columns in TRANSAKSI table:");
            boolean found = false;
            while (rs.next()) {
                String colName = rs.getString("COLUMN_NAME");
                System.out.println("- " + colName);
                if ("nama_pelanggan".equalsIgnoreCase(colName)) {
                    found = true;
                }
            }

            if (found) {
                System.out.println("✅ RESULT: Column 'nama_pelanggan' FOUND.");
            } else {
                System.out.println("❌ RESULT: Column 'nama_pelanggan' NOT FOUND.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
