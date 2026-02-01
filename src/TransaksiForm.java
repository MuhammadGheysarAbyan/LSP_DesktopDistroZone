import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.io.File;
import java.util.Date;

public class TransaksiForm extends JFrame {
    private JTable tableProduk, tableKeranjang;
    private DefaultTableModel modelProduk, modelKeranjang;
    private JTextField txtCari, txtTotal, txtBayar, txtKembalian, txtNamaPelanggan; // Added txtNamaPelanggan
    private JComboBox<String> cmbPayment;
    private JLabel lblKodeTransaksi, lblTanggalWaktu, lblKasirIcon;
    private Connection conn;
    private javax.swing.table.TableRowSorter<DefaultTableModel> produkSorter; // For instant search filter

    // Data kasir
    private int kasirId;
    private String kasirName;
    private String userCode;

    // Warna tema hijau Distro Zone (Emerald Green - Sesuai Web)
    private static final Color GREEN_PRIMARY = new Color(16, 185, 129); // #10B981
    private static final Color GREEN_DARK = new Color(15, 118, 110); // #0F766E
    private static final Color GREEN_LIGHT = new Color(52, 211, 153); // #34D399
    private static final Color GREEN_BRIGHT = new Color(16, 185, 129); // #10B981
    private static final Color RED_ERROR = new Color(239, 68, 68); // #EF4444
    private static final Color GRAY_BACK = new Color(100, 116, 139); // #64748B
    private static final Color TEXT_WHITE = new Color(255, 255, 255);
    private static final Color TEXT_GRAY = new Color(148, 163, 184); // #94A3B8

    private static final DecimalFormat RUPIAH = new DecimalFormat("#,###");

    // ================= KONSTRUKTOR =================
    public TransaksiForm(int kasirId, String kasirName, String userCode) {
        this.kasirId = kasirId;
        this.kasirName = kasirName;
        this.userCode = userCode;

        setTitle("🛒 Transaksi Penjualan - " + kasirName);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        connectDatabase();
        initUI();
        loadProduk("");
        generateKodeTransaksi();
        updateTanggalWaktu();
    }

