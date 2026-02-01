import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.awt.image.BufferedImage;

public class LaporanKeuangan extends JFrame {
    private int userId;
    private String adminName;
    private String userCode;
    private JTable table;
    private DefaultTableModel model;
    private JSpinner dariTanggal, sampaiTanggal;
    private JComboBox<String> comboFilter, comboKasir, comboKategori, comboStatus, comboPlatform, comboPayment;
    private DecimalFormat df;
    private JLabel lblTotalOmset, lblTotalTransaksi, lblTotalModal, lblTotalLaba;
    private JLabel lblTotalTunai, lblTotalQRIS, lblTotalBCA;
    private JPanel statsPanel;

    // WARNA THEME DISTRO ZONE (Emerald Green - Sesuai Web)
    private static final Color GREEN_PRIMARY = new Color(16, 185, 129); // #10B981
    private static final Color GREEN_DARK = new Color(15, 118, 110); // #0F766E
    private static final Color GREEN_LIGHT = new Color(52, 211, 153); // #34D399
    private static final Color TEXT_DARK = new Color(31, 41, 55); // #1F2937
    private static final Color TEXT_GRAY = new Color(100, 116, 139); // #64748B
    private static final Color BACKGROUND_LIGHT = new Color(236, 253, 245); // #ECFDF5
    private static final Color BORDER_COLOR = new Color(167, 243, 208); // #A7F3D0

    // ================= KONSTRUKTOR BARU =================
    public LaporanKeuangan(int userId, String adminName, String userCode) {
        this.userId = userId;
        this.adminName = adminName;
        this.userCode = userCode;

        initUI();
        initData();
    }

    // ================= KONSTRUKTOR LAMA (untuk kompatibilitas) =================
    public LaporanKeuangan(String usernameAdmin) {
        this(0, usernameAdmin, "ADM001"); // Default values untuk kompatibilitas
    }

