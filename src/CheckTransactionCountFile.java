import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.io.FileWriter;

public class CheckTransactionCountFile {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distrozone_db";
        String user = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password);
                Statement stmt = conn.createStatement();
                FileWriter fw = new FileWriter("verification.txt")) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM transaksi");
            if (rs.next()) {
                int count = rs.getInt(1);
                fw.write("TRANSACTION_COUNT: " + count + "\n");
            }

            rs = stmt.executeQuery("SELECT COUNT(*) FROM detail_transaksi");
            if (rs.next()) {
                int count = rs.getInt(1);
                fw.write("DETAIL_COUNT: " + count + "\n");
            }

        } catch (Exception e) {
            try {
                FileWriter fw = new FileWriter("verification.txt");
                fw.write("ERROR: " + e.getMessage());
                fw.close();
            } catch (Exception ex) {
            }
        }
    }
}
