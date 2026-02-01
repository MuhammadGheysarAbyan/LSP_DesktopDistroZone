import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckTransactionCount {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distrozone_db";
        String user = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password);
                Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM transaksi");
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("TRANSACTION_COUNT: " + count);
            }

            rs = stmt.executeQuery("SELECT COUNT(*) FROM detail_transaksi");
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("DETAIL_COUNT: " + count);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