    private void initUI() {
        // Format angka ribuan pakai titik
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        df = new DecimalFormat("#,###", symbols);

        setTitle("📊 Laporan Keuangan - ");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

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

        // ================= HEADER DENGAN ICON =================
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        // Load icon report.png
        ImageIcon reportIcon = loadReportIcon();

        JLabel lblHeader = new JLabel("Laporan Keuangan", reportIcon, JLabel.CENTER);
        lblHeader.setHorizontalTextPosition(SwingConstants.RIGHT);
        lblHeader.setIconTextGap(15);
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblHeader.setForeground(Color.WHITE);
        lblHeader.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        headerPanel.add(lblHeader, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ================= STATISTIK PANEL DENGAN 2 BARIS =================
        statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Baris 1: Statistik Umum (Omset, Transaksi, Modal, Laba)
        JPanel statsRow1 = new JPanel(new GridLayout(1, 4, 15, 0));
        statsRow1.setOpaque(false);

        lblTotalOmset = createStatCard("Total Omset", "Rp 0", GREEN_PRIMARY);
        lblTotalTransaksi = createStatCard("Total Transaksi", "0", new Color(33, 150, 243)); // Blue
        lblTotalModal = createStatCard("Total Modal", "Rp 0", new Color(255, 152, 0)); // Orange
        lblTotalLaba = createStatCard("Total Laba", "Rp 0", new Color(156, 39, 176)); // Purple

        statsRow1.add(lblTotalOmset);
        statsRow1.add(lblTotalTransaksi);
        statsRow1.add(lblTotalModal);
        statsRow1.add(lblTotalLaba);

        // Baris 2: Statistik per Metode Pembayaran
        JPanel statsRow2 = new JPanel(new GridLayout(1, 3, 15, 0));
        statsRow2.setOpaque(false);
        statsRow2.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        lblTotalTunai = createStatCard("Total Tunai", "Rp 0", new Color(46, 125, 50)); // Dark Green
        lblTotalQRIS = createStatCard("Total QRIS", "Rp 0", new Color(0, 150, 136)); // Teal
        lblTotalBCA = createStatCard("Total Transfer BCA", "Rp 0", new Color(25, 118, 210)); // Blue

        statsRow2.add(lblTotalTunai);
        statsRow2.add(lblTotalQRIS);
        statsRow2.add(lblTotalBCA);

        statsPanel.add(statsRow1);
        statsPanel.add(statsRow2);

        // ================= CONTENT BOX =================
        JPanel contentBox = new JPanel(new BorderLayout(10, 10));
        contentBox.setBackground(new Color(255, 255, 255, 245));
        contentBox.setBorder(BorderFactory.createLineBorder(GREEN_PRIMARY, 2));

        // Wrapper untuk statsPanel dan contentBox
        JPanel centerWrapper = new JPanel(new BorderLayout(10, 10));
        centerWrapper.setOpaque(false);
        centerWrapper.add(statsPanel, BorderLayout.NORTH);
        centerWrapper.add(contentBox, BorderLayout.CENTER);

        mainPanel.add(centerWrapper, BorderLayout.CENTER);

        // ================= FILTER PANEL =================
        JPanel filterPanel = new JPanel(new BorderLayout(10, 5));
        filterPanel.setOpaque(false);
        filterPanel.setBackground(new Color(255, 255, 255, 200));

        // Baris 1: Filter controls
        JPanel filterBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        filterBox.setOpaque(false);

        // ComboBox untuk jenis filter
        comboFilter = new JComboBox<>(new String[] { "Semua", "Harian", "Bulanan", "Rentang Tanggal" });
        comboFilter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboFilter.setBackground(Color.WHITE);

        // ComboBox untuk kasir
        comboKasir = new JComboBox<>(new String[] { "Semua Kasir" });
        comboKasir.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboKasir.setBackground(Color.WHITE);

        // ComboBox untuk kategori
        comboKategori = new JComboBox<>(new String[] { "Semua Kategori" });
        comboKategori.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboKategori.setBackground(Color.WHITE);

        // ComboBox untuk status transaksi
        comboStatus = new JComboBox<>(new String[] { "Semua Status", "selesai", "pending", "dibatalkan" });
        comboStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboStatus.setBackground(Color.WHITE);

        // ComboBox untuk platform (Desktop/Web)
        comboPlatform = new JComboBox<>(new String[] { "Semua Platform", "Desktop", "Web" });
        comboPlatform.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboPlatform.setBackground(Color.WHITE);

        // ComboBox untuk metode pembayaran
        comboPayment = new JComboBox<>(new String[] { "Semua Metode", "Tunai", "QRIS", "Transfer BCA" });
        comboPayment.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboPayment.setBackground(Color.WHITE);

        dariTanggal = new JSpinner(new SpinnerDateModel());
        dariTanggal.setEditor(new JSpinner.DateEditor(dariTanggal, "dd-MM-yyyy"));
        dariTanggal.setPreferredSize(new Dimension(110, 28));

        sampaiTanggal = new JSpinner(new SpinnerDateModel());
        sampaiTanggal.setEditor(new JSpinner.DateEditor(sampaiTanggal, "dd-MM-yyyy"));
        sampaiTanggal.setPreferredSize(new Dimension(110, 28));

        JButton btnFilter = createIconButton("Filter", "filter.png", GREEN_PRIMARY);

        filterBox.add(new JLabel("Filter:"));
        filterBox.add(comboFilter);
        filterBox.add(new JLabel("Kasir:"));
        filterBox.add(comboKasir);
        filterBox.add(new JLabel("Kategori:"));
        filterBox.add(comboKategori);
        filterBox.add(new JLabel("Status:"));
        filterBox.add(comboStatus);
        filterBox.add(new JLabel("Platform:"));
        filterBox.add(comboPlatform);
        filterBox.add(new JLabel("Metode:"));
        filterBox.add(comboPayment);
        filterBox.add(new JLabel("Dari:"));
        filterBox.add(dariTanggal);
        filterBox.add(new JLabel("Sampai:"));
        filterBox.add(sampaiTanggal);
        filterBox.add(btnFilter);

        // Baris 2: Action buttons
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        actionBox.setOpaque(false);

        JButton btnReset = createIconButton("Reset", "reset.png", new Color(255, 152, 0)); // Orange
        JButton btnExport = createIconButton("Export", "export.png", new Color(156, 39, 176)); // Purple
        JButton btnPrint = createIconButton("Cetak", "report.png", new Color(52, 152, 219)); // Blue

        actionBox.add(new JLabel("Aksi:"));
        actionBox.add(btnReset);
        actionBox.add(btnExport);
        actionBox.add(btnPrint);

        // Combine both rows
        JPanel filterWrapper = new JPanel(new GridLayout(2, 1, 0, 2));
        filterWrapper.setOpaque(false);
        filterWrapper.add(filterBox);
        filterWrapper.add(actionBox);

        filterPanel.add(filterWrapper, BorderLayout.CENTER);
        contentBox.add(filterPanel, BorderLayout.NORTH);

        // ================= TABEL DENGAN KOLOM BARU =================
        model = new DefaultTableModel(
                new String[] {
                        "Kode Transaksi", "Tanggal", "Waktu", "Kasir", "Pelanggan", "Platform",
                        "Metode", "Total", "Modal", "Laba", "Status", "Detail"
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
            // Kolom: 0-Kode, 1-Tanggal, 2-Waktu, 3-Kasir, 4-Pelanggan, 5-Platform,
            // 6-Metode, 7-Total, 8-Modal, 9-Laba, 10-Status, 11-Detail
            if (i == 7 || i == 8 || i == 9) { // Kolom Total, Modal, Laba (right align)
                table.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
            } else if (i == 11) { // Kolom Detail (button)
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

        JButton btnBack = createIconButton("Kembali", "back.png", TEXT_GRAY);
        btnPanel.add(btnBack);

        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        // ================= EVENT HANDLERS =================
        btnBack.addActionListener(e -> {
            dispose();
            new AdminDashboard(userId, adminName, userCode).setVisible(true);
        });

        btnExport.addActionListener(e -> exportToExcel());

        btnFilter.addActionListener(e -> applyFilter());

        btnReset.addActionListener(e -> {
            comboFilter.setSelectedIndex(0);
            comboKasir.setSelectedIndex(0);
            comboKategori.setSelectedIndex(0);
            comboStatus.setSelectedIndex(0);
            comboPlatform.setSelectedIndex(0);
            comboPayment.setSelectedIndex(0);
            setTanggalHariIni();
            loadData(null, null, null, null, null, null, null, null);
            updateStatistics(null, null, null, null, null, null, null);
        });

        btnPrint.addActionListener(e -> printLaporan());

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
                if (row >= 0 && col == 10) { // Kolom Detail (index 10)
                    showDetailTransaksi(row);
                }
            }
        });
    }

    // ================= METHOD UNTUK LOAD ICON REPORT.PNG =================
    private ImageIcon loadReportIcon() {
        try {
            // Coba load dari file report.png
            ImageIcon originalIcon = new ImageIcon("report.png");

            // Jika file tidak ditemukan, coba dari resources
            if (originalIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                originalIcon = new ImageIcon(getClass().getResource("/img/report.png"));
            }

            // Jika masih tidak ditemukan, buat icon default
            if (originalIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                return createDefaultReportIcon();
            }

            // Scale icon ke ukuran 40x40
            Image img = originalIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            return new ImageIcon(img);

        } catch (Exception e) {
            System.out.println("Gagal load report.png, menggunakan icon default");
            return createDefaultReportIcon();
        }
    }

    // ================= METHOD UNTUK BUAT ICON DEFAULT LAPORAN =================
    private ImageIcon createDefaultReportIcon() {
        // Buat gambar default 40x40
        ImageIcon icon = new ImageIcon(createDefaultReportImage());
        return icon;
    }

    private Image createDefaultReportImage() {
        int width = 40;
        int height = 40;

        // Create a buffered image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background circle
        g2d.setColor(new Color(33, 150, 243)); // Blue color for report icon
        g2d.fillOval(0, 0, width, height);

        // Draw report/document icon
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2f));

        // Draw document shape
        int[] xPoints = { 8, 32, 32, 8 };
        int[] yPoints = { 10, 10, 30, 30 };
        g2d.drawPolygon(xPoints, yPoints, 4);

