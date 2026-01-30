
import java.sql.*;

public class CheckKodeVarian {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distrozone_db";
        String user = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT kode_varian FROM KAOS_VARIAN LIMIT 10");
            System.out.println("Existing Kode Varian Examples:");
            while (rs.next()) {
                System.out.println(rs.getString("kode_varian"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