    private void connectDatabase() {
        try {
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/distrozone_db", "root", "");
            System.out.println("✅ Database Transaksi connected");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Koneksi database gagal!\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void closeDatabase() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initUI() {
        // ================= BACKGROUND GRADIENT HIJAU =================
        JPanel mainPanel = new JPanel() {
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
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        add(mainPanel);

        // ================= HEADER =================
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

        // Title dengan icon
        JLabel lblHeader = new JLabel("Transaksi Penjualan");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblHeader.setForeground(TEXT_WHITE);
        lblHeader.setHorizontalAlignment(SwingConstants.CENTER);

        // Add minimal cart icon to title
        try {
            java.net.URL url = getClass().getResource("/img/chart.png"); // Use chart/cart icon
            if (url != null) {
                ImageIcon icon = new ImageIcon(url);
                Image img = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                lblHeader.setIcon(new ImageIcon(img));
                lblHeader.setIconTextGap(15);
                lblHeader.setHorizontalTextPosition(SwingConstants.RIGHT); // Icon on left, Text on right
            }
        } catch (Exception e) {
        }

        // Panel info transaksi
        JPanel infoPanel = new JPanel(new GridLayout(1, 2, 30, 0));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(15, 50, 0, 50));

        // Initialize lblKodeTransaksi (hidden but needed for logic if referenced
        // elsewhere, or just remove logic later)
        lblKodeTransaksi = new JLabel("TRX-");

        // Tanggal & Waktu
        JPanel panelTanggal = createInfoPanel("Tanggal & Waktu:", lblTanggalWaktu = new JLabel(""));

        // Nama Kasir
        // Nama Kasir with Photo
        // Nama Kasir with Photo (Static)
        // Nama Kasir with Photo (Loaded dynamically)
        ImageIcon userIcon = getUserPhoto(kasirId, 45);
        lblKasirIcon = new JLabel(userIcon);

        JLabel lblKasirName = new JLabel(kasirName + " (" + userCode + ")");
        lblKasirName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblKasirName.setForeground(TEXT_WHITE);

        JPanel panelKasir = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelKasir.setOpaque(false);
        panelKasir.add(new JLabel("Kasir:")); // Title
        panelKasir.add(lblKasirIcon);
        panelKasir.add(lblKasirName);

        // Make title small like createInfoPanel
        ((JLabel) panelKasir.getComponent(0)).setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ((JLabel) panelKasir.getComponent(0)).setForeground(TEXT_GRAY);

        // infoPanel.add(panelKode); // Removed
        infoPanel.add(panelTanggal);
        infoPanel.add(panelKasir);

        headerPanel.add(lblHeader, BorderLayout.NORTH);
        headerPanel.add(infoPanel, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ================= PANEL TENGAH =================
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // ===== PANEL KIRI (PRODUK) - DIPERBAIKI =====
        JPanel panelProduk = createCardPanel("📋 DAFTAR PRODUK");
        panelProduk.setLayout(new BorderLayout(10, 10));

        // Panel Pencarian - DIPINDAHKAN KE ATAS
        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        txtCari = new JTextField();
        txtCari.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtCari.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREEN_DARK, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JButton btnCari = createStyledButton("Cari", GREEN_LIGHT, 100, 35, "search.png");
        btnCari.setToolTipText("Cari Produk");
        searchPanel.add(new JLabel("Cari Produk:"), BorderLayout.WEST);
        searchPanel.add(txtCari, BorderLayout.CENTER);
        searchPanel.add(btnCari, BorderLayout.EAST);

        // Tabel Produk - DIPERBESAR
        modelProduk = new DefaultTableModel(
                new String[] { "Kode", "Nama Kaos", "Kategori", "Harga", "Stok", "Size", "Warna", "Foto" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 7)
                    return ImageIcon.class;
                return Object.class;
            }
        };

        tableProduk = createStyledTable(modelProduk);

        JScrollPane spProduk = new JScrollPane(tableProduk);
        spProduk.setBorder(BorderFactory.createLineBorder(GREEN_DARK, 1));

        // Panel untuk tombol tambah - DIPINDAHKAN KE BAWAH
        JPanel panelTombol = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panelTombol.setOpaque(false);
        panelTombol.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JButton btnTambah = createStyledButton("Tambah", GREEN_BRIGHT, 120, 35, "add.png");
        btnTambah.setToolTipText("Tambah ke Keranjang");

        panelTombol.add(btnTambah);

        // Layout panel produk - DIPERBAIKI
        JPanel panelProdukContent = new JPanel(new BorderLayout(0, 10));
        panelProdukContent.setOpaque(false);
        panelProdukContent.add(searchPanel, BorderLayout.NORTH);
        panelProdukContent.add(spProduk, BorderLayout.CENTER);
        panelProdukContent.add(panelTombol, BorderLayout.SOUTH);

        panelProduk.add(panelProdukContent, BorderLayout.CENTER);

        // ===== PANEL KANAN (KERANJANG & PEMBAYARAN) =====
        JPanel panelKanank = createCardPanel("KERANJANG BELANJA");

        // Tabel Keranjang
        modelKeranjang = new DefaultTableModel(
                new String[] { "Foto", "Kode", "Nama Kaos", "Size", "Warna", "Harga", "Qty", "Subtotal", "Aksi" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 6 || col == 8; // Qty (6) and Aksi (8) are editable
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0)
                    return ImageIcon.class; // Foto
                return Object.class;
            }
        };

        tableKeranjang = createStyledTable(modelKeranjang);

        JScrollPane spKeranjang = new JScrollPane(tableKeranjang);
        spKeranjang.setBorder(BorderFactory.createLineBorder(GREEN_DARK, 1));

        // Configure Columns
        // 0: Foto
        tableKeranjang.getColumnModel().getColumn(0).setPreferredWidth(60);
        // 6: Qty (Numeric Editor)
        JTextField tfQty = new JTextField();
        util.InputValidator.restrictToNumbers(tfQty);
        tableKeranjang.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(tfQty));

        // 8: Aksi (Button)
        tableKeranjang.getColumnModel().getColumn(8).setCellRenderer(new ButtonRenderer());
        tableKeranjang.getColumnModel().getColumn(8).setCellEditor(new ButtonEditor(new JCheckBox()));
        tableKeranjang.setRowHeight(50); // Increase row height for photos

        // Panel Pembayaran
        JPanel pembayaranPanel = new JPanel(new BorderLayout(10, 10));
        pembayaranPanel.setOpaque(false);
        pembayaranPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // Panel input pembayaran
        JPanel paymentInputPanel = new JPanel(new GridLayout(5, 2, 10, 10)); // Changed rows from 4 to 5
        paymentInputPanel.setOpaque(false);

        txtNamaPelanggan = createStyledTextField("Isi Nama", true, Font.PLAIN, Color.GRAY); // Placeholder

        // Add Placeholder Logic
        txtNamaPelanggan.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (txtNamaPelanggan.getText().equals("Isi Nama")) {
                    txtNamaPelanggan.setText("");
                    txtNamaPelanggan.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (txtNamaPelanggan.getText().isEmpty()) {
                    txtNamaPelanggan.setText("Isi Nama");
                    txtNamaPelanggan.setForeground(Color.GRAY);
                }
            }
        });
        txtTotal = createStyledTextField("Rp 0", true, Font.BOLD, GREEN_DARK);
        txtBayar = createStyledTextField("", true, Font.PLAIN, Color.BLACK); // editable=true for Tunai default
        util.InputValidator.restrictToNumbers(txtBayar);
        txtKembalian = createStyledTextField("Rp 0", true, Font.BOLD, new Color(33, 150, 243));

        String[] paymentMethods = { "Tunai", "QRIS", "Transfer BCA" };
        cmbPayment = new JComboBox<>(paymentMethods);
        cmbPayment.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cmbPayment.setBackground(Color.WHITE);
        cmbPayment.setBorder(BorderFactory.createLineBorder(GREEN_DARK, 1));

        paymentInputPanel.add(new JLabel("Nama Pelanggan:"));
        paymentInputPanel.add(txtNamaPelanggan);
        paymentInputPanel.add(new JLabel("Total:"));
        paymentInputPanel.add(txtTotal);
        paymentInputPanel.add(new JLabel("Metode Pembayaran:"));
        paymentInputPanel.add(cmbPayment);
        paymentInputPanel.add(new JLabel("Bayar:"));
        paymentInputPanel.add(txtBayar);
        paymentInputPanel.add(new JLabel("Kembalian:"));
        paymentInputPanel.add(txtKembalian);

        // Panel tombol aksi
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        JButton btnBayar = createStyledButton("Bayar", GREEN_PRIMARY, 120, 35, "save.png");
        btnBayar.setToolTipText("Bayar (Proses Transaksi)");

        JButton btnBatal = createStyledButton("Batal", RED_ERROR, 110, 35, "cancel.png");
        btnBatal.setToolTipText("Batal / Reset");

        JButton btnKembali = createStyledButton("Kembali", GRAY_BACK, 120, 35, "back.png");
        btnKembali.setToolTipText("Kembali ke Dashboard");

        buttonPanel.add(btnBayar);
        buttonPanel.add(btnBatal);
        buttonPanel.add(btnKembali);

        pembayaranPanel.add(paymentInputPanel, BorderLayout.CENTER);
        pembayaranPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Layout panel kanan
        panelKanank.add(spKeranjang, BorderLayout.CENTER);
        panelKanank.add(pembayaranPanel, BorderLayout.SOUTH);

        centerPanel.add(panelProduk);
        centerPanel.add(panelKanank);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // ================= EVENT HANDLERS =================
        // btnCari now uses filterAction (line ~379) with instant RowFilter
        btnTambah.addActionListener(e -> tambahKeKeranjang());
        btnBatal.addActionListener(e -> {
            modelKeranjang.setRowCount(0);
            hitungTotal();
        });
        btnBayar.addActionListener(e -> prosesPembayaran());
        btnKembali.addActionListener(e -> {
            closeDatabase();
            new KasirDashboard(kasirId, kasirName, userCode).setVisible(true);
            dispose();
        });

        // Initialize TableRowSorter for instant search (like DaftarKaos)
        produkSorter = new javax.swing.table.TableRowSorter<>(modelProduk);
        tableProduk.setRowSorter(produkSorter);

        // Instant live search filter (no database query needed)
        Runnable filterAction = () -> {
            String text = txtCari.getText().trim().toLowerCase();

            if (text.isEmpty()) {
                produkSorter.setRowFilter(null);
            } else {
                javax.swing.RowFilter<DefaultTableModel, Integer> filter = new javax.swing.RowFilter<DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                        // Search in: Kode (0), Nama Kaos (1), Kategori (2), Size (5), Warna (6)
                        for (int i : new int[] { 0, 1, 2, 5, 6 }) {
                            String value = entry.getStringValue(i);
                            if (value != null && value.toLowerCase().contains(text)) {
                                return true;
                            }
                        }
                        return false;
                    }
                };
                produkSorter.setRowFilter(filter);
            }
        };

        txtCari.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterAction.run();
            }

            public void removeUpdate(DocumentEvent e) {
                filterAction.run();
            }

            public void changedUpdate(DocumentEvent e) {
                filterAction.run();
            }
        });

        btnCari.addActionListener(e -> filterAction.run());

        // Qty change listener
        tableKeranjang.getModel().addTableModelListener(e -> {
            if (e.getColumn() == 6) { // Qty column (index 6 now)
                int row = e.getFirstRow();
                try {
                    int qty = Integer.parseInt(modelKeranjang.getValueAt(row, 6).toString());
                    if (qty <= 0) {
                        modelKeranjang.setValueAt(1, row, 6);
                        qty = 1;
                    }
                    double harga = Double.parseDouble(modelKeranjang.getValueAt(row, 5).toString());
                    double subtotal = qty * harga;
                    modelKeranjang.setValueAt(subtotal, row, 7);
                    hitungTotal();
                } catch (NumberFormatException ex) {
                    modelKeranjang.setValueAt(1, row, 6);
                }
            }
        });

        // Bayar field auto format
        txtBayar.getDocument().addDocumentListener(new DocumentListener() {
            boolean editing = false;

            public void insertUpdate(DocumentEvent e) {
                formatBayar();
            }

            public void removeUpdate(DocumentEvent e) {
                formatBayar();
            }

            public void changedUpdate(DocumentEvent e) {
                formatBayar();
            }

            private void formatBayar() {
                if (editing)
                    return;
                editing = true;
                SwingUtilities.invokeLater(() -> {
                    try {
                        String text = txtBayar.getText().replaceAll("[^0-9]", "");
                        if (text.isEmpty()) {
                            txtBayar.setText("");
                        } else {
                            long value = Long.parseLong(text);
                            txtBayar.setText("Rp " + RUPIAH.format(value));
                        }
                    } catch (Exception ex) {
                        txtBayar.setText("");
                    } finally {
                        editing = false;
                        hitungKembalian();
                    }
                });
            }
        });

        // Double click on product table untuk tambah ke keranjang
        tableProduk.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    tambahKeKeranjangDariTabel();
                }
            }
        });

        // Payment method change
        cmbPayment.addActionListener(e -> {
            if (cmbPayment.getSelectedItem().equals("Tunai")) {
                txtBayar.setEditable(true);
                txtBayar.setText("");
                txtBayar.setBackground(Color.WHITE);
            } else {
                txtBayar.setEditable(false);
                txtBayar.setText("Rp " + RUPIAH.format(getTotalAmount()));
                txtBayar.setBackground(new Color(240, 240, 240));
                hitungKembalian();
            }
        });
    }

    private void tambahKeKeranjangDariTabel() {
        int viewRow = tableProduk.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Pilih produk dari tabel terlebih dahulu!", "Peringatan",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert view row index to model row index (penting saat filter/sort aktif)
        int row = tableProduk.convertRowIndexToModel(viewRow);

        try {
            String kodeKaos = (String) modelProduk.getValueAt(row, 0);
            String namaKaos = (String) modelProduk.getValueAt(row, 1);
            String size = (String) modelProduk.getValueAt(row, 5);
            String warna = (String) modelProduk.getValueAt(row, 6);
            String hargaStr = modelProduk.getValueAt(row, 3).toString();
            ImageIcon foto = (ImageIcon) modelProduk.getValueAt(row, 7); // Get image

            // PERBAIKAN: Parse harga yang memiliki format "Rp 85,000"
            double harga = parseHargaFormatted(hargaStr);

            // Ask for quantity
            String qtyStr = JOptionPane.showInputDialog(this,
                    "Masukkan jumlah untuk " + namaKaos + " (" + warna + ", " + size + "):", "1");

            if (qtyStr == null)
                return; // User cancelled

            int qty;
            try {
                qty = Integer.parseInt(qtyStr.trim());
                if (qty <= 0) {
                    JOptionPane.showMessageDialog(this, "Qty harus lebih dari 0!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Masukkan angka yang valid!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check stock
            int stok = (int) modelProduk.getValueAt(row, 4);
            if (qty > stok) {
                JOptionPane.showMessageDialog(this,
                        "Stok tidak cukup!\nStok tersedia: " + stok,
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if already in cart
            for (int i = 0; i < modelKeranjang.getRowCount(); i++) {
                String existingKode = (String) modelKeranjang.getValueAt(i, 1); // Index 1 is Kode (Shifted)

                if (existingKode.equals(kodeKaos)) {
                    int currentQty = (int) modelKeranjang.getValueAt(i, 6); // Index 6 is Qty
                    int newQty = currentQty + qty;

                    if (newQty > stok) {
                        JOptionPane.showMessageDialog(this,
                                "Stok tidak cukup!\nStok tersedia: " + stok,
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    modelKeranjang.setValueAt(newQty, i, 6);
                    double subtotal = newQty * harga;
                    modelKeranjang.setValueAt(subtotal, i, 7); // Index 7 is Subtotal
                    hitungTotal();

                    JOptionPane.showMessageDialog(this,
                            "Qty berhasil ditambahkan!\n" + namaKaos + "\n" +
                                    "Qty baru: " + newQty,
                            "Sukses", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }

            // Add new item
            double subtotal = qty * harga;

            // Resize icon for cart if needed, or use as is (Table row height handles it)
            // But let's ensure it's not too big
            ImageIcon cartIcon = foto;
            if (foto != null) {
                Image img = foto.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                cartIcon = new ImageIcon(img);
            }

            modelKeranjang.addRow(new Object[] {
                    cartIcon, // 0: Foto
                    kodeKaos, // 1: Kode
                    namaKaos, // 2: Nama
                    size, // 3: Size
                    warna, // 4: Warna
                    harga, // 5: Harga
                    qty, // 6: Qty
                    subtotal, // 7: Subtotal
                    "Hapus" // 8: Aksi
            });

            hitungTotal();

            JOptionPane.showMessageDialog(this,
                    "Berhasil ditambahkan ke keranjang!\n" + namaKaos + " (" + warna + ", " + size + ")\n" +
                            "Qty: " + qty + " x Rp " + RUPIAH.format(harga) + " = Rp " + RUPIAH.format(subtotal),
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // PERBAIKAN: Method untuk parse harga dari format "Rp 85,000"
    private double parseHargaFormatted(String hargaStr) {
        try {
            // Hapus "Rp " dan titik/koma
            String clean = hargaStr.replace("Rp ", "").replace(",", "").trim();
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            // Jika gagal, coba parse tanpa membersihkan terlebih dahulu
            try {
                return Double.parseDouble(hargaStr);
            } catch (NumberFormatException e2) {
                return 0;
            }
        }
    }

    private JPanel createCardPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(255, 255, 255, 245));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREEN_PRIMARY, 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(GREEN_DARK);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        panel.add(lblTitle, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createInfoPanel(String title, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTitle.setForeground(TEXT_GRAY);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        valueLabel.setForeground(TEXT_WHITE);

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);

        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor, int width, int height, String iconName) {
        JButton btn = new JButton(text);

        // Load Icon if provided
        if (iconName != null && !iconName.isEmpty()) {
            try {
                // Try classpath
                java.net.URL url = getClass().getResource("/img/" + iconName);
                if (url != null) {
                    ImageIcon icon = new ImageIcon(url);
                    Image img = icon.getImage().getScaledInstance(12, 12, Image.SCALE_SMOOTH); // Smaller icon (12px)
                    btn.setIcon(new ImageIcon(img));
                } else {
                    // Try filesystem
                    File f = new File("src/img/" + iconName);
                    if (f.exists()) {
                        ImageIcon icon = new ImageIcon(f.getAbsolutePath());
                        Image img = icon.getImage().getScaledInstance(12, 12, Image.SCALE_SMOOTH); // Smaller icon
                                                                                                   // (12px)
                        btn.setIcon(new ImageIcon(img));
                    } else {
                        // Fallback check common paths
                        f = new File("img/" + iconName);
                        if (f.exists()) {
                            ImageIcon icon = new ImageIcon(f.getAbsolutePath());
                            Image img = icon.getImage().getScaledInstance(12, 12, Image.SCALE_SMOOTH); // Smaller icon
                                                                                                       // (12px)
                            btn.setIcon(new ImageIcon(img));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Gagal load icon btn: " + iconName);
            }
        }

        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(width, height));

        // Custom Painter for Rounded Flat Buttons (No 3D)
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                // Flat background - no shadow, no gradient
                Color backgroundColor;
                if (btn.getModel().isPressed()) {
                    backgroundColor = bgColor.darker();
                } else if (btn.getModel().isRollover()) {
                    backgroundColor = bgColor.brighter();
                } else {
                    backgroundColor = bgColor;
                }

                g2.setColor(backgroundColor);
                g2.fillRect(0, 0, w, h);

                g2.dispose();
                super.paint(g, c);
            }
        });

        return btn;
    }

    private JTextField createStyledTextField(String text, boolean editable, int fontStyle, Color foreground) {
        JTextField tf = new JTextField(text);
        tf.setEditable(editable);
        tf.setFont(new Font("Segoe UI", fontStyle, 14));
        tf.setForeground(foreground);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREEN_DARK, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        tf.setBackground(Color.WHITE);
        return tf;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    // Modern alternating colors - mint green tint
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(236, 253, 245));
                } else {
                    c.setBackground(new Color(167, 243, 208)); // Selected - emerald light
                }
                return c;
            }
        };
        table.setRowHeight(55); // Increased for images
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Custom Header Renderer to force color
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(GREEN_PRIMARY);
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                return this;
            }
        };
        table.getTableHeader().setDefaultRenderer(headerRenderer);
        table.getTableHeader().setPreferredSize(new Dimension(table.getColumnModel().getTotalColumnWidth(), 40)); // Header
                                                                                                                  // height
        table.setSelectionBackground(GREEN_LIGHT);
        table.setSelectionForeground(Color.BLACK);
        table.setGridColor(new Color(224, 224, 224));

        // Center align for some columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        // Right align for numeric columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        if (model == modelProduk) {
            table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // Kode
            table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer); // Harga
            table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Stok
            table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer); // Size
            table.getColumnModel().getColumn(6).setCellRenderer(centerRenderer); // Warna
        } else {
            // Keranjang Table
            // Col 0 is Foto (Image), do NOT set text renderer
            table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Kode
            table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Size
            table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Warna
            table.getColumnModel().getColumn(5).setCellRenderer(rightRenderer); // Harga
            table.getColumnModel().getColumn(6).setCellRenderer(centerRenderer); // Qty
            table.getColumnModel().getColumn(7).setCellRenderer(rightRenderer); // Subtotal
        }
        return table;
    }

    // ================= DATABASE METHODS =================
    private void loadProduk(String keyword) {
        // Use a background thread to prevent UI freezing
        new Thread(() -> {
            System.out.println("DEBUG: Loading products with new schema (KAOS_MASTER/VARIAN)...");

            // Clear table on EDT
            SwingUtilities.invokeLater(() -> modelProduk.setRowCount(0));

            if (conn == null) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Database tidak terhubung!",
                        "Error", JOptionPane.ERROR_MESSAGE));
                return;
            }

            try {
                String sql = "SELECT kv.kode_varian, km.nama_kaos, kat.nama_kategori, " +
                        "kv.harga, kv.stok, kv.size, kv.warna, km.foto_utama, kv.foto_varian " +
                        "FROM KAOS_MASTER km " +
                        "JOIN KAOS_VARIAN kv ON km.id = kv.kaos_master_id " +
                        "LEFT JOIN kategori kat ON km.kategori_id = kat.id " +
                        "WHERE (km.nama_kaos LIKE ? OR kv.kode_varian LIKE ?) " +
                        "AND kv.stok > 0 " +
                        "ORDER BY km.nama_kaos, kv.warna, kv.size";

                PreparedStatement pst = conn.prepareStatement(sql);
                String searchPattern = "%" + keyword + "%";
                pst.setString(1, searchPattern);
                pst.setString(2, searchPattern);

                ResultSet rs = pst.executeQuery();

                while (rs.next()) {
                    // Stop if connection is closed (User exited)
                    if (conn == null || conn.isClosed())
                        break;

                    // Load Image (Prioritas: Variant -> Master)
                    String fotoPath = rs.getString("foto_varian");
                    if (fotoPath == null || fotoPath.isEmpty()) {
                        fotoPath = rs.getString("foto_utama");
                    }

                    ImageIcon imgIcon = util.ImageHelper.loadImage(fotoPath, 50, 50);

                    // Prepare final variables for EDT
                    final Object[] rowData = new Object[] {
                            rs.getString("kode_varian"),
                            rs.getString("nama_kaos"),
                            rs.getString("nama_kategori"),
                            "Rp " + RUPIAH.format(rs.getDouble("harga")),
                            rs.getInt("stok"),
                            rs.getString("size"),
                            rs.getString("warna"),
                            imgIcon
                    };

                    // Update Table on EDT
                    SwingUtilities.invokeLater(() -> modelProduk.addRow(rowData));
                }

                rs.close();
                pst.close();

            } catch (SQLException e) {
                // Ignore error if caused by closing connection
                if (!e.getMessage().contains("closed")) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                            "Gagal memuat data produk!\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                }
            }
        }).start();
    }

    private void tambahKeKeranjang() {
        // Method ini akan menambah produk yang dipilih di tabel ke keranjang
        tambahKeKeranjangDariTabel();
    }

    private void hitungTotal() {
        double total = 0;
        for (int i = 0; i < modelKeranjang.getRowCount(); i++) {
            total += Double.parseDouble(modelKeranjang.getValueAt(i, 7).toString()); // Index 7 is Subtotal
        }
        txtTotal.setText("Rp " + RUPIAH.format(total));

        // Auto update bayar for non-tunai
        if (cmbPayment != null && !cmbPayment.getSelectedItem().equals("Tunai")) {
            txtBayar.setText("Rp " + RUPIAH.format(total));
        }

        hitungKembalian();
    }

    private double getTotalAmount() {
        try {
            String totalStr = txtTotal.getText().replace("Rp ", "").replace(".", "").replace(",", "").trim();
            return Double.parseDouble(totalStr);
        } catch (Exception e) {
            return 0;
        }
    }

    private void hitungKembalian() {
        try {
            double total = getTotalAmount();

            if (cmbPayment.getSelectedItem().equals("Tunai")) {
                String bayarStr = txtBayar.getText().replace("Rp ", "").replace(".", "").replace(",", "").trim();

                if (bayarStr.isEmpty()) {
                    txtKembalian.setText("Rp 0");
                    return;
                }

                double bayar = Double.parseDouble(bayarStr);
                double kembalian = bayar - total;

                if (kembalian < 0) {
                    txtKembalian.setText("Kurang: Rp " + RUPIAH.format(Math.abs(kembalian)));
                    txtKembalian.setForeground(RED_ERROR);
                } else {
                    txtKembalian.setText("Rp " + RUPIAH.format(kembalian));
                    txtKembalian.setForeground(new Color(33, 150, 243));
                }
            } else {
                txtKembalian.setText("Rp 0");
                txtKembalian.setForeground(new Color(33, 150, 243));
            }

        } catch (NumberFormatException e) {
            txtKembalian.setText("Rp 0");
        }
    }

    private void prosesPembayaran() {
        if (modelKeranjang.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Keranjang kosong!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double total = getTotalAmount();
        String paymentMethod = (String) cmbPayment.getSelectedItem();

        if (paymentMethod.equals("Tunai")) {
            String bayarStr = txtBayar.getText().replace("Rp ", "").replace(".", "").replace(",", "").trim();

            if (bayarStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Masukkan jumlah pembayaran!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                double bayar = Double.parseDouble(bayarStr);
                double kembalian = bayar - total;

                if (kembalian < 0) {
                    JOptionPane.showMessageDialog(this,
                            "Uang tidak cukup!\nTotal: Rp " + RUPIAH.format(total) + "\nBayar: Rp "
                                    + RUPIAH.format(bayar) +
                                    "\nKurang: Rp " + RUPIAH.format(Math.abs(kembalian)),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Format angka tidak valid!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Confirm transaction
        String confirmMsg = "Konfirmasi Transaksi:\n\n" +
                "Total: Rp " + RUPIAH.format(total) + "\n" +
                "Metode: " + paymentMethod + "\n" +
                "Jumlah Item: " + modelKeranjang.getRowCount();

        if (paymentMethod.equals("Tunai")) {
            double bayar = Double
                    .parseDouble(txtBayar.getText().replace("Rp ", "").replace(".", "").replace(",", "").trim());
            double kembalian = bayar - total;
            confirmMsg += "\nBayar: Rp " + RUPIAH.format(bayar) +
                    "\nKembalian: Rp " + RUPIAH.format(kembalian);
        }

        confirmMsg += "\n\nLanjutkan transaksi?";

        int confirm = JOptionPane.showConfirmDialog(this,
                confirmMsg,
                "Konfirmasi",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION)
            return;

        // Untuk QRIS dan Transfer BCA, tampilkan dialog pembayaran QR/VA
        if (paymentMethod.equals("QRIS") || paymentMethod.contains("Transfer")) {
            String kodeTransaksi = lblKodeTransaksi.getText();
            boolean paymentConfirmed = PaymentQRDialog.showPaymentDialog(this, paymentMethod, total, kodeTransaksi);

            if (!paymentConfirmed) {
                JOptionPane.showMessageDialog(this,
                        "Pembayaran dibatalkan.",
                        "Info",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }

        // Start transaction
        saveTransaksi(total, paymentMethod);
    }

    private void saveTransaksi(double total, String paymentMethod) {
        try {
            conn.setAutoCommit(false);

            // Get transaction code from label
            String kodeTransaksi = lblKodeTransaksi.getText();
            String namaPelanggan = txtNamaPelanggan.getText().trim();
            if (namaPelanggan.isEmpty() || namaPelanggan.equals("Isi Nama"))
                namaPelanggan = "Guest";

            // Check status column type in TRANSAKSI table if necessary, or assume
            // 'selesai' for Indonesian consistency
            // For simplicity and alignment with new schema, we'll use 'completed'
            // (Database Schema requires this ENUM value)
            String statusValue = "completed";

            // Insert to TRANSAKSI table status is always selesai as per user request
            // Platform = 'desktop' untuk membedakan dengan transaksi web
            String sqlTrans = "INSERT INTO TRANSAKSI (kode_transaksi, customer_id, nama_pelanggan, total, grand_total, "
                    +
                    "tanggal, kasir_id, payment_method, status, platform, waktu, created_at) " +
                    "VALUES (?, NULL, ?, ?, ?, CURDATE(), ?, ?, ?, 'desktop', CURTIME(), NOW())";

            PreparedStatement pstTrans = conn.prepareStatement(sqlTrans, Statement.RETURN_GENERATED_KEYS);
            pstTrans.setString(1, kodeTransaksi);
            pstTrans.setString(2, namaPelanggan);
            pstTrans.setDouble(3, total);
            pstTrans.setDouble(4, total);
            pstTrans.setInt(5, kasirId);
            pstTrans.setString(6, paymentMethod);
            pstTrans.setString(7, statusValue);

            pstTrans.executeUpdate();

            ResultSet rs = pstTrans.getGeneratedKeys();
            int transaksiId = 0;
            if (rs.next()) {
                transaksiId = rs.getInt(1);
            }

            // Insert detail transaksi and update stock
            for (int i = 0; i < modelKeranjang.getRowCount(); i++) {
                String kodeVarian = (String) modelKeranjang.getValueAt(i, 1); // Index 1: Kode
                int qty = (int) modelKeranjang.getValueAt(i, 6); // Index 6: Qty
                double hargaJual = Double.parseDouble(modelKeranjang.getValueAt(i, 5).toString()); // Index 5: Harga
                double subtotal = Double.parseDouble(modelKeranjang.getValueAt(i, 7).toString()); // Index 7: Subtotal

                // Get varian info (id, harga_pokok, stok) from KAOS_VARIAN
                String sqlKaos = "SELECT id, harga_pokok, stok FROM KAOS_VARIAN WHERE kode_varian = ?";
                PreparedStatement pstKaos = conn.prepareStatement(sqlKaos);
                pstKaos.setString(1, kodeVarian);
                ResultSet rsKaos = pstKaos.executeQuery();

                if (rsKaos.next()) {
                    int varianId = rsKaos.getInt("id");
                    double hargaPokok = rsKaos.getDouble("harga_pokok");
                    int currentStok = rsKaos.getInt("stok");

                    // Check stock again
                    if (qty > currentStok) {
                        throw new SQLException("Stok " + kodeVarian + " tidak cukup!");
                    }

                    // Insert detail transaksi
                    // Use kaos_id to refer to KAOS_VARIAN id.
                    String sqlDetail = "INSERT INTO DETAIL_TRANSAKSI (transaksi_id, kaos_id, qty, harga_jual, harga_modal, subtotal, laba, created_at) "
                            +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";

                    PreparedStatement pstDetail = conn.prepareStatement(sqlDetail);
                    pstDetail.setInt(1, transaksiId);
                    pstDetail.setInt(2, varianId);
                    pstDetail.setInt(3, qty);
                    pstDetail.setDouble(4, hargaJual);
                    pstDetail.setDouble(5, hargaPokok);
                    pstDetail.setDouble(6, subtotal);
                    pstDetail.setDouble(7, (hargaJual - hargaPokok) * qty);
                    pstDetail.executeUpdate();

                    // Update stock in KAOS_VARIAN
                    String sqlUpdate = "UPDATE KAOS_VARIAN SET stok = stok - ? WHERE id = ?";
                    PreparedStatement pstUpdate = conn.prepareStatement(sqlUpdate);
                    pstUpdate.setInt(1, qty);
                    pstUpdate.setInt(2, varianId);
                    pstUpdate.executeUpdate();

                    rsKaos.close();
                    pstKaos.close();
                }
            }

            conn.commit();

            // Get payment details for receipt
            double bayar = 0;
            double kembalian = 0;

            if (paymentMethod.equals("Tunai")) {
                bayar = Double
                        .parseDouble(txtBayar.getText().replace("Rp ", "").replace(".", "").replace(",", "").trim());
                kembalian = bayar - total;
            } else {
                bayar = total;
                kembalian = 0;
            }

            // Show success message
            JOptionPane.showMessageDialog(this,
                    "✅ Transaksi Berhasil Disimpan!\n\n" +
                            "Kode Transaksi: " + kodeTransaksi + "\n" +
                            "Total: Rp " + RUPIAH.format(total) + "\n" +
                            "Metode: " + paymentMethod,
                    "Sukses",
                    JOptionPane.INFORMATION_MESSAGE);

            // Print receipt
            cetakStruk(kodeTransaksi, total, bayar, kembalian, paymentMethod);

            // Reset form
            resetForm();

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "❌ Gagal menyimpan transaksi!\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void generateKodeTransaksi() {
        try {
            String kodeTransaksi = util.CodeGenerator.generateTransactionCode(conn);
            // Hide but keep valid
            lblKodeTransaksi.setText(kodeTransaksi);
        } catch (SQLException e) {
            e.printStackTrace();
            lblKodeTransaksi.setText("ERROR");
        }
    }

    private void updateTanggalWaktu() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        lblTanggalWaktu.setText(sdf.format(new Date()));

        // Update every second
        Timer timer = new Timer(1000, e -> {
            lblTanggalWaktu.setText(sdf.format(new Date()));
        });
        timer.start();
    }

    private void cetakStruk(String kodeTransaksi, double total, double bayar, double kembalian, String paymentMethod) {
        JDialog dialog = new JDialog(this, "Struk Pembayaran", true);
        dialog.setSize(450, 650); // Slightly wider for HTML
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(GREEN_PRIMARY, 2));

        // Use JEditorPane for HTML content (Modern & Centered)
        JEditorPane txtStruk = new JEditorPane();
        txtStruk.setContentType("text/html");
        txtStruk.setEditable(false);
        txtStruk.setBackground(Color.WHITE);

        // Build HTML Content
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Segoe UI, sans-serif; font-size: 11px; color: #333;'>");

        // Logo & Header
        html.append("<div style='text-align: center; margin-bottom: 10px;'>");

        // Try to load logo (as file URI or Resource)
        java.net.URL logoUrl = getClass().getResource("/img/logodistrozone.png");
        if (logoUrl != null) {
            html.append("<img src='").append(logoUrl).append("' width='100'><br>");
        } else {
            // Fallback if resource not found, try absolute path logic if needed or just
            // text
            html.append("<h1 style='color: #4CAF50; margin: 5px 0;'>DISTRO ZONE</h1>");
        }

        html.append("<b>Jl. Raya Pegangsaan Timur No.29H</b><br>");
        html.append("Telp: (021) 12345678");
        html.append("</div>");

        html.append("<hr style='border: 1px dashed #ccc;'>");

        // Info Transaksi
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        html.append("<table style='width: 100%; font-size: 11px;'>");
        html.append("<tr><td>Tgl</td><td>: ").append(sdf.format(new Date())).append("</td></tr>");
        html.append("<tr><td>Kasir</td><td>: ").append(kasirName).append("</td></tr>");
        html.append("<tr><td>Kode</td><td>: ").append(kodeTransaksi).append("</td></tr>");
        html.append("</table>");

        html.append("<br>");

        // Items Table
        html.append("<table style='width: 100%; border-collapse: collapse; font-size: 11px;'>");
        html.append(
                "<tr style='background-color: #f2f2f2;'><th style='text-align:left; padding: 5px;'>Item</th><th style='text-align:center; padding: 5px;'>Qty</th><th style='text-align:right; padding: 5px;'>Subtotal</th></tr>");

        for (int i = 0; i < modelKeranjang.getRowCount(); i++) {
            String nama = ((String) modelKeranjang.getValueAt(i, 1));
            // Trim name if too long
            if (nama.length() > 20)
                nama = nama.substring(0, 20) + "...";

            int qty = 0;
            try {
                qty = Integer.parseInt(modelKeranjang.getValueAt(i, 5).toString());
            } catch (Exception e) {
                qty = 1;
            }

            double subtotal = 0;
            try {
                subtotal = Double.parseDouble(modelKeranjang.getValueAt(i, 6).toString());
            } catch (Exception e) {
                subtotal = 0;
            }

            html.append("<tr>");
            html.append("<td style='padding: 3px; border-bottom: 1px solid #eee;'>").append(nama).append("</td>");
            html.append("<td style='text-align:center; padding: 3px; border-bottom: 1px solid #eee;'>").append(qty)
                    .append("</td>");
            html.append("<td style='text-align:right; padding: 3px; border-bottom: 1px solid #eee;'>").append("Rp ")
                    .append(RUPIAH.format(subtotal)).append("</td>");
            html.append("</tr>");
        }
        html.append("</table>");

        html.append("<br>");

        // Totals
        html.append("<table style='width: 100%; font-size: 11px;'>");
        html.append(
                "<tr><td style='text-align:right; font-weight:bold;'>TOTAL:</td><td style='text-align:right; font-weight:bold;'>Rp ")
                .append(RUPIAH.format(total)).append("</td></tr>");
        html.append("<tr><td style='text-align:right;'>BAYAR:</td><td style='text-align:right;'>Rp ")
                .append(RUPIAH.format(bayar)).append("</td></tr>");
        html.append("<tr><td style='text-align:right;'>KEMBALIAN:</td><td style='text-align:right;'>Rp ")
                .append(RUPIAH.format(kembalian)).append("</td></tr>");
        html.append("</table>");

        html.append("<br>");
        html.append("Metode Bayar: <b>").append(paymentMethod).append("</b><br>");

        html.append("<hr style='border: 1px dashed #ccc;'>");
        html.append("<div style='text-align: center; margin-top: 10px;'>");
        html.append("<b>TERIMA KASIH!</b><br>");
        html.append("Barang yang sudah dibeli tidak dapat ditukar/dikembalikan.");
        html.append("</div>");

        html.append("</body></html>");

        txtStruk.setText(html.toString());
        // Store HTML for PDF saving
        final String htmlContent = html.toString();

        // Scroll pane for content
        JScrollPane scrollPane = new JScrollPane(txtStruk);
        scrollPane.setBorder(null);

        // Button Panel with Fixed Buttons (Reusing Style Logic inline)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);

        JButton btnSavePdf = createStyledButton("Simpan PDF", GREEN_PRIMARY, 130, 35, "save.png");
        JButton btnPrint = createStyledButton("Cetak", new Color(33, 150, 243), 100, 35, "report.png");
        JButton btnClose = createStyledButton("Tutup", GRAY_BACK, 100, 35, "cancel.png");

        // Save to PDF action
        btnSavePdf.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Simpan Struk sebagai PDF");
            fileChooser.setSelectedFile(new File("Struk_" + kodeTransaksi + ".html"));
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("HTML Files", "html"));

            int userSelection = fileChooser.showSaveDialog(dialog);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                if (!fileToSave.getName().toLowerCase().endsWith(".html")) {
                    fileToSave = new File(fileToSave.getAbsolutePath() + ".html");
                }
                try (java.io.PrintWriter writer = new java.io.PrintWriter(fileToSave)) {
                    writer.println(htmlContent);
                    JOptionPane.showMessageDialog(dialog,
                            "✅ Struk berhasil disimpan!\n" + fileToSave.getAbsolutePath(),
                            "Sukses", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog,
                            "❌ Gagal menyimpan struk!\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Print action
        btnPrint.addActionListener(e -> {
            try {
                txtStruk.print();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "❌ Gagal mencetak struk!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnClose.addActionListener(e -> dialog.dispose());

        buttonPanel.add(btnSavePdf);
        buttonPanel.add(btnPrint);
        buttonPanel.add(btnClose);

        panel.add(scrollPane, BorderLayout.CENTER); // Use scrollPane instead of raw component
        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void resetForm() {
        modelKeranjang.setRowCount(0);
        txtTotal.setText("Rp 0");
        txtBayar.setText("");
        txtKembalian.setText("Rp 0");

        // Reset Nama Pelanggan to Placeholder
        txtNamaPelanggan.setText("Isi Nama");
        txtNamaPelanggan.setForeground(Color.GRAY);

        cmbPayment.setSelectedIndex(0);
        loadProduk("");
        generateKodeTransaksi();
    }

    @Override
    public void dispose() {
        closeDatabase();
        super.dispose();
    }

    // IMAGE HELPER METHODS - Moved to util.ImageHelper
    // resolveImagePath removed.

    private ImageIcon createPlaceholderIcon() {
        // Simple placeholder
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(50, 50,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(new Color(230, 230, 230));
        g2d.fillRect(0, 0, 50, 50);
        g2d.setColor(Color.GRAY);
        g2d.drawRect(0, 0, 49, 49);
        g2d.drawString("No Img", 5, 25);
        g2d.dispose();
        return new ImageIcon(image);
    }

    private ImageIcon getUserPhoto(int userId, int size) {
        ImageIcon defaultIcon = createPlaceholderIcon();
        // Try load user.png from resources as better fallback
        try {
            java.net.URL url = getClass().getResource("/img/user.png");
            if (url != null)
                defaultIcon = new ImageIcon(
                        new ImageIcon(url).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
        } catch (Exception e) {
        }

        if (conn == null)
            return defaultIcon;

        try {
            String sql = "SELECT foto FROM users WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String foto = rs.getString("foto");
                if (foto != null && !foto.isEmpty()) {
                    String resolved = util.ImageHelper.resolveImagePath(foto);
                    if (resolved != null) {
                        ImageIcon icon = new ImageIcon(resolved);
                        Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                        return new ImageIcon(createRoundedImage(img, size));
                    }
                }
            }
            rs.close();
            pst.close();
        } catch (Exception e) {
            System.out.println("Gagal load foto user: " + e.getMessage());
        }
        return defaultIcon;
    }

    private java.awt.image.BufferedImage createRoundedImage(Image image, int size) {
        java.awt.image.BufferedImage roundedImage = new java.awt.image.BufferedImage(size, size,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = roundedImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, size, size, 20, 20)); // Rounded Square like Admin
        g2.drawImage(image.getScaledInstance(size, size, Image.SCALE_SMOOTH), 0, 0, null);
        g2.dispose();
        return roundedImage;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new TransaksiForm(1, "Kasir Utama", "KSR001").setVisible(true);
        });
    }

    // ================= INNER CLASSES FOR BUTTON RENDERER & EDITOR
    // =================
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setText("Hapus");
            setForeground(Color.WHITE);
            setBackground(RED_ERROR);
            setFocusPainted(false);
            setBorderPainted(false);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            // Ensure opaque for some LaFs
            setContentAreaFilled(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(RED_ERROR.darker());
            } else {
                setBackground(RED_ERROR);
            }
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private int currentRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setText("Hapus");
            button.setForeground(Color.WHITE);
            button.setBackground(RED_ERROR);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setFont(new Font("Segoe UI", Font.BOLD, 11));
            button.setContentAreaFilled(true); // Ensure opaque

            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            isPushed = true;
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) {
                // Hapus baris logic
                SwingUtilities.invokeLater(() -> {
                    try {
                        // Confirm deletion
                        int confirm = JOptionPane.showConfirmDialog(button, "Hapus item ini dari keranjang?",
                                "Konfirmasi", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            modelKeranjang.removeRow(currentRow);
                            hitungTotal();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            isPushed = false;
            return "Hapus";
        }

        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }

        protected void fireEditingStopped() {
            super.fireEditingStopped();
        }
    }
}