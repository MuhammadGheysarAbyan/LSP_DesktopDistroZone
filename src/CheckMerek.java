import java.sql.*;

public class CheckMerek {
    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/distrozone_db", "root", "");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM MEREK")) {

            System.out.println("--- MEREK TABLE CONTENT ---");
            while (rs.next()) {
                System.out.println(rs.getInt("id") + ": " + rs.getString("nama_merek"));
            }
            System.out.println("---------------------------");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
