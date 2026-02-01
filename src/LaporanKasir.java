import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.text.SimpleDateFormat;

public class LaporanKasir extends JFrame {
    private int kasirId;
    private String kasirName;
    private String userCode;
    private JTable table;
    private DefaultTableModel model;
    private JSpinner dariTanggal, sampaiTanggal;
    private JComboBox<String> comboFilter, comboStatus, comboPayment;
    private DecimalFormat df;
    private JLabel lblTotalOmset, lblTotalTransaksi, lblRataTransaksi, lblLaba;
    private JLabel lblTotalTunai, lblTotalQRIS, lblTotalTransfer;
    private JPanel statsPanel;

    // WARNA THEME DISTRO ZONE (Emerald Green - Sesuai Web)
    private static final Color GREEN_PRIMARY = new Color(16, 185, 129); // #10B981
    private static final Color GREEN_LIGHT = new Color(52, 211, 153); // #34D399
    private static final Color TEXT_DARK = new Color(31, 41, 55); // #1F2937
    private static final Color TEXT_GRAY = new Color(100, 116, 139); // #64748B

    // ================= KONSTRUKTOR =================
    public LaporanKasir(int kasirId, String kasirName, String userCode) {
        this.kasirId = kasirId;
        this.kasirName = kasirName;
        this.userCode = userCode;

        // Format angka ribuan pakai titik
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        df = new DecimalFormat("#,###", symbols);

        setTitle("📊 Laporan Kasir - ");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();
        initData();
    }

    private void initUI() {
        // ================= PANEL UTAMA DENGAN GRADIENT =================
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, GREEN_PRIMARY, 0, getHeight(), Color.WHITE);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        add(mainPanel);

        // ================= HEADER =================
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel lblHeader = new JLabel(" Laporan Transaksi Kasir", JLabel.CENTER);
        ImageIcon headerIcon = loadImageIcon("report.png", 40);
        if (headerIcon != null)
            lblHeader.setIcon(headerIcon);
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblHeader.setForeground(Color.WHITE);

