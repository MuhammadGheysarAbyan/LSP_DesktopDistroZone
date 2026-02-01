package util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class CodeGenerator {

    /**
     * Generates a transaction code in the format TRX-yyMMdd-NNN
     * Example: TRX-240130-001
     */
    public static String generateTransactionCode(Connection conn) throws SQLException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String datePart = sdf.format(new Date());
        String todayPrefix = "TRX-" + datePart + "-";

        String sql = "SELECT MAX(kode_transaksi) as last_code FROM TRANSAKSI WHERE kode_transaksi LIKE '" + todayPrefix
                + "%'";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                String lastCode = rs.getString("last_code");
                if (lastCode != null && !lastCode.isEmpty()) {
                    try {
                        String seqStr = lastCode.substring(lastCode.length() - 3);
                        int seq = Integer.parseInt(seqStr) + 1;
                        return todayPrefix + String.format("%03d", seq);
                    } catch (Exception e) {
                        // Fallback if parsing fails
                    }
                }
            }
        }
        return todayPrefix + "001";
    }

    /**
     * Generates a user code in the format ADM0001 or KSR0001
     * Fills gaps in the sequence.
     */
    public static String generateUserCode(Connection conn, String role) throws SQLException {
        String prefix = role.equalsIgnoreCase("admin") ? "ADM" : "KSR";

        // 1. Check for gaps (reuse deleted IDs logic)
        String sql = "SELECT user_code FROM users WHERE role = ? AND user_code LIKE ? ORDER BY user_code";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, role);
            pst.setString(2, prefix + "%");
            try (ResultSet rs = pst.executeQuery()) {
                HashMap<Integer, Boolean> usedNumbers = new HashMap<>();
                while (rs.next()) {
                    String code = rs.getString("user_code");
                    try {
                        // Extract number part (assuming format ADMxxxx)
                        // prefix length is 3
                        String numStr = code.substring(3);
                        int num = Integer.parseInt(numStr);
                        usedNumbers.put(num, true);
                    } catch (Exception ignored) {
                    }
                }

                // Find first missing number
                for (int i = 1; i <= 9999; i++) {
                    if (!usedNumbers.containsKey(i)) {
                        return String.format("%s%04d", prefix, i);
                    }
                }
            }
        }

        // 2. If no gaps, get max and increment
        sql = "SELECT MAX(CAST(SUBSTRING(user_code, 4) AS UNSIGNED)) as max_num FROM users WHERE role = ? AND user_code LIKE ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, role);
            pst.setString(2, prefix + "%");
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int maxNum = rs.getInt("max_num");
                    return String.format("%s%04d", prefix, maxNum + 1);
                }
            }
        }

        return prefix + "0001";
    }

    /**
     * Generates a variant code in the format KV-MMM-SS
     * MMM = Master ID (3 digits)
     * SS = Sequence Number (2 digits)
     * Example: KV-001-01
     */
    public static String generateVariantCode(Connection conn, int masterId) throws SQLException {
        // Get next sequence number for this masterId
        int nextSeq = 1;
        // Use SQL to parse the last part of KV-001-01
        // SUBSTRING_INDEX(kode_varian, '-', -1) gets the last part
        String seqSql = "SELECT MAX(CAST(SUBSTRING_INDEX(kode_varian, '-', -1) AS UNSIGNED)) FROM KAOS_VARIAN WHERE kaos_master_id = ?";
        try (PreparedStatement seqPst = conn.prepareStatement(seqSql)) {
            seqPst.setInt(1, masterId);
            try (ResultSet seqRs = seqPst.executeQuery()) {
                if (seqRs.next()) {
                    // Check if result is 0 (meaning NULL/no rows)
                    // getInt returns 0 if null, but let's check wasNull just in case logic differs
                    // Actually MAX returns NULL if no rows, so getInt returns 0.
                    // If there is data "KV-001-05", it returns 5.
                    // If table empty, returns 0.
                    // So nextSeq = 0 + 1 or 5 + 1
                    nextSeq = seqRs.getInt(1) + 1;
                }
            }
        }

        return String.format("KV-%03d-%02d", masterId, nextSeq);
    }

    /**
     * Formats the Kaos Master code for display (not stored in DB, usually just ID
     * is stored)
     * Example: KM-0001
     */
    public static String formatKaosMasterCode(int id) {
        return "KM-" + String.format("%04d", id);
    }
}
