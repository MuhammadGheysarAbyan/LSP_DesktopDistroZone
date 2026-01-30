import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeleteTesKaos {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/distrozone_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    public static void main(String[] args) {
        System.out.println("🔍 Mencari kaos dengan nama mengandung 'tes'...");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String findSql = "SELECT id, nama_kaos FROM KAOS_MASTER WHERE nama_kaos LIKE '%tes%'";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(findSql)) {
                List<Integer> idsToDelete = new ArrayList<>();
                List<String> namesToDelete = new ArrayList<>();

                while (rs.next()) {
                    idsToDelete.add(rs.getInt("id"));
                    namesToDelete.add(rs.getString("nama_kaos"));
                }

                if (idsToDelete.isEmpty()) {
                    System.out.println("⚠️ Tidak ditemukan kaos dengan nama 'tes'.");
                    return;
                }

                System.out.println("✅ Ditemukan " + idsToDelete.size() + " kaos:");
                for (String name : namesToDelete) {
                    System.out.println("   - " + name);
                }

                for (int id : idsToDelete) {
                    deleteKaos(conn, id);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void deleteKaos(Connection conn, int kaosId) throws SQLException {
        System.out.println("\n🚀 Memulai penghapusan untuk Kaos ID: " + kaosId);

        // 1. Get Transaction IDs
        String getTrxSql = "SELECT DISTINCT dt.transaksi_id FROM DETAIL_TRANSAKSI dt " +
                "JOIN KAOS_VARIAN kv ON dt.kaos_id = kv.id " +
                "WHERE kv.kaos_master_id = ?";
        PreparedStatement pstGet = conn.prepareStatement(getTrxSql);
        pstGet.setInt(1, kaosId);
        ResultSet rsTrx = pstGet.executeQuery();
        List<Integer> trxIds = new ArrayList<>();
        while (rsTrx.next()) {
            trxIds.add(rsTrx.getInt("transaksi_id"));
        }

        if (!trxIds.isEmpty()) {
            System.out.println("   📦 Ditemukan " + trxIds.size() + " transaksi terkait. Menghapus...");
            StringBuilder inClause = new StringBuilder();
            for (int i = 0; i < trxIds.size(); i++) {
                inClause.append("?");
                if (i < trxIds.size() - 1)
                    inClause.append(",");
            }

            // Delete Details
            String delDetail = "DELETE FROM DETAIL_TRANSAKSI WHERE transaksi_id IN (" + inClause.toString() + ")";
            PreparedStatement pstDelDet = conn.prepareStatement(delDetail);
            for (int i = 0; i < trxIds.size(); i++)
                pstDelDet.setInt(i + 1, trxIds.get(i));
            int detCount = pstDelDet.executeUpdate();
            System.out.println("      - " + detCount + " baris detail transaksi dihapus.");

            // Delete Header
            String delHeader = "DELETE FROM TRANSAKSI WHERE id IN (" + inClause.toString() + ")";
            PreparedStatement pstDelHead = conn.prepareStatement(delHeader);
            for (int i = 0; i < trxIds.size(); i++)
                pstDelHead.setInt(i + 1, trxIds.get(i));
            int headCount = pstDelHead.executeUpdate();
            System.out.println("      - " + headCount + " header transaksi dihapus.");
        } else {
            System.out.println("   ✅ Tidak ada transaksi terkait.");
        }

        // Hapus varian
        String deleteVarianSql = "DELETE FROM KAOS_VARIAN WHERE kaos_master_id = ?";
        PreparedStatement deleteVarianStmt = conn.prepareStatement(deleteVarianSql);
        deleteVarianStmt.setInt(1, kaosId);
        int varianCount = deleteVarianStmt.executeUpdate();
        System.out.println("   - " + varianCount + " varian dihapus.");

        // Hapus dari KAOS_MASTER
        String deleteKaosSql = "DELETE FROM KAOS_MASTER WHERE id = ?";
        PreparedStatement deleteKaosStmt = conn.prepareStatement(deleteKaosSql);
        deleteKaosStmt.setInt(1, kaosId);
        deleteKaosStmt.executeUpdate();

        System.out.println("✅ Kaos ID " + kaosId + " BERHASIL DIHAPUS.");
    }
}