        headerPanel.add(lblHeader, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ================= WRAPPER UNTUK CENTER (STATS + TABLE) =================
        JPanel centerWrapper = new JPanel(new BorderLayout(0, 20));
        centerWrapper.setOpaque(false);

        mainPanel.add(centerWrapper, BorderLayout.CENTER);

        // ================= STATISTIK PANEL =================
        statsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        lblTotalTransaksi = new JLabel("0");
        lblTotalOmset = new JLabel("Rp 0"); // Keep reference but don't display
        lblRataTransaksi = new JLabel("Rp 0"); // Keep reference but don't display
        lblLaba = new JLabel("Rp 0"); // Keep reference but don't display

        lblTotalTunai = new JLabel("Rp 0");
        lblTotalQRIS = new JLabel("Rp 0");
        lblTotalTransfer = new JLabel("Rp 0");

        // Kasir only sees Total Transaksi - no financial data (omzet, laba, rata-rata)
        // UPDATE: User requested Payment Breakdown
        statsPanel.add(createStatCard("Total Transaksi", lblTotalTransaksi, new Color(33, 150, 243)));
        statsPanel.add(createStatCard("Total Tunai", lblTotalTunai, GREEN_PRIMARY));
        statsPanel.add(createStatCard("Total QRIS", lblTotalQRIS, new Color(156, 39, 176)));
        statsPanel.add(createStatCard("Total Transfer", lblTotalTransfer, new Color(255, 152, 0)));

        centerWrapper.add(statsPanel, BorderLayout.NORTH);

        // ================= CONTENT BOX =================
        JPanel contentBox = new JPanel(new BorderLayout(10, 10));
        contentBox.setBackground(new Color(255, 255, 255, 245));
        contentBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREEN_PRIMARY, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        centerWrapper.add(contentBox, BorderLayout.CENTER);

        // ================= FILTER PANEL =================
        JPanel filterPanel = new JPanel(new BorderLayout(10, 10));
        filterPanel.setOpaque(false);
        filterPanel.setBackground(new Color(255, 255, 255, 200));

        JPanel filterBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterBox.setOpaque(false);

        // ComboBox untuk jenis filter
        comboFilter = new JComboBox<>(new String[] { "Semua", "Harian", "Bulanan", "Rentang Tanggal" });
        comboFilter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboFilter.setBackground(Color.WHITE);

        // ComboBox untuk status transaksi - UPDATED to match DB values while keeping
        // user friendly
        comboStatus = new JComboBox<>(new String[] { "Semua Status", "selesai", "pending", "dibatalkan" });
        comboStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboStatus.setBackground(Color.WHITE);

        // ComboBox untuk payment method - BCA only
        comboPayment = new JComboBox<>(
                new String[] { "Semua Metode", "Tunai", "QRIS", "Transfer BCA" });
        comboPayment.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboPayment.setBackground(Color.WHITE);

        dariTanggal = new JSpinner(new SpinnerDateModel());
        dariTanggal.setEditor(new JSpinner.DateEditor(dariTanggal, "yyyy-MM-dd"));
        dariTanggal.setPreferredSize(new Dimension(120, 30));

        sampaiTanggal = new JSpinner(new SpinnerDateModel());
        sampaiTanggal.setEditor(new JSpinner.DateEditor(sampaiTanggal, "yyyy-MM-dd"));
        sampaiTanggal.setPreferredSize(new Dimension(120, 30));

        JButton btnFilter = createButton("Filter", GREEN_PRIMARY, "filter.png");
        JButton btnReset = createButton("Reset", new Color(255, 152, 0), "reset.png"); // Orange
        JButton btnExport = createButton("Export", new Color(156, 39, 176), "export.png"); // Purple
        JButton btnPrint = createButton("Cetak", new Color(52, 152, 219), "report.png"); // Blue

        filterBox.add(new JLabel("Jenis Filter:"));
        filterBox.add(comboFilter);
        filterBox.add(new JLabel("Status:"));
        filterBox.add(comboStatus);
        filterBox.add(new JLabel("Metode:"));
        filterBox.add(comboPayment);
        filterBox.add(new JLabel("Dari:"));
        filterBox.add(dariTanggal);
        filterBox.add(new JLabel("Sampai:"));
        filterBox.add(sampaiTanggal);
        filterBox.add(btnFilter);
        filterBox.add(btnReset);
        filterBox.add(btnExport);
        filterBox.add(btnPrint);

        filterPanel.add(filterBox, BorderLayout.CENTER);
        contentBox.add(filterPanel, BorderLayout.NORTH);

        // ================= TABEL =================
        // Kasir tidak bisa lihat Laba - hanya admin yang bisa
        model = new DefaultTableModel(
                new String[] {
                        "Kode Transaksi", "Tanggal", "Waktu", "Metode", "Pelanggan",
                        "Total", "Status", "Detail"
                }, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(35);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        // Custom Header Renderer to ensure color is applied
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(GREEN_PRIMARY);
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
                setHorizontalAlignment(SwingConstants.CENTER); // Center align headers to match content
                return this;
            }
        };
        table.getTableHeader().setDefaultRenderer(headerRenderer);
        table.getTableHeader().setPreferredSize(new Dimension(table.getColumnModel().getTotalColumnWidth(), 40));

        table.setSelectionBackground(GREEN_LIGHT);
        table.setSelectionForeground(TEXT_DARK);