        // Draw folded corner
        g2d.drawLine(24, 10, 32, 10);
        g2d.drawLine(32, 10, 32, 18);

        // Draw lines on document (simulating text)
        g2d.drawLine(12, 15, 28, 15);
        g2d.drawLine(12, 18, 20, 18);
        g2d.drawLine(12, 21, 28, 21);
        g2d.drawLine(12, 24, 24, 24);

        // Draw chart/graph element
        g2d.drawLine(15, 27, 18, 25);
        g2d.drawLine(18, 25, 21, 28);
        g2d.drawLine(21, 28, 24, 26);
        g2d.drawLine(24, 26, 27, 29);

        g2d.dispose();
        return image;
    }

    // ================= METHOD UNTUK BUAT TOMBOL DENGAN ICON =================
    private JButton createIconButton(String text, String iconName, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(140, 40));

        // Coba load icon
        try {
            ImageIcon originalIcon = new ImageIcon(iconName);

            // Jika file tidak ditemukan, coba dari resources
            if (originalIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                originalIcon = new ImageIcon(getClass().getResource("/img/" + iconName));
            }

            // Jika masih tidak ditemukan, buat icon default berdasarkan nama file
            if (originalIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                originalIcon = createDefaultIcon(iconName);
            }

            // Scale icon ke ukuran 20x20
            Image img = originalIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(img));
            btn.setHorizontalTextPosition(SwingConstants.RIGHT);
            btn.setIconTextGap(8);

        } catch (Exception e) {
            System.out.println("Gagal load icon " + iconName + ", menggunakan tombol tanpa icon");
        }

        // Custom paint for consistent styling (prevents white "glitch")
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (btn.getModel().isRollover()) {
                    g2.setColor(bgColor.darker());
                } else {
                    g2.setColor(bgColor);
                }

                // KOTAK (Square)
                g2.fillRect(0, 0, c.getWidth(), c.getHeight());
                g2.dispose();

                super.paint(g, c);
            }
        });

        return btn;
    }

    // ================= METHOD UNTUK BUAT ICON DEFAULT BERDASARKAN NAMA FILE
    // =================
    private ImageIcon createDefaultIcon(String iconName) {
        int width = 20;
        int height = 20;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Warna background icon
        Color iconColor = Color.WHITE;

        if (iconName.contains("filter")) {
            // Icon filter (garis gelombang)
            g2d.setColor(iconColor);
            g2d.setStroke(new BasicStroke(2f));

            // Draw filter symbol (wave lines)
            int[] xPoints = { 3, 6, 9, 12, 15, 18 };
            int[] yPoints = { 10, 6, 14, 8, 12, 10 };

            for (int i = 0; i < xPoints.length - 1; i++) {
                g2d.drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
            }

        } else if (iconName.contains("reset")) {
            // Icon reset (panah melingkar)
            g2d.setColor(iconColor);
            g2d.setStroke(new BasicStroke(2f));

            // Draw circle
            g2d.drawOval(4, 4, 12, 12);

            // Draw arrow
            g2d.drawLine(16, 6, 12, 10); // Arrow head
            g2d.drawLine(16, 6, 12, 2); // Arrow head
            g2d.drawLine(10, 12, 16, 6); // Arrow line

        } else if (iconName.contains("export")) {
            // Icon export (panah keluar)
            g2d.setColor(iconColor);
            g2d.setStroke(new BasicStroke(2f));

            // Draw box
            g2d.drawRect(5, 5, 10, 10);

            // Draw arrow
            g2d.drawLine(15, 5, 20, 0); // Arrow line 1
            g2d.drawLine(15, 5, 20, 5); // Arrow line 2
            g2d.drawLine(15, 5, 20, 10); // Arrow line 3

        } else if (iconName.contains("laba")) {
            // Icon laba (chart naik)
            g2d.setColor(iconColor);
            g2d.setStroke(new BasicStroke(2f));

            // Draw chart lines
            g2d.drawLine(4, 16, 8, 10); // Line 1
            g2d.drawLine(8, 10, 12, 14); // Line 2
            g2d.drawLine(12, 14, 16, 8); // Line 3

            // Draw chart points
            g2d.fillOval(3, 15, 4, 4); // Point 1
            g2d.fillOval(7, 9, 4, 4); // Point 2
            g2d.fillOval(11, 13, 4, 4); // Point 3
            g2d.fillOval(15, 7, 4, 4); // Point 4

        } else if (iconName.contains("back")) {
            // Icon back (panah kiri)
            g2d.setColor(iconColor);
            g2d.setStroke(new BasicStroke(2f));

            // Draw arrow
            g2d.drawLine(12, 10, 6, 10); // Horizontal line
            g2d.drawLine(6, 10, 9, 6); // Arrow head top
            g2d.drawLine(6, 10, 9, 14); // Arrow head bottom
        }

        g2d.dispose();
        return new ImageIcon(image);
    }

    private void initData() {
        loadKasirData();
        loadKategoriData();
        setTanggalHariIni();
        loadData(null, null, null, null, null, null, null, null);
        updateStatistics(null, null, null, null, null, null, null);
    }

    private JLabel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(color);

        JLabel valueLabel = new JLabel(value, JLabel.CENTER);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        valueLabel.setForeground(TEXT_DARK);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        JLabel wrapper = new JLabel() {
            @Override
            public String getText() {
                return valueLabel.getText();
            }

            @Override
            public void setText(String text) {
                valueLabel.setText(text);
            }

            @Override
            public Dimension getPreferredSize() {
                return card.getPreferredSize();
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(100, 60);
            }
        };
        wrapper.setLayout(new BorderLayout());
        wrapper.add(card, BorderLayout.CENTER);
        wrapper.setPreferredSize(new Dimension(180, 70));

        return wrapper;
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
            String selectedKasir = (String) comboKasir.getSelectedItem();
            String selectedKategori = (String) comboKategori.getSelectedItem();
            String selectedStatus = (String) comboStatus.getSelectedItem();

            String kasirUsername = selectedKasir.equals("Semua Kasir") ? null : selectedKasir;
            String kategori = selectedKategori.equals("Semua Kategori") ? null : selectedKategori;
            String status = selectedStatus.equals("Semua Status") ? null : selectedStatus;

            String selectedPlatform = (String) comboPlatform.getSelectedItem();
            String platform = selectedPlatform.equals("Semua Platform") ? null : selectedPlatform.toLowerCase();

            String selectedPayment = (String) comboPayment.getSelectedItem();
            String paymentMethod = selectedPayment.equals("Semua Metode") ? null : selectedPayment;

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

            loadData(sqlDari, sqlSampai, jenisFilter, kasirUsername, kategori, status, platform, paymentMethod);
            updateStatistics(sqlDari, sqlSampai, kasirUsername, kategori, status, platform, paymentMethod);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Format tanggal tidak valid!\n" + ex.getMessage());
        }
    }

    private void loadKasirData() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/distrozone_db", "root", "");

            String sql = "SELECT username, nama FROM users WHERE role = 'kasir' AND status = 'active' ORDER BY username";
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                comboKasir.addItem(rs.getString("nama") + " (" + rs.getString("username") + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat data kasir:\n" + e.getMessage());
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadKategoriData() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/distrozone_db", "root", "");

            String sql = "SELECT nama_kategori FROM kategori ORDER BY nama_kategori";
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                comboKategori.addItem(rs.getString("nama_kategori"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat data kategori:\n" + e.getMessage());
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadData(java.sql.Date dari, java.sql.Date sampai, String jenisFilter,
            String kasirUsername, String kategori, String status, String platform, String paymentMethod) {
        model.setRowCount(0);

        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/distrozone_db", "root", "");

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ")
                    .append("t.kode_transaksi, t.tanggal, t.waktu, t.total, t.status, t.platform, t.nama_pelanggan, t.payment_method, ")
                    .append("u.username AS kasir_username, u.nama AS kasir_nama, ")
                    .append("COALESCE(SUM(d.harga_modal * d.qty), 0) AS total_modal, ")
                    .append("COALESCE(SUM(d.laba), 0) AS total_laba ")
                    .append("FROM TRANSAKSI t ")
                    .append("JOIN users u ON t.kasir_id = u.id ")
                    .append("LEFT JOIN DETAIL_TRANSAKSI d ON t.id = d.transaksi_id ")
                    .append("WHERE 1=1 ");

            // Filter berdasarkan tanggal
            if (dari != null && sampai != null && !"Semua".equals(jenisFilter)) {
                sql.append("AND t.tanggal BETWEEN ? AND DATE_SUB(?, INTERVAL 1 DAY) ");
            }

            // Filter berdasarkan kasir
            if (kasirUsername != null && !kasirUsername.isEmpty()) {
                // Extract username dari string (Format: Nama (username))
                String username = kasirUsername.substring(kasirUsername.indexOf("(") + 1, kasirUsername.indexOf(")"));
                sql.append("AND u.username = ? ");
            }

            // Filter berdasarkan status
            if (status != null && !status.isEmpty()) {
                if (status.equalsIgnoreCase("selesai")) {
                    sql.append("AND t.status IN ('selesai', 'completed') ");
                } else {
                    sql.append("AND t.status = ? ");
                }
            }

            // Filter berdasarkan kategori jika dipilih
            if (kategori != null && !kategori.isEmpty()) {
                sql.append("AND EXISTS (SELECT 1 FROM DETAIL_TRANSAKSI dt ")
                        .append("JOIN KAOS_VARIAN kv ON dt.kaos_id = kv.id ")
                        .append("JOIN KAOS_MASTER km ON kv.kaos_master_id = km.id ")
                        .append("JOIN kategori kat ON km.kategori_id = kat.id ")
                        .append("WHERE dt.transaksi_id = t.id AND kat.nama_kategori = ?) ");
            }

            // Filter berdasarkan platform (Desktop/Web)
            if (platform != null && !platform.isEmpty()) {
                sql.append("AND t.platform = ? ");
            }

            // Filter berdasarkan metode pembayaran
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                sql.append("AND t.payment_method = ? ");
            }

            sql.append(
                    "GROUP BY t.id, t.kode_transaksi, t.tanggal, t.waktu, t.total, t.status, t.platform, t.payment_method, ")
                    .append("u.username, u.nama ")
                    .append("ORDER BY t.tanggal DESC, t.waktu DESC");

            PreparedStatement pst = conn.prepareStatement(sql.toString());
            int paramIndex = 1;

            if (dari != null && sampai != null && !"Semua".equals(jenisFilter)) {
                pst.setDate(paramIndex++, dari);
                pst.setDate(paramIndex++, sampai);
            }

            if (kasirUsername != null && !kasirUsername.isEmpty()) {
                String username = kasirUsername.substring(kasirUsername.indexOf("(") + 1, kasirUsername.indexOf(")"));
                pst.setString(paramIndex++, username);
            }

            if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("selesai")) {
                pst.setString(paramIndex++, status);
            }

            if (kategori != null && !kategori.isEmpty()) {
                pst.setString(paramIndex++, kategori);
            }

            if (platform != null && !platform.isEmpty()) {
                pst.setString(paramIndex++, platform);
            }

            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                pst.setString(paramIndex++, paymentMethod);
            }

            ResultSet rs = pst.executeQuery();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                String kasirDisplay = rs.getString("kasir_nama") + " (" + rs.getString("kasir_username") + ")";
                String tanggalStr = dateFormat.format(rs.getDate("tanggal"));
                String waktuStr = rs.getTime("waktu") != null ? timeFormat.format(rs.getTime("waktu")) : "-";
                double total = rs.getDouble("total");
                double modal = rs.getDouble("total_modal");
                double laba = rs.getDouble("total_laba");
                String platformStr = rs.getString("platform");
                if (platformStr == null || platformStr.isEmpty()) {
                    platformStr = "Desktop";
                } else {
                    platformStr = platformStr.substring(0, 1).toUpperCase() + platformStr.substring(1);
                }

                String transactionStatus = rs.getString("status");
                String statusDisplay;
                if (transactionStatus == null || transactionStatus.isEmpty()) {
                    statusDisplay = "-";
                } else {
                    switch (transactionStatus.toLowerCase()) {
                        case "selesai":
                        case "completed":
                            statusDisplay = "Selesai";
                            break;
                        case "pending":
                            statusDisplay = "Pending";
                            break;
                        case "verified":
                            statusDisplay = "Terverifikasi";
                            break;
                        case "cancelled":
                        case "dibatalkan":
                            statusDisplay = "Dibatalkan";
                            break;
                        case "sent":
                            statusDisplay = "Dikirim";
                            break;
                        default:
                            statusDisplay = transactionStatus.substring(0, 1).toUpperCase()
                                    + transactionStatus.substring(1);
                            break;
                    }
                }

                String namaPelanggan = rs.getString("nama_pelanggan");
                if (namaPelanggan == null || namaPelanggan.isEmpty())
                    namaPelanggan = "-";

                String paymentMethodStr = rs.getString("payment_method");
                if (paymentMethodStr == null || paymentMethodStr.isEmpty()) {
                    paymentMethodStr = "Tunai";
                } else {
                    // Format payment method nicely
                    switch (paymentMethodStr.toLowerCase()) {
                        case "tunai":
                        case "cash":
                            paymentMethodStr = "Tunai";
                            break;
                        case "qris":
                            paymentMethodStr = "QRIS";
                            break;
                        case "transfer_bca":
                        case "transfer bca":
                            paymentMethodStr = "Transfer BCA";
                            break;
                        default:
                            paymentMethodStr = paymentMethodStr.substring(0, 1).toUpperCase()
                                    + paymentMethodStr.substring(1);
                    }
                }

                model.addRow(new Object[] {
                        rs.getString("kode_transaksi"),
                        tanggalStr,
                        waktuStr,
                        kasirDisplay,
                        namaPelanggan,
                        platformStr,
                        paymentMethodStr,
                        "Rp " + df.format(total),
                        "Rp " + df.format(modal),
                        "Rp " + df.format(laba),
                        statusDisplay,
                        "Lihat Detail"
                });
            }

            // Update judul dengan info filter
            String filterInfo = getFilterInfo(jenisFilter, dari, sampai, kasirUsername, kategori, status);
            setTitle("📊 Laporan Keuangan - Distro Zone | " + filterInfo);

            // Debug info
            System.out.println("✅ Data loaded: " + rowCount + " transaksi");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat laporan keuangan:\n" + e.getMessage());
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateStatistics(java.sql.Date dari, java.sql.Date sampai,
            String kasirUsername, String kategori, String status, String platform, String paymentMethod) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/distrozone_db", "root", "");

            // 1. QUERY UMUM (OMSET & TRANSAKSI)
            StringBuilder sqlTrans = new StringBuilder();
            sqlTrans.append("SELECT COUNT(DISTINCT t.id) AS total_transaksi, COALESCE(SUM(t.total), 0) AS total_omset ")
                    .append("FROM TRANSAKSI t ")
                    .append("JOIN users u ON t.kasir_id = u.id ")
                    .append("WHERE 1=1 ");

            appendFilters(sqlTrans, dari, sampai, kasirUsername, kategori, status, platform, paymentMethod);

            PreparedStatement pstTrans = conn.prepareStatement(sqlTrans.toString());
            setFilterParams(pstTrans, dari, sampai, kasirUsername, kategori, status, platform, paymentMethod);

            ResultSet rsTrans = pstTrans.executeQuery();
            int totalTransaksi = 0;
            double totalOmset = 0;
            if (rsTrans.next()) {
                totalTransaksi = rsTrans.getInt("total_transaksi");
                totalOmset = rsTrans.getDouble("total_omset");
            }
            rsTrans.close();
            pstTrans.close();

            // 2. QUERY MODAL & LABA
            // Must join DETAIL_TRANSAKSI for calculations, but respect transaction-level
            // filters
            StringBuilder sqlDetail = new StringBuilder();
            sqlDetail.append("SELECT COALESCE(SUM(d.harga_modal * d.qty), 0) AS total_modal, ")
                    .append("COALESCE(SUM(d.laba), 0) AS total_laba ")
                    .append("FROM TRANSAKSI t ")
                    .append("JOIN users u ON t.kasir_id = u.id ")
                    .append("JOIN DETAIL_TRANSAKSI d ON t.id = d.transaksi_id ")
                    .append("WHERE 1=1 ");

            appendFilters(sqlDetail, dari, sampai, kasirUsername, kategori, status, platform, paymentMethod);

            PreparedStatement pstDetail = conn.prepareStatement(sqlDetail.toString());
            setFilterParams(pstDetail, dari, sampai, kasirUsername, kategori, status, platform, paymentMethod);

            ResultSet rsDetail = pstDetail.executeQuery();
            double totalModal = 0;
            double totalLaba = 0;
            if (rsDetail.next()) {
                totalModal = rsDetail.getDouble("total_modal");
                totalLaba = rsDetail.getDouble("total_laba");
            }
            rsDetail.close();
            pstDetail.close();

            // 3. QUERY PEMBAYARAN (FIX: Now uses same filters as Omset)
            StringBuilder sqlPayment = new StringBuilder();
            sqlPayment.append("SELECT t.payment_method, COALESCE(SUM(t.total), 0) AS total_amount ")
                    .append("FROM TRANSAKSI t ")
                    .append("JOIN users u ON t.kasir_id = u.id ")
                    .append("WHERE 1=1 ");

            appendFilters(sqlPayment, dari, sampai, kasirUsername, kategori, status, platform, paymentMethod);
            sqlPayment.append(" GROUP BY t.payment_method");

            PreparedStatement pstPayment = conn.prepareStatement(sqlPayment.toString());
            setFilterParams(pstPayment, dari, sampai, kasirUsername, kategori, status, platform, paymentMethod);

            ResultSet rsPayment = pstPayment.executeQuery();
            double totalTunai = 0;
            double totalQRIS = 0;
            double totalBCA = 0;

            while (rsPayment.next()) {
                String method = rsPayment.getString("payment_method");
                double amount = rsPayment.getDouble("total_amount");

                if ("Tunai".equalsIgnoreCase(method)) {
                    totalTunai = amount;
                } else if ("QRIS".equalsIgnoreCase(method)) {
                    totalQRIS = amount;
                } else if (method != null && method.toLowerCase().contains("bca")) {
                    totalBCA = amount;
                }
            }
            rsPayment.close();
            pstPayment.close();

            // SET UI
            lblTotalOmset.setText("Rp " + df.format(totalOmset));
            lblTotalTransaksi.setText(String.valueOf(totalTransaksi));
            lblTotalModal.setText("Rp " + df.format(totalModal));
            lblTotalLaba.setText("Rp " + df.format(totalLaba));
            lblTotalTunai.setText("Rp " + df.format(totalTunai));
            lblTotalQRIS.setText("Rp " + df.format(totalQRIS));
            lblTotalBCA.setText("Rp " + df.format(totalBCA));

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat statistik:\n" + e.getMessage());
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // HELPER METHODS TO REDUCE DUPLICATION AND ENSURE CONSISTENCY
    private void appendFilters(StringBuilder sql, java.sql.Date dari, java.sql.Date sampai,
            String kasirUsername, String kategori, String status, String platform, String paymentMethod) {

        if (dari != null && sampai != null) {
            sql.append("AND t.tanggal BETWEEN ? AND DATE_SUB(?, INTERVAL 1 DAY) ");
        }
        if (kasirUsername != null && !kasirUsername.isEmpty()) {
            sql.append("AND u.username = ? ");
        }
        if (status != null && !status.isEmpty()) {
            if (status.equalsIgnoreCase("selesai")) {
                sql.append("AND t.status IN ('selesai', 'completed') ");
            } else {
                sql.append("AND t.status = ? ");
            }
        }
        if (kategori != null && !kategori.isEmpty()) {
            sql.append("AND EXISTS (SELECT 1 FROM DETAIL_TRANSAKSI dt ")
                    .append("JOIN KAOS_VARIAN kv ON dt.kaos_id = kv.id ")
                    .append("JOIN KAOS_MASTER km ON kv.kaos_master_id = km.id ")
                    .append("JOIN kategori kat ON km.kategori_id = kat.id ")
                    .append("WHERE dt.transaksi_id = t.id AND kat.nama_kategori = ?) ");
        }
        if (platform != null && !platform.isEmpty()) {
            sql.append("AND t.platform = ? ");
        }
        if (paymentMethod != null && !paymentMethod.isEmpty()) {
            sql.append("AND t.payment_method = ? ");
        }
    }

    private void setFilterParams(PreparedStatement pst, java.sql.Date dari, java.sql.Date sampai,
            String kasirUsername, String kategori, String status, String platform, String paymentMethod)
            throws SQLException {
        int idx = 1;
        if (dari != null && sampai != null) {
            pst.setDate(idx++, dari);
            pst.setDate(idx++, sampai);
        }
        if (kasirUsername != null && !kasirUsername.isEmpty()) {
            String username = kasirUsername.substring(kasirUsername.indexOf("(") + 1, kasirUsername.indexOf(")"));
            pst.setString(idx++, username);
        }
        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("selesai")) {
            pst.setString(idx++, status);
        }
        if (kategori != null && !kategori.isEmpty()) {
            pst.setString(idx++, kategori);
        }
        if (platform != null && !platform.isEmpty()) {
            pst.setString(idx++, platform);
        }
        if (paymentMethod != null && !paymentMethod.isEmpty()) {
            pst.setString(idx++, paymentMethod);
        }
    }

    private String getFilterInfo(String jenisFilter, java.sql.Date dari, java.sql.Date sampai,
            String kasirUsername, String kategori, String status) {
        StringBuilder info = new StringBuilder();

        if (jenisFilter != null && !"Semua".equals(jenisFilter)) {
            info.append(jenisFilter);
        }

        if (dari != null && sampai != null && !"Semua".equals(jenisFilter)) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            info.append(" (").append(sdf.format(dari)).append(" s/d ").append(sdf.format(sampai)).append(")");
        }

        if (kasirUsername != null && !kasirUsername.isEmpty()) {
            info.append(" | Kasir: ").append(kasirUsername);
        }

        if (kategori != null && !kategori.isEmpty()) {
            info.append(" | Kategori: ").append(kategori);
        }

        if (status != null && !status.isEmpty() && !"Semua Status".equals(status)) {
            info.append(" | Status: ").append(status);
        }

        return info.toString().isEmpty() ? "Semua Data" : info.toString();
    }

    private void showDetailTransaksi(int row) {
        String kodeTransaksi = (String) table.getValueAt(row, 0);
        String currentStatus = (String) table.getValueAt(row, 7); // Ambil status dari tabel

        JDialog dialog = new JDialog(this, "Detail Transaksi: " + kodeTransaksi, true);
        dialog.setSize(650, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setMargin(new Insets(10, 10, 10, 10));

        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/distrozone_db", "root", "");

            String sql = "SELECT t.kode_transaksi, t.tanggal, t.waktu, t.total, t.status, " +
                    "u.nama AS kasir_nama, u.username AS kasir_username, t.platform, " +
                    "km.nama_kaos, km.merek, kv.size, kv.warna, d.qty, d.harga_jual, " +
                    "d.harga_modal, d.subtotal, d.laba " +
                    "FROM TRANSAKSI t " +
                    "JOIN users u ON t.kasir_id = u.id " +
                    "JOIN DETAIL_TRANSAKSI d ON t.id = d.transaksi_id " +
                    "JOIN KAOS_VARIAN kv ON d.kaos_id = kv.id " +
                    "JOIN KAOS_MASTER km ON kv.kaos_master_id = km.id " +
                    "WHERE t.kode_transaksi = ?";

            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, kodeTransaksi);
            ResultSet rs = pst.executeQuery();

            StringBuilder detail = new StringBuilder();
            detail.append("DETAIL TRANSAKSI\n");
            detail.append("==============================\n\n");

            boolean firstRow = true;
            double totalTransaksi = 0;
            double totalModal = 0;
            double totalLaba = 0;
            int itemCount = 0;
            String platform = "desktop";

            while (rs.next()) {
                if (firstRow) {
                    platform = rs.getString("platform");
                    if (platform == null)
                        platform = "desktop";

                    detail.append("Kode Transaksi: ").append(rs.getString("kode_transaksi")).append("\n");
                    detail.append("Tanggal     : ").append(rs.getDate("tanggal")).append("\n");
                    detail.append("Waktu       : ").append(rs.getTime("waktu")).append("\n");
                    detail.append("Platform    : ").append(platform.toUpperCase()).append("\n");
                    detail.append("Kasir       : ").append(rs.getString("kasir_nama")).append(" (")
                            .append(rs.getString("kasir_username")).append(")\n");
                    detail.append("Status      : ").append(rs.getString("status").toUpperCase()).append("\n");
                    detail.append("==============================\n");
                    detail.append("DETAIL ITEM:\n");
                    detail.append("==============================\n");
                    firstRow = false;
                }

                String produkInfo = String.format("%s - %s (%s, %s)",
                        rs.getString("nama_kaos"),
                        rs.getString("merek"),
                        rs.getString("warna"),
                        rs.getString("size"));

                detail.append(String.format("%-40s\n", produkInfo));
                detail.append(String.format("  %3d x Rp %10s = Rp %10s\n",
                        rs.getInt("qty"),
                        df.format(rs.getDouble("harga_jual")),
                        df.format(rs.getDouble("subtotal"))));
                // Opsional: Tampilkan modal/laba hanya jika admin yang melihat (ctx admin)
                // detail.append(String.format(" (Modal: Rp %s, Laba: Rp %s)\n\n",
                // df.format(rs.getDouble("harga_modal")),
                // df.format(rs.getDouble("laba"))));
                detail.append("\n");

                itemCount++;
                totalTransaksi = rs.getDouble("total");
                totalModal += rs.getDouble("harga_modal") * rs.getInt("qty");
                totalLaba += rs.getDouble("laba");
            }

            detail.append("==============================\n");
            detail.append("Total Item : ").append(itemCount).append("\n");
            detail.append("Total Modal: Rp ").append(df.format(totalModal)).append("\n");
            detail.append("Total Laba : Rp ").append(df.format(totalLaba)).append("\n");
            detail.append("GRAND TOTAL: Rp ").append(df.format(totalTransaksi));

            textArea.setText(detail.toString());
            textArea.setCaretPosition(0);

        } catch (Exception e) {
            e.printStackTrace();
            textArea.setText("Gagal memuat detail: " + e.getMessage());
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
            }
        }

        JScrollPane scrollPane = new JScrollPane(textArea);
        dialog.add(scrollPane, BorderLayout.CENTER);

        // ================= BUTTON PANEL =================
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnClose = new JButton("Tutup");
        btnClose.addActionListener(e -> dialog.dispose());

        // Tombol Update Status (Hanya jika belum selesai/dibatalkan)
        // REMOVED: User requested to remove "Pesanan Tiba / Selesai" from desktop.
        // Logic moved to web only.

        btnPanel.add(btnClose);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void showLabaPerKasir() {
        java.util.Date dariDate = (java.util.Date) dariTanggal.getValue();
        java.util.Date sampaiDate = (java.util.Date) sampaiTanggal.getValue();

        if (dariDate == null || sampaiDate == null) {
            JOptionPane.showMessageDialog(this, "Pilih periode tanggal terlebih dahulu!");
            return;
        }

        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/distrozone_db", "root", "");

            String sql = "SELECT " +
                    "u.nama AS kasir_nama, " +
                    "u.username AS kasir_username, " +
                    "COALESCE(ts.total_transaksi, 0) AS total_transaksi, " +
                    "COALESCE(ts.total_omset, 0) AS total_omset, " +
                    "COALESCE(ds.total_modal, 0) AS total_modal, " +
                    "COALESCE(ds.total_laba, 0) AS total_laba " +
                    "FROM users u " +
                    "LEFT JOIN (" +
                    "    SELECT kasir_id, COUNT(id) as total_transaksi, SUM(total) as total_omset " +
                    "    FROM TRANSAKSI " +
                    "    WHERE status = 'selesai' AND tanggal BETWEEN ? AND DATE_SUB(?, INTERVAL 1 DAY) " +
                    "    GROUP BY kasir_id" +
                    ") ts ON u.id = ts.kasir_id " +
                    "LEFT JOIN (" +
                    "    SELECT t.kasir_id, SUM(d.harga_modal * d.qty) as total_modal, SUM(d.laba) as total_laba " +
                    "    FROM TRANSAKSI t " +
                    "    JOIN DETAIL_TRANSAKSI d ON t.id = d.transaksi_id " +
                    "    WHERE t.status = 'selesai' AND t.tanggal BETWEEN ? AND DATE_SUB(?, INTERVAL 1 DAY) " +
                    "    GROUP BY t.kasir_id" +
                    ") ds ON u.id = ds.kasir_id " +
                    "WHERE u.role = 'kasir' AND u.status = 'active' " +
                    "ORDER BY total_laba DESC";

            PreparedStatement pst = conn.prepareStatement(sql);
            // Params for ts subquery
            pst.setDate(1, new java.sql.Date(dariDate.getTime()));
            pst.setDate(2, new java.sql.Date(sampaiDate.getTime()));
            // Params for ds subquery
            pst.setDate(3, new java.sql.Date(dariDate.getTime()));
            pst.setDate(4, new java.sql.Date(sampaiDate.getTime()));

            ResultSet rs = pst.executeQuery();

            StringBuilder report = new StringBuilder();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

            report.append("LAPORAN LABA PER KASIR\n");
            report.append("Periode: ").append(sdf.format(dariDate)).append(" s/d ").append(sdf.format(sampaiDate))
                    .append("\n");
            report.append("================================================================================\n\n");

            boolean hasData = false;
            double grandTotalOmset = 0;
            double grandTotalModal = 0;
            double grandTotalLaba = 0;

            while (rs.next()) {
                hasData = true;
                String kasir = rs.getString("kasir_nama") + " (" + rs.getString("kasir_username") + ")";
                int totalTransaksi = rs.getInt("total_transaksi");
                double omset = rs.getDouble("total_omset");
                double modal = rs.getDouble("total_modal");
                double laba = rs.getDouble("total_laba");

                report.append("Kasir: ").append(kasir).append("\n");
                report.append("  Total Transaksi: ").append(totalTransaksi).append("\n");
                report.append("  Total Omset    : Rp ").append(df.format(omset)).append("\n");
                report.append("  Total Modal    : Rp ").append(df.format(modal)).append("\n");
                report.append("  Total Laba     : Rp ").append(df.format(laba)).append("\n");
                report.append("  Rata Laba/Transaksi: Rp ")
                        .append(df.format(totalTransaksi > 0 ? laba / totalTransaksi : 0)).append("\n");
                report.append("--------------------------------------------------------------------------------\n\n");

                grandTotalOmset += omset;
                grandTotalModal += modal;
                grandTotalLaba += laba;
            }

            if (!hasData) {
                report.append("Tidak ada data transaksi untuk periode ini.\n");
            } else {
                report.append("\n");
                report.append("RINGKASAN KESELURUHAN:\n");
                report.append("================================================================================\n");
                report.append("Total Omset Seluruh Kasir : Rp ").append(df.format(grandTotalOmset)).append("\n");
                report.append("Total Modal Seluruh Kasir : Rp ").append(df.format(grandTotalModal)).append("\n");
                report.append("TOTAL LABA SELURUH KASIR  : Rp ").append(df.format(grandTotalLaba)).append("\n");
            }

            JTextArea textArea = new JTextArea(report.toString());
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(700, 500));

            JOptionPane.showMessageDialog(this, scrollPane, "Laba per Kasir",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat laporan laba per kasir:\n" + e.getMessage());
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void exportToPDF() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Tidak ada data untuk di-export!", "Peringatan",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Build statistics map with all financial data
        java.util.LinkedHashMap<String, String> stats = new java.util.LinkedHashMap<>();
        stats.put("Total Transaksi", lblTotalTransaksi.getText());
        stats.put("Total Omset", lblTotalOmset.getText());
        stats.put("Total Modal", lblTotalModal.getText());
        stats.put("Total Laba", lblTotalLaba.getText());
        stats.put("Tunai", lblTotalTunai.getText());
        stats.put("QRIS", lblTotalQRIS.getText());
        stats.put("BCA", lblTotalBCA.getText());

        String subtitle = "Admin: " + adminName + " | Periode: " +
                new SimpleDateFormat("dd/MM/yyyy").format((java.util.Date) dariTanggal.getValue()) + " - " +
                new SimpleDateFormat("dd/MM/yyyy").format((java.util.Date) sampaiTanggal.getValue());
        String filename = "laporan_keuangan_" + new SimpleDateFormat("yyyyMMdd").format(new java.util.Date());

        util.PDFExporter.exportWithDialog(table, "LAPORAN KEUANGAN DISTRO ZONE", subtitle, stats, this, filename);
    }

    private void exportToExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Simpan Laporan Excel");
        fileChooser.setSelectedFile(new java.io.File("laporan_keuangan_distrozone.xlsx"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();

            // Simulasi export (bisa diimplementasi dengan Apache POI atau library lain)
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                String dariStr = sdf.format(((java.util.Date) dariTanggal.getValue()));
                String sampaiStr = sdf.format(((java.util.Date) sampaiTanggal.getValue()));

                StringBuilder csvContent = new StringBuilder();
                csvContent.append("Laporan Keuangan Distro Zone\n");
                csvContent.append("Periode: ").append(dariStr).append(" s/d ").append(sampaiStr).append("\n\n");

                // Header tabel
                for (int i = 0; i < model.getColumnCount() - 1; i++) { // -1 untuk skip kolom detail
                    csvContent.append(model.getColumnName(i)).append(",");
                }
                csvContent.append("\n");

                // Data tabel
                for (int row = 0; row < model.getRowCount(); row++) {
                    for (int col = 0; col < model.getColumnCount() - 1; col++) {
                        Object value = model.getValueAt(row, col);
                        if (value != null) {
                            csvContent.append(value.toString().replace(",", ".")).append(",");
                        } else {
                            csvContent.append(",");
                        }
                    }
                    csvContent.append("\n");
                }

                // Tambahkan statistik
                csvContent.append("\n\nSTATISTIK:\n");
                csvContent.append("Total Transaksi: ").append(lblTotalTransaksi.getText()).append("\n");
                csvContent.append("Total Omset: ").append(lblTotalOmset.getText()).append("\n");
                csvContent.append("Total Modal: ").append(lblTotalModal.getText()).append("\n");
                csvContent.append("Total Laba: ").append(lblTotalLaba.getText()).append("\n");

                // Tulis ke file
                java.io.FileWriter writer = new java.io.FileWriter(file.getAbsolutePath() + ".csv");
                writer.write(csvContent.toString());
                writer.close();

                JOptionPane.showMessageDialog(this,
                        "✅ Laporan berhasil diexport!\nFile: " + file.getAbsolutePath() + ".csv",
                        "Export Berhasil",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "❌ Gagal export laporan:\n" + ex.getMessage(),
                        "Export Gagal",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Print laporan langsung ke printer
    private void printLaporan() {
        try {
            if (table.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this,
                        "Tidak ada data untuk dicetak!",
                        "Info",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Use MessageFormat for header and footer
            java.text.MessageFormat headerFormat = new java.text.MessageFormat("Laporan Keuangan - DistroZone");
            java.text.MessageFormat footerFormat = new java.text.MessageFormat("Halaman {0}");

            // Print the table
            boolean complete = table.print(
                    JTable.PrintMode.FIT_WIDTH,
                    headerFormat,
                    footerFormat);

            if (complete) {
                JOptionPane.showMessageDialog(this,
                        "Cetak laporan berhasil!",
                        "Cetak Berhasil",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Cetak laporan dibatalkan.",
                        "Info",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (java.awt.print.PrinterException ex) {
            JOptionPane.showMessageDialog(this,
                    "Gagal mencetak laporan:\n" + ex.getMessage(),
                    "Cetak Gagal",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Untuk kompatibilitas jika ada yang memanggil main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LaporanKeuangan frame = new LaporanKeuangan(1, "Admin", "ADM001");
            frame.setVisible(true);
        });
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}