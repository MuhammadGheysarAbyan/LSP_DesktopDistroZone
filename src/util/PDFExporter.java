package util;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Desktop;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Utility class untuk export laporan ke format HTML/PDF
 * HTML digunakan karena bisa langsung di-print sebagai PDF dari browser
 */
public class PDFExporter {

    private static final String GREEN_PRIMARY = "#10B981";
    private static final String GREEN_DARK = "#047857";
    private static final String GREEN_LIGHT = "#ECFDF5";

    /**
     * Export tabel ke file HTML yang bisa di-print sebagai PDF
     * 
     * @param table      JTable yang akan di-export
     * @param title      Judul laporan
     * @param subtitle   Subjudul (misal nama kasir, periode)
     * @param statistics Map statistik (key=label, value=nilai)
     * @param outputFile File output (akan ditambah .html jika belum ada)
     * @return true jika berhasil
     */
    public static boolean exportTableToPDF(JTable table, String title, String subtitle,
            Map<String, String> statistics, File outputFile) {

        try {
            // Pastikan ekstensi .html
            if (!outputFile.getName().toLowerCase().endsWith(".html")) {
                outputFile = new File(outputFile.getAbsolutePath() + ".html");
            }

            DefaultTableModel model = (DefaultTableModel) table.getModel();

            StringBuilder html = new StringBuilder();

            // HTML Header dengan styling untuk print
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang='id'>\n<head>\n");
            html.append("<meta charset='UTF-8'>\n");
            html.append("<title>").append(title).append("</title>\n");
            html.append("<style>\n");
            html.append("@page { size: A4; margin: 15mm; }\n");
            html.append(
                    "@media print { body { -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; } }\n");
            html.append("* { margin: 0; padding: 0; box-sizing: border-box; }\n");
            html.append(
                    "body { font-family: 'Segoe UI', Arial, sans-serif; padding: 20px; color: #333; background: #fff; }\n");
            html.append(
                    ".header { text-align: center; margin-bottom: 25px; padding-bottom: 15px; border-bottom: 3px solid ")
                    .append(GREEN_PRIMARY).append("; }\n");
            html.append(".header h1 { color: ").append(GREEN_DARK).append("; font-size: 24px; margin-bottom: 5px; }\n");
            html.append(".header h2 { color: #666; font-size: 16px; font-weight: normal; }\n");
            html.append(".header .date { color: #888; font-size: 12px; margin-top: 10px; }\n");
            html.append(
                    ".stats { display: flex; flex-wrap: wrap; gap: 15px; margin-bottom: 25px; justify-content: center; }\n");
            html.append(".stat-card { background: ").append(GREEN_LIGHT)
                    .append("; padding: 15px 20px; border-radius: 8px; ");
            html.append("border-left: 4px solid ").append(GREEN_PRIMARY)
                    .append("; min-width: 150px; text-align: center; }\n");
            html.append(
                    ".stat-card .label { font-size: 11px; color: #666; text-transform: uppercase; letter-spacing: 0.5px; }\n");
            html.append(".stat-card .value { font-size: 18px; font-weight: bold; color: ").append(GREEN_DARK)
                    .append("; margin-top: 5px; }\n");
            html.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; font-size: 12px; }\n");
            html.append("th { background: ").append(GREEN_PRIMARY)
                    .append("; color: white; padding: 12px 8px; text-align: left; font-weight: 600; }\n");
            html.append("td { padding: 10px 8px; border-bottom: 1px solid #e0e0e0; }\n");
            html.append("tr:nth-child(even) { background: #f9f9f9; }\n");
            html.append("tr:hover { background: ").append(GREEN_LIGHT).append("; }\n");
            html.append(
                    ".footer { text-align: center; margin-top: 30px; padding-top: 15px; border-top: 1px solid #ddd; color: #888; font-size: 11px; }\n");
            html.append(
                    ".print-info { background: #fff3cd; padding: 10px; border-radius: 5px; margin-bottom: 20px; text-align: center; font-size: 12px; }\n");
            html.append("@media print { .print-info { display: none; } }\n");
            html.append("</style>\n</head>\n<body>\n");

            // Print instruction (hidden when printing)
            html.append("<div class='print-info'>\n");
            html.append("💡 <strong>Tip:</strong> Tekan <kbd>Ctrl</kbd>+<kbd>P</kbd> untuk menyimpan sebagai PDF\n");
            html.append("</div>\n");

            // Header
            html.append("<div class='header'>\n");
            html.append("<h1>📊 ").append(title).append("</h1>\n");
            if (subtitle != null && !subtitle.isEmpty()) {
                html.append("<h2>").append(subtitle).append("</h2>\n");
            }
            html.append("<div class='date'>Dicetak: ")
                    .append(new SimpleDateFormat("dd MMMM yyyy, HH:mm:ss").format(new Date())).append("</div>\n");
            html.append("</div>\n");

            // Statistics
            if (statistics != null && !statistics.isEmpty()) {
                html.append("<div class='stats'>\n");
                for (Map.Entry<String, String> stat : statistics.entrySet()) {
                    html.append("<div class='stat-card'>\n");
                    html.append("<div class='label'>").append(stat.getKey()).append("</div>\n");
                    html.append("<div class='value'>").append(stat.getValue()).append("</div>\n");
                    html.append("</div>\n");
                }
                html.append("</div>\n");
            }

            // Table
            html.append("<table>\n<thead>\n<tr>\n");

            // Table headers (skip last column if it's "Lihat" or action button)
            int colCount = model.getColumnCount();
            String lastColName = model.getColumnName(colCount - 1);
            if (lastColName.equalsIgnoreCase("Detail") || lastColName.equalsIgnoreCase("Aksi") ||
                    lastColName.equalsIgnoreCase("Lihat") || lastColName.contains("Lihat")) {
                colCount--;
            }

            for (int i = 0; i < colCount; i++) {
                html.append("<th>").append(model.getColumnName(i)).append("</th>\n");
            }
            html.append("</tr>\n</thead>\n<tbody>\n");

            // Table data
            for (int row = 0; row < model.getRowCount(); row++) {
                html.append("<tr>\n");
                for (int col = 0; col < colCount; col++) {
                    Object value = model.getValueAt(row, col);
                    String cellValue = value != null ? value.toString() : "-";
                    // Clean up emoji/status symbols for cleaner PDF
                    cellValue = cellValue.replace("✅ ", "").replace("⏳ ", "").replace("❌ ", "");
                    html.append("<td>").append(cellValue).append("</td>\n");
                }
                html.append("</tr>\n");
            }

            html.append("</tbody>\n</table>\n");

            // Footer
            html.append("<div class='footer'>\n");
            html.append("<p><strong>DistroZone</strong> - Sistem Manajemen Toko</p>\n");
            html.append("<p>Total Data: ").append(model.getRowCount()).append(" baris</p>\n");
            html.append("</div>\n");

            html.append("</body>\n</html>");

            // Write to file
            try (PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"))) {
                writer.print(html.toString());
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Buka file di browser default
     */
    public static void openInBrowser(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(file.toURI());
            } else {
                // Fallback untuk sistem tanpa Desktop support
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Tidak dapat membuka browser.\nSilakan buka file secara manual:\n" + file.getAbsolutePath(),
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Export dengan dialog file chooser
     */
    public static void exportWithDialog(JTable table, String title, String subtitle,
            Map<String, String> statistics, java.awt.Component parent, String defaultFileName) {

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Simpan Laporan PDF");
        fileChooser.setSelectedFile(new File(defaultFileName + ".html"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("HTML Files (*.html)", "html"));

        if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            if (exportTableToPDF(table, title, subtitle, statistics, file)) {
                // Pastikan ekstensi benar
                if (!file.getName().toLowerCase().endsWith(".html")) {
                    file = new File(file.getAbsolutePath() + ".html");
                }

                int choice = JOptionPane.showConfirmDialog(parent,
                        "✅ Laporan berhasil diexport!\n\nFile: " + file.getAbsolutePath() +
                                "\n\nBuka di browser sekarang?",
                        "Export Berhasil",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);

                if (choice == JOptionPane.YES_OPTION) {
                    openInBrowser(file);
                }
            } else {
                JOptionPane.showMessageDialog(parent,
                        "❌ Gagal mengexport laporan!",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