        // Center alignment untuk beberapa kolom
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        // Right alignment untuk kolom numeric
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        for (int i = 0; i < model.getColumnCount(); i++) {
            if (i == 5) { // Kolom Total (shifted by 1 due to Pelanggan)
                table.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
            } else if (i == 7) { // Kolom Detail is now index 7
                DefaultTableCellRenderer buttonRenderer = new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value,
                            boolean isSelected, boolean hasFocus, int row, int column) {
                        JButton button = new JButton("Lihat");
                        button.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                        button.setForeground(Color.WHITE);
                        button.setFocusPainted(false);
                        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                        button.setContentAreaFilled(false);
                        button.setOpaque(true);
                        button.setBackground(new Color(33, 150, 243));

                        // Fix for rendering glitch in table
                        if (isSelected) {
                            button.setBackground(new Color(33, 150, 243).darker());
                        }

                        return button;
                    }
                };
                table.getColumnModel().getColumn(i).setCellRenderer(buttonRenderer);
            } else {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        JScrollPane sp = new JScrollPane(table);
        contentBox.add(sp, BorderLayout.CENTER);

        // ================= BUTTON PANEL =================
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnPanel.setOpaque(false);

        JButton btnBack = createButton("Kembali", TEXT_GRAY, "back.png");
        btnPanel.add(btnBack);

        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        // ================= EVENT HANDLERS =================
        btnBack.addActionListener(e -> {
            dispose();
            new KasirDashboard(kasirId, kasirName, userCode).setVisible(true);
        });

        btnExport.addActionListener(e -> exportToExcel());
        btnPrint.addActionListener(e -> printLaporan());

        btnFilter.addActionListener(e -> applyFilter());

        btnReset.addActionListener(e -> {
            comboFilter.setSelectedIndex(0);
            comboStatus.setSelectedIndex(0);
            comboPayment.setSelectedIndex(0); // Reset payment
            setTanggalHariIni();
            loadData(null, null, null, null);
            updateStatistics(null, null, null, null);
        });

        comboFilter.addActionListener(e -> {
            String selectedFilter = (String) comboFilter.getSelectedItem();
            switch (selectedFilter) {
                case "Harian":
                    setTanggalHariIni();
                    break;
                case "Bulanan":
                    setTanggalBulanIni();
                    break;
                case "Rentang Tanggal":
                    // Biarkan user memilih tanggal manual
                    break;
                default:
                    break;
            }
        });

        // Double click untuk lihat detail
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.rowAtPoint(evt.getPoint());
                int col = table.columnAtPoint(evt.getPoint());
                if (row >= 0 && col == 7) { // Kolom Detail is now index 7
                    showDetailTransaksi(row);
                }
            }
        });
    }

    private void initData() {
        setTanggalHariIni();
        loadData(null, null, null, null);
        updateStatistics(null, null, null, null);
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color color) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        // Ensure minimum size so it looks like a card not a button
        card.setPreferredSize(new Dimension(200, 80));

        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(color);

        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        valueLabel.setForeground(TEXT_DARK);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void setTanggalHariIni() {
        Calendar cal = Calendar.getInstance();
        dariTanggal.setValue(cal.getTime());
        sampaiTanggal.setValue(cal.getTime());
    }

    private void setTanggalBulanIni() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        dariTanggal.setValue(cal.getTime());

        cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        sampaiTanggal.setValue(cal.getTime());
    }

    private void applyFilter() {
        try {
            String jenisFilter = (String) comboFilter.getSelectedItem();
            String selectedStatus = (String) comboStatus.getSelectedItem();
            String selectedPayment = (String) comboPayment.getSelectedItem();

            String status = selectedStatus.equals("Semua Status") ? null : selectedStatus;
            String payment = selectedPayment.equals("Semua Metode") ? null : selectedPayment;

            java.util.Date dariDate = (java.util.Date) dariTanggal.getValue();
            java.util.Date sampaiDate = (java.util.Date) sampaiTanggal.getValue();

            Calendar c = Calendar.getInstance();
            c.setTime(sampaiDate);
            c.add(Calendar.DAY_OF_MONTH, 1);

            java.sql.Date sqlDari = new java.sql.Date(dariDate.getTime());
            java.sql.Date sqlSampai = new java.sql.Date(c.getTimeInMillis());

            if ("Semua".equals(jenisFilter)) {
                sqlDari = null;
                sqlSampai = null;
            }

            loadData(sqlDari, sqlSampai, status, payment);
            updateStatistics(sqlDari, sqlSampai, status, payment);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Format tanggal tidak valid!\n" + ex.getMessage());
        }
    }

    private JButton createButton(String text, Color bgColor, String iconName) {
        JButton btn = new JButton(text);
        // Try to load icon
        if (iconName != null) {
            try {
                // Load icon from src/img or img
                ImageIcon icon = null;
                java.net.URL url = getClass().getResource("/img/" + iconName);
                if (url != null) {
                    icon = new ImageIcon(url);
                } else {
                    icon = new ImageIcon("src/img/" + iconName);
                }

                if (icon != null) {
                    Image img = icon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                    btn.setIcon(new ImageIcon(img));
                }
            } catch (Exception e) {
            }
        }

        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setPreferredSize(new Dimension(140, 40));
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);

        // Custom paint with rounded corners (Flat Design - No 3D)
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                // Flat background - no shadow, no gradient
                Color backgroundColor = btn.getModel().isRollover() ? bgColor.brighter() : bgColor;
                g2.setColor(backgroundColor);
                g2.fillRect(0, 0, w, h);

                g2.dispose();
                super.paint(g, c);
            }
        });
        return btn;
    }

    private void loadData(java.sql.Date dari, java.sql.Date sampai, String status, String paymentMethod) {
        model.setRowCount(0);

        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/distrozone_db", "root", "");

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ")
                    .append("t.kode_transaksi, t.tanggal, t.waktu, t.total, t.payment_method, t.status, t.nama_pelanggan, ") // Added
                    // payment_method
                    .append("SUM(d.laba) as total_laba ")
                    .append("FROM TRANSAKSI t ")
                    .append("JOIN DETAIL_TRANSAKSI d ON t.id = d.transaksi_id ")
                    .append("WHERE 1=1 ") // Remove kasir_id restriction to show all (or handle per role if needed, but
                                          // user said "semua")
                    // .append("AND t.kasir_id = ? ") // Commented out to show ALL transactions as
                    // requested "admin riwayat dan lainnya"
                    // Remove platform restriction to show web orders
                    .append("AND t.status IN ('selesai', 'verified', 'completed', 'paid', 'sent') ");

            // Filter berdasarkan tanggal
            if (dari != null && sampai != null) {
                sql.append("AND t.tanggal BETWEEN ? AND ? ");
            }

            // Filter berdasarkan status
            if (status != null && !status.isEmpty()) {
                if (status.equalsIgnoreCase("selesai")) {
                    sql.append("AND t.status IN ('selesai', 'completed') ");
                } else {
                    sql.append("AND t.status = ? ");
                }
            }

            // Filter based on payment method
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                sql.append("AND t.payment_method = ? ");
            }

            sql.append("GROUP BY t.id, t.kode_transaksi, t.tanggal, t.waktu, t.total, t.payment_method, t.status ")
                    .append("ORDER BY t.tanggal DESC, t.waktu DESC");

            PreparedStatement pst = conn.prepareStatement(sql.toString());
            int index = 1;
            // pst.setInt(index++, kasirId); // Removed

            if (dari != null && sampai != null) {
                pst.setDate(index++, dari);
                pst.setDate(index++, sampai);
            }

            if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("selesai")) {
                pst.setString(index++, status);
            }

            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                pst.setString(index++, paymentMethod);
            }

            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                String statusTxt = rs.getString("status");
                String statusDisplay;
                switch (statusTxt) {
                    case "selesai":
                    case "completed":
                        statusDisplay = "Selesai";
                        break;
                    case "pending":
                        statusDisplay = "Pending";
                        break;
                    case "dibatalkan":
                        statusDisplay = "Dibatalkan";
                        break;
                    default:
                        statusDisplay = statusTxt;
                }

                String namaPelanggan = rs.getString("nama_pelanggan");
                if (namaPelanggan == null || namaPelanggan.isEmpty())
                    namaPelanggan = "-";

                // Laba tidak ditampilkan untuk kasir
                model.addRow(new Object[] {
                        rs.getString("kode_transaksi"),
                        rs.getDate("tanggal"),
                        rs.getTime("waktu"),
                        rs.getString("payment_method"),
                        namaPelanggan,
                        "Rp " + df.format(rs.getDouble("total")),
                        statusDisplay,
                        "Lihat"
                });
            }

            rs.close();
            pst.close();
            conn.close();

            System.out.println("✅ Data loaded: " + model.getRowCount() + " transaksi");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat data laporan!\n" + e.getMessage());
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateStatistics(java.sql.Date dari, java.sql.Date sampai, String status, String paymentMethod) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/distrozone_db", "root", "");

            // 1. Hitung Total Transaksi & Omset (Dari tabel TRANSAKSI saja)
            StringBuilder sqlTrans = new StringBuilder();
            sqlTrans.append("SELECT COUNT(id) AS total_transaksi, SUM(total) AS total_omset, AVG(total) AS rata_rata ")
                    .append("FROM TRANSAKSI WHERE 1=1 ") // Remove kasir_id
                    .append("AND status IN ('selesai', 'verified', 'completed', 'paid', 'sent') ");
            // Or should it respect the filter? The UI has a filter for status.
            // If user filters by "pending", omitted? The original code had "AND t.status =
            // 'selesai'" HARDCODED regardless of filter!
            // Wait, checking original code: "WHERE t.kasir_id = ? AND t.status = 'selesai'
            // "
            // But then it adds "AND t.status = ?" if filter is set. This might result in
            // "status='selesai' AND status='pending'" -> Empty.
            // I should respect the filter if set, otherwise default to 'selesai' or 'all'?
            // Original intention concerning 'selesai' hardcode seems to be for
            // "Earnings/Omset" which usually implies completed.
            // However, if I select "Pending" in filter, stats showing 0 is correct.

            // Let's stick to: If filter is set, use it. If not, maybe show all or just
            // selesai?
            // The original code had `... status='selesai'` AND dynamic status.
            // If user selects "Semua Status", query becomes `status='selesai'`.
            // If user selects "Pending", query becomes `status='selesai' AND
            // status='pending'` -> 0.
            // This implies the stats ONLY show completed transactions, which makes sense
            // for "Omset".
            // So I will calculate stats ONLY for completed transactions, unless user
            // specifically filters?
            // Actually, if I filter for "Pending", showing Omset of Pending transactions
            // might be useful?
            // But strict "Laba/Omset" usually means realized money.
            // Let's follow the code: It enforced 'selesai'. So I will keep enforcing
            // 'selesai' for the stats,
            // OR checks if the original code allowed changing it.
            // Original: "WHERE t.kasir_id = ? AND t.status = 'selesai' "
            // Then it appended "AND t.status = ?" if status != null.
            // So if I filter "pending", I get 0. This is desired behavior for "Financial
            // Report".

            if (dari != null && sampai != null) {
                sqlTrans.append("AND tanggal BETWEEN ? AND ? ");
            }
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                sqlTrans.append("AND payment_method = ? ");
            }
            // Note: We ignore status filter here because we enforce 'selesai' for valid
            // stats,
            // OR we should allow filter to override?
            // If I selected "Pending", I expect to see stats for pending?
            // But stats include 'Laba', which isn't realized.
            // Let's keep the logic: Stats = Completed Only.

            PreparedStatement pstTrans = conn.prepareStatement(sqlTrans.toString());
            int idx = 1;
            // pstTrans.setInt(idx++, kasirId); // Removed
            if (dari != null && sampai != null) {
                pstTrans.setDate(idx++, dari);
                pstTrans.setDate(idx++, sampai);
            }
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                pstTrans.setString(idx++, paymentMethod);
            }

            ResultSet rsTrans = pstTrans.executeQuery();
            int totalTransaksi = 0;
            double totalOmset = 0;
            double rataRata = 0;
            if (rsTrans.next()) {
                totalTransaksi = rsTrans.getInt("total_transaksi");
                totalOmset = rsTrans.getDouble("total_omset");
                rataRata = rsTrans.getDouble("rata_rata");
            }
            rsTrans.close();
            pstTrans.close();

            // 2. Hitung Total Laba (Butuh Join Detail)
            StringBuilder sqlLaba = new StringBuilder();
            sqlLaba.append("SELECT SUM(d.laba) AS total_laba ")
                    .append("FROM TRANSAKSI t ")
                    .append("JOIN DETAIL_TRANSAKSI d ON t.id = d.transaksi_id ")
                    .append("WHERE 1=1 ") // Remove kasir_id
                    .append("AND t.status IN ('selesai', 'verified', 'completed', 'paid', 'sent') ");

            if (dari != null && sampai != null) {
                sqlLaba.append("AND t.tanggal BETWEEN ? AND ? ");
            }
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                sqlLaba.append("AND t.payment_method = ? ");
            }

            PreparedStatement pstLaba = conn.prepareStatement(sqlLaba.toString());
            idx = 1;
            // pstLaba.setInt(idx++, kasirId); // Removed
            if (dari != null && sampai != null) {
                pstLaba.setDate(idx++, dari);
                pstLaba.setDate(idx++, sampai);
            }
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                pstLaba.setString(idx++, paymentMethod);
            }

            ResultSet rsLaba = pstLaba.executeQuery();
            double totalLaba = 0;
            if (rsLaba.next()) {
                totalLaba = rsLaba.getDouble("total_laba");
            }
            rsLaba.close();
            pstLaba.close();

            lblTotalTransaksi.setText(String.valueOf(totalTransaksi));
            lblTotalOmset.setText("Rp " + df.format(totalOmset));
            lblRataTransaksi.setText("Rp " + df.format(rataRata));
            lblLaba.setText("Rp " + df.format(totalLaba));

            // 3. Hitung Per Payment Method
            StringBuilder sqlMethod = new StringBuilder();
            sqlMethod.append("SELECT payment_method, SUM(total) as method_total ")
                    .append("FROM TRANSAKSI WHERE 1=1 ")
                    .append("AND status IN ('selesai', 'verified', 'completed', 'paid', 'sent') ");

            if (dari != null && sampai != null) {
                sqlMethod.append("AND tanggal BETWEEN ? AND ? ");
            }
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                sqlMethod.append("AND payment_method = ? ");
            }

            sqlMethod.append("GROUP BY payment_method");

            PreparedStatement pstMethod = conn.prepareStatement(sqlMethod.toString());
            int idxMethod = 1;
            if (dari != null && sampai != null) {
                pstMethod.setDate(idxMethod++, dari);
                pstMethod.setDate(idxMethod++, sampai);
            }
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                pstMethod.setString(idxMethod++, paymentMethod);
            }

            ResultSet rsMethod = pstMethod.executeQuery();
            double tTunai = 0, tQRIS = 0, tTransfer = 0;

            while (rsMethod.next()) {
                String pm = rsMethod.getString("payment_method");
                double val = rsMethod.getDouble("method_total");
                if (pm == null)
                    pm = "Tunai"; // Default

                if (pm.equalsIgnoreCase("Tunai") || pm.equalsIgnoreCase("Cash")) {
                    tTunai += val;
                } else if (pm.toLowerCase().contains("qris")) {
                    tQRIS += val;
                } else if (pm.toLowerCase().contains("bca") || pm.toLowerCase().contains("transfer")) {
                    tTransfer += val;
                } else {
                    tTunai += val;
                }
            }
            rsMethod.close();
            pstMethod.close();

            lblTotalTunai.setText("Rp " + df.format(tTunai));
            lblTotalQRIS.setText("Rp " + df.format(tQRIS));
            lblTotalTransfer.setText("Rp " + df.format(tTransfer));

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void showDetailTransaksi(int row) {
        String kodeTransaksi = (String) table.getValueAt(row, 0);

        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/distrozone_db", "root", "");

            String sql = "SELECT t.kode_transaksi, t.tanggal, t.waktu, t.total, t.status, t.payment_method, t.platform, "
                    +
                    "km.nama_kaos, kv.size, d.qty, d.harga_jual, d.subtotal, d.laba " +
                    "FROM TRANSAKSI t " +
                    "JOIN DETAIL_TRANSAKSI d ON t.id = d.transaksi_id " +
                    "JOIN KAOS_VARIAN kv ON d.kaos_id = kv.id " +
                    "JOIN KAOS_MASTER km ON kv.kaos_master_id = km.id " +
                    "WHERE t.kode_transaksi = ?"; // Removed kasir_id check

            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, kodeTransaksi);
            // pst.setInt(2, kasirId); // Removed
            ResultSet rs = pst.executeQuery();

            StringBuilder detail = new StringBuilder();
            detail.append("📋 DETAIL TRANSAKSI\n");
            detail.append("═══════════════════════\n\n");

            boolean firstRow = true;
            double totalTransaksi = 0;
            double totalLaba = 0;
            int itemCount = 0;
            String platform = "desktop"; // Default

            while (rs.next()) {
                if (firstRow) {
                    platform = rs.getString("platform");
                    if (platform == null)
                        platform = "desktop";

                    detail.append("Kode Transaksi: ").append(rs.getString("kode_transaksi")).append("\n");
                    detail.append("Tanggal      : ").append(rs.getDate("tanggal")).append("\n");
                    detail.append("Waktu        : ").append(rs.getTime("waktu")).append("\n");
                    detail.append("Metode Bayar : ").append(rs.getString("payment_method")).append("\n");
                    detail.append("Status       : ").append(rs.getString("status")).append("\n");
                    detail.append("Platform     : ").append(platform.toUpperCase()).append("\n");
                    detail.append("═══════════════════════\n");
                    detail.append("DETAIL PRODUK:\n");
                    detail.append("═══════════════════════\n");
                    detail.append(String.format("%-25s %5s %10s %12s %10s\n",
                            "Nama Kaos", "Qty", "Harga", "Subtotal", "Laba"));
                    detail.append("══════════════════════════════════════════════════════════\n");
                    firstRow = false;
                }

                String namaKaos = rs.getString("nama_kaos") + " (" + rs.getString("size") + ")";
                int qty = rs.getInt("qty");
                double harga = rs.getDouble("harga_jual");
                double subtotal = rs.getDouble("subtotal");
                double laba = rs.getDouble("laba");

                detail.append(String.format("%-25s %5d %10s %12s %10s\n",
                        namaKaos.length() > 25 ? namaKaos.substring(0, 22) + "..." : namaKaos,
                        qty,
                        "Rp " + df.format(harga),
                        "Rp " + df.format(subtotal),
                        "Rp " + df.format(laba)));

                itemCount++;
                totalTransaksi = rs.getDouble("total");
                totalLaba += laba;
            }

            detail.append("══════════════════════════════════════════════════════════\n");
            detail.append(String.format("Total Item  : %d\n", itemCount));
            detail.append(String.format("Total Transaksi : Rp %s\n", df.format(totalTransaksi)));
            detail.append(String.format("Total Laba      : Rp %s\n", df.format(totalLaba)));
            detail.append("═══════════════════════\n");

            if ("web".equalsIgnoreCase(platform)) {
                detail.append("💡 Transaksi ini dilakukan via **WEB ONLINE**");
            } else {
                detail.append("💡 Transaksi ini dilakukan via **DESKTOP APP**\n");
                detail.append("   Oleh Kasir: ").append(kasirName);
            }

            JTextArea textArea = new JTextArea(detail.toString());
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(600, 350));

            JOptionPane.showMessageDialog(this, scrollPane, "Detail Transaksi: " + kodeTransaksi,
                    JOptionPane.INFORMATION_MESSAGE);

            rs.close();
            pst.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat detail transaksi!");
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void exportToExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Simpan Laporan (HTML/PDF)");
        fileChooser.setSelectedFile(new java.io.File("laporan_kasir_" + kasirName + ".html"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("HTML Files", "html"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".html")) {
                file = new java.io.File(file.getAbsolutePath() + ".html");
            }

            try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                // Build HTML Report
                StringBuilder html = new StringBuilder();
                html.append("<html><head><meta charset='UTF-8'>");
                html.append("<title>Laporan Kasir - ").append(kasirName).append("</title>");
                html.append("<style>");
                html.append("body { font-family: 'Segoe UI', sans-serif; margin: 20px; color: #333; }");
                html.append("h1 { color: #10B981; text-align: center; }");
                html.append("h2 { color: #0F766E; }");
                html.append(".stats { display: flex; gap: 20px; margin-bottom: 20px; }");
                html.append(
                        ".stat-card { background: #ECFDF5; padding: 15px; border-radius: 8px; border-left: 4px solid #10B981; }");
                html.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }");
                html.append("th { background: #10B981; color: white; padding: 10px; text-align: left; }");
                html.append("td { padding: 8px; border-bottom: 1px solid #ddd; }");
                html.append("tr:hover { background: #f5f5f5; }");
                html.append(".footer { text-align: center; margin-top: 30px; color: #888; font-size: 12px; }");
                html.append("</style></head><body>");

                // Header
                html.append("<h1>📊 LAPORAN TRANSAKSI KASIR</h1>");
                html.append("<h2 style='text-align:center;'>").append(kasirName).append(" (").append(userCode)
                        .append(")</h2>");
                html.append("<p style='text-align:center;'>Tanggal: ")
                        .append(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date()))
                        .append("</p>");

                // Statistics
                html.append("<div class='stats'>");
                html.append("<div class='stat-card'><strong>Total Transaksi</strong><br>")
                        .append(lblTotalTransaksi.getText()).append("</div>");
                html.append("<div class='stat-card'><strong>Total Omset</strong><br>").append(lblTotalOmset.getText())
                        .append("</div>");
                html.append("<div class='stat-card'><strong>Total Laba</strong><br>").append(lblLaba.getText())
                        .append("</div>");
                html.append("</div>");

                // Table
                html.append("<table>");
                html.append(
                        "<tr><th>Kode</th><th>Tanggal</th><th>Waktu</th><th>Total</th><th>Laba</th><th>Status</th></tr>");

                for (int i = 0; i < model.getRowCount(); i++) {
                    html.append("<tr>");
                    html.append("<td>").append(model.getValueAt(i, 0)).append("</td>");
                    html.append("<td>").append(model.getValueAt(i, 1)).append("</td>");
                    html.append("<td>").append(model.getValueAt(i, 2)).append("</td>");
                    html.append("<td>").append(model.getValueAt(i, 3)).append("</td>");
                    html.append("<td>").append(model.getValueAt(i, 4)).append("</td>");
                    String status = model.getValueAt(i, 5).toString().replace("✅ ", "").replace("⏳ ", "").replace("❌ ",
                            "");
                    html.append("<td>").append(status).append("</td>");
                    html.append("</tr>");
                }
                html.append("</table>");

                // Footer
                html.append("<div class='footer'>");
                html.append("<p>Distro Zone - Laporan Kasir</p>");
                html.append(
                        "<p><strong>Tip:</strong> Buka file ini di browser, lalu tekan Ctrl+P untuk menyimpan sebagai PDF</p>");
                html.append("</div>");

                html.append("</body></html>");

                writer.println(html.toString());

                JOptionPane.showMessageDialog(this,
                        "✅ Laporan berhasil diexport!\n\nFile: " + file.getAbsolutePath() +
                                "\n\n💡 Tip: Buka file di browser, lalu tekan Ctrl+P untuk menyimpan sebagai PDF",
                        "Export Berhasil",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "❌ Gagal mengexport laporan!\n" + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void printLaporan() {
        StringBuilder printContent = new StringBuilder();
        printContent.append("========================================\n");
        printContent.append("          LAPORAN KASIR\n");
        printContent.append("          ").append(kasirName).append("\n");
        printContent.append("          (").append(userCode).append(")\n");
        printContent.append("========================================\n");
        printContent.append("Tanggal: ")
                .append(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date())).append("\n");
        printContent.append("----------------------------------------\n");

        // Add statistics
        printContent.append("STATISTIK:\n");
        printContent.append("  Total Transaksi : ").append(lblTotalTransaksi.getText()).append("\n");
        printContent.append("  Total Omset     : ").append(lblTotalOmset.getText()).append("\n");
        printContent.append("  Total Laba      : ").append(lblLaba.getText()).append("\n");
        printContent.append("----------------------------------------\n");

        // Add table headers
        printContent.append(String.format("%-15s %-10s %-8s %-12s %-12s %-10s\n",
                "Kode", "Tanggal", "Waktu", "Total", "Laba", "Status"));
        printContent.append("----------------------------------------\n");

        // Add table data
        for (int i = 0; i < model.getRowCount(); i++) {
            String kode = model.getValueAt(i, 0).toString();
            String tanggal = model.getValueAt(i, 1).toString();
            String waktu = model.getValueAt(i, 2).toString();
            String total = model.getValueAt(i, 3).toString();
            String laba = model.getValueAt(i, 4).toString();
            String status = model.getValueAt(i, 5).toString().replace("✅ ", "").replace("⏳ ", "").replace("❌ ", "");

            printContent.append(String.format("%-15s %-10s %-8s %-12s %-12s %-10s\n",
                    kode, tanggal, waktu, total, laba, status));
        }

        printContent.append("========================================\n");
        printContent.append("Distro Zone - Laporan Kasir\n");
        printContent.append("========================================\n");

        JTextArea printArea = new JTextArea(printContent.toString());
        printArea.setFont(new Font("Monospaced", Font.PLAIN, 11));

        try {
            printArea.print();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Gagal mencetak laporan!\n" + e.getMessage(),
                    "Print Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Untuk kompatibilitas jika ada yang memanggil main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new LaporanKasir(1, "Kasir Utama", "KSR001").setVisible(true);
        });
    }

    // ================= METHOD UNTUK LOAD IMAGE ICON =================
    private ImageIcon loadImageIcon(String filename, int size) {
        try {
            // Option 1: Coba dari resources
            java.net.URL imageUrl = getClass().getResource("/img/" + filename);
            if (imageUrl != null) {
                ImageIcon icon = new ImageIcon(imageUrl);
                Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }

            // Option 2: Coba load dari src/img
            java.io.File file = new java.io.File("src/img/" + filename);
            if (file.exists()) {
                ImageIcon icon = new ImageIcon(file.getPath());
                Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }

            // Option 3: Coba load dari folder img root
            file = new java.io.File("img/" + filename);
            if (file.exists()) {
                ImageIcon icon = new ImageIcon(file.getPath());
                Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }

            return null;
        } catch (Exception e) {
            System.out.println("Error load " + filename + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}