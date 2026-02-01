import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.nio.file.*;
import java.util.UUID;
import java.util.*; // Added for Lists/ArrayLists
import util.InputValidator;
import util.ImageHelper;

public class KaosManagement extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private Integer selectedId = null;
    private String selectedFoto = null;
    private DecimalFormat df;
    private JTextField tfSearch;
    private TableRowSorter<DefaultTableModel> sorter;

    // Identitas admin
    private int userId;
    private String adminName;
    private String userCode;

    // Warna tema hijau Distro Zone (Emerald Green - Sesuai Web)
    private static final Color GREEN_PRIMARY = new Color(16, 185, 129); // #10B981
    private static final Color GREEN_DARK = new Color(15, 118, 110); // #0F766E
    private static final Color GREEN_LIGHT = new Color(52, 211, 153); // #34D399
    private static final Color TEXT_DARK = new Color(31, 41, 55); // #1F2937

    // Data untuk combobox
    private String[] typeOptions = { "Lengan Pendek", "Lengan Panjang" };
    private String[] warnaOptions = { "Hitam", "Putih", "Merah", "Biru", "Hijau", "Kuning", "Abu-abu", "Navy",
            "Maroon" };
    private String[] sizeOptions = { "XS", "S", "M", "L", "XL", "2XL", "3XL", "4XL", "5XL" };

    // Helper untuk mapping warna ke hex
    private String getColorHex(String colorName) {
        if (colorName == null)
            return "#000000";
        switch (colorName.toLowerCase()) {
            case "hitam":
                return "#000000";
            case "putih":
                return "#FFFFFF";
            case "merah":
                return "#FF0000";
            case "biru":
                return "#0000FF";
            case "hijau":
                return "#008000";
            case "kuning":
                return "#FFFF00";
            case "abu-abu":
                return "#808080";
            case "navy":
                return "#000080";
            case "maroon":
                return "#800000";
            default:
                return "#333333"; // Default dark gray for unknown
        }
    }

    // ================= KONSTRUKTOR =================
    public KaosManagement(int userId, String adminName, String userCode) {
        this.userId = userId;
        this.adminName = adminName;
        this.userCode = userCode;

        initUI();
        initData();
    }

    private void initUI() {
        // Setup format angka
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        df = new DecimalFormat("#,###", symbols);

        setTitle("👕 Manajemen Kaos - " + adminName);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10)) {
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

        // ================= HEADER =================
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        headerPanel.setOpaque(false);

        // Load icon box
        ImageIcon headerIcon = createImageIcon("tshirt.png", 40, 40);
        if (headerIcon == null) {
            headerIcon = createBoxIcon(40, 40);
        }

        JLabel lblIcon = new JLabel(headerIcon);

        JLabel lblHeader = new JLabel("Manajemen Kaos & Varian");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblHeader.setForeground(Color.WHITE);

        headerPanel.add(lblIcon);
        headerPanel.add(lblHeader);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ================= PANEL SEARCH =================
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchPanel.setOpaque(false);

        tfSearch = new JTextField();
        tfSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tfSearch.setPreferredSize(new Dimension(400, 35));
        tfSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREEN_DARK, 2),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        tfSearch.setToolTipText("Cari berdasarkan nama, merek, kategori, warna...");

        // Button Cari dengan icon search
        JButton btnCari = createIconButton("search.png", "Cari", GREEN_PRIMARY);
        btnCari.setPreferredSize(new Dimension(120, 35));

        searchPanel.add(new JLabel("Pencarian:"));
        searchPanel.add(tfSearch);
        searchPanel.add(btnCari);

        // ================= TABEL KAOS DENGAN VARIAN =================
        model = new DefaultTableModel(
                new String[] { "ID", "Kode", "Nama Kaos", "Merek", "Kategori", "Varian", "Harga Jual", "Harga Pokok",
                        "Stok Total", "Foto" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 9)
                    return ImageIcon.class;
                if (columnIndex == 5 || columnIndex == 8)
                    return Integer.class;
                return Object.class;
            }
        };

        table = new JTable(model);
        table.setRowHeight(60);

        // Custom Header Renderer
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(GREEN_PRIMARY);
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 13));
                setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
                return this;
            }
        };
        table.getTableHeader().setDefaultRenderer(headerRenderer);
        table.getTableHeader().setPreferredSize(new Dimension(table.getColumnModel().getTotalColumnWidth(), 40));

        table.setSelectionBackground(GREEN_PRIMARY);
        table.setSelectionForeground(Color.WHITE);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Sembunyikan kolom ID
        TableColumn idCol = table.getColumnModel().getColumn(0);
        idCol.setMinWidth(0);
        idCol.setMaxWidth(0);
        idCol.setPreferredWidth(0);

        // Center align untuk kolom Varian, Stok
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer); // Varian
        table.getColumnModel().getColumn(8).setCellRenderer(centerRenderer); // Stok Total

        // Right align untuk harga
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        table.getColumnModel().getColumn(6).setCellRenderer(rightRenderer); // Harga Jual
        table.getColumnModel().getColumn(7).setCellRenderer(rightRenderer); // Harga Pokok

        // Renderer untuk kolom Foto (agar gambar di tengah)
        DefaultTableCellRenderer imageRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = new JLabel();
                if (value instanceof ImageIcon) {
                    label.setIcon((ImageIcon) value);
                    label.setHorizontalAlignment(JLabel.CENTER);
                }
                if (isSelected) {
                    label.setBackground(table.getSelectionBackground());
                    label.setForeground(table.getSelectionForeground());
                } else {
                    label.setBackground(table.getBackground());
                    label.setForeground(table.getForeground());
                }
                label.setOpaque(true);
                return label;
            }
        };
        table.getColumnModel().getColumn(9).setCellRenderer(imageRenderer);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(GREEN_DARK, 2, true));

        // ================= FILTER REALTIME =================
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        Runnable filterAction = () -> {
            String text = tfSearch.getText().trim();
            if (text.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 2, 3, 4)); // Nama, Merek, Kategori
            }
        };

        tfSearch.getDocument().addDocumentListener(new DocumentListener() {
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

        // ================= PANEL TOMBOL =================
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setOpaque(false);

        // Buat tombol dengan icon kecil
        JButton btnTambah = createIconButton("add.png", "Tambah Kaos", GREEN_PRIMARY);
        JButton btnEdit = createIconButton("edit.png", "Edit Kaos", new Color(52, 152, 219));
        JButton btnDetail = createIconButton("detail.png", "Detail", new Color(155, 89, 182));
        JButton btnHapus = createIconButton("delete.png", "Hapus Kaos", new Color(231, 76, 60));
        JButton btnTambahMerek = createIconButton("brand.png", "Tambah Merek", new Color(241, 196, 15)); // Changed from
                                                                                                         // Varian to
                                                                                                         // Merek
        JButton btnKelolaKategori = createIconButton("category.png", "Kategori", new Color(46, 204, 113));
        JButton btnRefresh = createIconButton("refresh.png", "Refresh", new Color(52, 152, 219));
        JButton btnBack = createIconButton("back.png", "Kembali", new Color(153, 153, 153));

        btnPanel.add(btnTambah);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDetail);
        btnPanel.add(btnHapus);
        btnPanel.add(btnTambahMerek);
        // btnKelolaVarian ("Warna & Ukuran") removed
        btnPanel.add(btnKelolaKategori);
        btnPanel.add(btnRefresh);
        // btnReorder ("Rapikan ID") removed
        btnPanel.add(btnBack);

        // ================= LAYOUT UTAMA =================
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setOpaque(false);
        contentPanel.add(searchPanel, BorderLayout.NORTH);
        contentPanel.add(sp, BorderLayout.CENTER);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // ================= ACTIONS =================
        // Actions
        btnTambah.addActionListener(e -> showUnifiedForm(null));

        btnEdit.addActionListener(e -> {
            if (selectedId != null) {
                showUnifiedForm(selectedId);
            } else {
                JOptionPane.showMessageDialog(this, "⚠️ Pilih kaos yang akan diedit!");
            }
        });

        btnDetail.addActionListener(e -> showDetailKaos());
        btnHapus.addActionListener(e -> hapusKaos());
        btnTambahMerek.addActionListener(e -> showMerekDialog()); // Changed action
        // "Kelola Varian" now also opens the Unified Dialog (Shortcut to Edit)
        // btnKelolaVarian listener removed

        btnRefresh.addActionListener(e -> {
            tfSearch.setText("");
            sorter.setRowFilter(null);
            loadData();
        });

        btnKelolaKategori.addActionListener(e -> {
            showKategoriDialog();
        });

        // btnReorder listener removed

        btnBack.addActionListener(e -> {
            dispose();
            if (userCode != null && userCode.startsWith("ADM")) {
                new AdminDashboard(userId, adminName, userCode).setVisible(true);
            } else {
                new KasirDashboard(userId, adminName, userCode).setVisible(true);
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row != -1) {
                    int modelRow = table.convertRowIndexToModel(row);
                    selectedId = (Integer) model.getValueAt(modelRow, 0);
                }
            }
        });

        // Double click untuk detail
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showDetailKaos();
                }
            }
        });
    }

    private void initData() {
        testDatabaseConnection();
        // createTablesIfNotExists(); // Moved check inside
        updateDatabaseSchema(); // Migration
        addSampleDataIfEmpty();
        loadData();
    }

    private void updateDatabaseSchema() {
        createTablesIfNotExists();
        try (Connection conn = getConnection()) {
            // Add type_varian to KAOS_VARIAN if not exists
            addColumnIfNotExists(conn, "KAOS_VARIAN", "type_varian", "VARCHAR(20)");
            // Ensure foto_utama in KAOS_MASTER
            addColumnIfNotExists(conn, "KAOS_MASTER", "foto_utama", "VARCHAR(255)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addColumnIfNotExists(Connection conn, String tableName, String columnName, String columnDef)
            throws SQLException {
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getColumns(null, null, tableName, columnName);
        if (!rs.next()) {
            System.out.println("⚠️ Menambahkan kolom " + columnName + " ke tabel " + tableName);
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDef);
        }
    }

    // ================= CREATE TABLES IF NOT EXISTS =================
    private void createTablesIfNotExists() {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();

            // KAOS_MASTER
            String createMaster = "CREATE TABLE IF NOT EXISTS KAOS_MASTER (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "nama_kaos VARCHAR(100) NOT NULL, " +
                    "merek VARCHAR(50), " +
                    "kategori_id INT, " +
                    "type_kaos VARCHAR(20), " +
                    "deskripsi TEXT, " +
                    "foto_utama VARCHAR(255), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")";
            stmt.execute(createMaster);

            // KAOS_VARIAN
            String createVarian = "CREATE TABLE IF NOT EXISTS KAOS_VARIAN (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "kaos_master_id INT NOT NULL, " +
                    "kode_varian VARCHAR(20) UNIQUE, " +
                    "warna VARCHAR(30), " +
                    "warna_hex VARCHAR(10), " +
                    "size VARCHAR(10), " +
                    "harga DECIMAL(12,2), " +
                    "harga_pokok DECIMAL(12,2), " +
                    "stok INT DEFAULT 0, " +
                    "foto_varian VARCHAR(255), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (kaos_master_id) REFERENCES KAOS_MASTER(id) ON DELETE CASCADE" +
                    ")";
            stmt.execute(createVarian);

            // MEREK TABLE
            String createMerek = "CREATE TABLE IF NOT EXISTS MEREK (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "nama_merek VARCHAR(50) NOT NULL UNIQUE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            stmt.execute(createMerek);

            // Populate MEREK from KAOS_MASTER if empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM MEREK");
            if (rs.next() && rs.getInt(1) == 0) {
                String migrateSql = "INSERT IGNORE INTO MEREK (nama_merek) " +
                        "SELECT DISTINCT merek FROM KAOS_MASTER WHERE merek IS NOT NULL AND merek != ''";
                int rows = stmt.executeUpdate(migrateSql);
                System.out.println("✅ Migrated " + rows + " brands to MEREK table");
            }

            System.out.println("✅ Tabel KAOS_MASTER, KAOS_VARIAN, dan MEREK sudah siap");

        } catch (Exception e) {
            System.out.println("❌ Error membuat tabel: " + e.getMessage());
        }
    }

    // ================= FORMAT RUPIAH =================
    private void formatRupiah(JTextField field) {
        field.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                String text = field.getText().replaceAll("[^0-9]", "");
                if (text.isEmpty()) {
                    field.setText("");
                    return;
                }
                try {
                    long value = Long.parseLong(text);
                    field.setText(df.format(value));
                } catch (Exception ex) {
                    field.setText("");
                }
            }
        });
    }

    private long parseRupiah(String text) {
        if (text == null || text.isEmpty())
            return 0;
        text = text.replaceAll("[^0-9]", "");
        if (text.isEmpty())
            return 0;
        return Long.parseLong(text);
    }

    // ================= DATABASE CONNECTION =================
    private Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/distrozone_db";
        String user = "root";
        String password = "";
        return DriverManager.getConnection(url, user, password);
    }

    private void testDatabaseConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("✅ Database connected successfully");
        } catch (Exception e) {
            System.out.println("❌ Database error: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "❌ Error koneksi database: " + e.getMessage(),
                    "Error Koneksi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================= LOAD DATA KAOS DENGAN VARIAN =================
    private void loadData() {
        model.setRowCount(0);
        selectedId = null;

        System.out.println("🔄 Memuat data kaos dengan varian...");

        try (Connection conn = getConnection()) {
            // Updated to use KAOS_MASTER and KAOS_VARIAN
            String sql = "SELECT k.id, k.nama_kaos, k.merek, kat.nama_kategori, " +
                    "COUNT(kv.id) as jumlah_varian, " +
                    "SUM(kv.stok) as total_stok, " +
                    "AVG(kv.harga) as avg_harga_jual, " +
                    "AVG(kv.harga_pokok) as avg_harga_pokok " +
                    "FROM KAOS_MASTER k " +
                    "LEFT JOIN kategori kat ON k.kategori_id = kat.id " +
                    "LEFT JOIN KAOS_VARIAN kv ON k.id = kv.kaos_master_id " +
                    "GROUP BY k.id " +
                    "ORDER BY k.id ASC"; // Changed to ASC for sequential order

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                int id = rs.getInt("id");
                // KAOS_MASTER doesn't have kode_kaos, we can use ID or maybe skip it in this
                // view
                String kodeKaos = "KM-" + String.format("%04d", id);
                String namaKaos = rs.getString("nama_kaos");
                String merek = rs.getString("merek");
                String kategori = rs.getString("nama_kategori");
                int jumlahVarian = rs.getInt("jumlah_varian");
                int totalStok = rs.getInt("total_stok");
                long avgHargaJual = rs.getLong("avg_harga_jual");
                long avgHargaPokok = rs.getLong("avg_harga_pokok");

                // Handle null values
                if (namaKaos == null)
                    namaKaos = "-";
                if (merek == null)
                    merek = "-";
                if (kategori == null)
                    kategori = "Tanpa Kategori";

                // Ambil foto utama dari master if available, else from varian
                String fotoPath = getFotoUtama(conn, id);
                ImageIcon imgIcon = util.ImageHelper.loadImage(fotoPath, 50, 50);

                String displayAvgHargaJual = (avgHargaJual > 0) ? "Rp " + df.format(avgHargaJual) : "-";
                String displayAvgHargaPokok = (avgHargaPokok > 0) ? "Rp " + df.format(avgHargaPokok) : "-";

                model.addRow(new Object[] {
                        id,
                        kodeKaos,
                        namaKaos,
                        merek,
                        kategori,
                        jumlahVarian,
                        displayAvgHargaJual,
                        displayAvgHargaPokok,
                        totalStok,
                        imgIcon
                });
            }

            System.out.println("✅ Data loaded: " + rowCount + " kaos");

            if (rowCount == 0) {
                System.out.println("⚠️ Tidak ada data kaos di database");
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "❌ Gagal memuat data kaos!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Base path untuk web server (sesuaikan dengan lokasi XAMPP htdocs)
    private static final String WEB_BASE_PATH = "C:/xampp/htdocs/distrozoneweb/";

    private String getFotoUtama(Connection conn, int kaosId) throws SQLException {
        // Check KAOS_MASTER.foto_utama first
        String sqlMaster = "SELECT foto_utama FROM KAOS_MASTER WHERE id = ?";
        PreparedStatement pstMaster = conn.prepareStatement(sqlMaster);
        pstMaster.setInt(1, kaosId);
        ResultSet rsMaster = pstMaster.executeQuery();
        if (rsMaster.next()) {
            String foto = rsMaster.getString("foto_utama");
            if (foto != null && !foto.isEmpty())
                return util.ImageHelper.resolveImagePath(foto);
        }

        // Fallback to KAOS_VARIAN
        String sql = "SELECT foto_varian FROM KAOS_VARIAN WHERE kaos_master_id = ? AND foto_varian IS NOT NULL AND foto_varian != '' LIMIT 1";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, kaosId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return util.ImageHelper.resolveImagePath(rs.getString("foto_varian"));
        }
        return null;
    }

    // IMAGE HELPER METHODS - Moved to util.ImageHelper
    // resolveImagePath removed explicitly.

    /**
     * Copy selected image to Web Server directory to ensure sync.
     * Returns the relative path to be stored in DB (e.g.,
     * assets/uploads/products/xyz.jpg)
     */
    private String copyImageToWeb(String sourcePath) {
        if (sourcePath == null || sourcePath.isEmpty()) {
            return null;
        }

        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            System.err.println("❌ Source file not found: " + sourcePath);
            return sourcePath; // Return original if fail
        }

        try {
            // Target directory: C:/xampp/htdocs/distrozoneweb/assets/uploads/products/
            String targetDirStr = WEB_BASE_PATH + "assets/uploads/products/";
            File targetDir = new File(targetDirStr);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            // Generate unique filename
            String ext = "";
            int i = sourcePath.lastIndexOf('.');
            if (i > 0) {
                ext = sourcePath.substring(i);
            }
            String newFilename = UUID.randomUUID().toString() + ext;
            File targetFile = new File(targetDir, newFilename);

            // Copy file
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✅ Image copied to Web: " + targetFile.getAbsolutePath());

            // Return relative path for DB
            // Web expects: assets/uploads/products/filename.jpg
            return "assets/uploads/products/" + newFilename;

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal copy gambar ke server web: " + e.getMessage());
            return sourcePath; // Fallback to raw path
        }
    }

    // reorderIds method removed per user request

    // ================= CRUD OPERATIONS =================
    private void tambahKaos() {
        JDialog dialog = new JDialog(this, "➕ Tambah Kaos Baru", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Fields
        JTextField tfNama = new JTextField(20);
        JComboBox<String> cbMerek = new JComboBox<>(); // Changed from JTextField
        cbMerek.setEditable(true); // Allow typing new brands or selecting existing
        loadMerekFromDatabase(cbMerek);

        JComboBox<String> cbKategori = new JComboBox<>();
        loadKategoriFromDatabase(cbKategori);

        JComboBox<String> cbTypeKaos = new JComboBox<>(new String[] { "Unisex", "Pria", "Wanita", "Anak" });
        JTextArea taDeskripsi = new JTextArea(3, 20);
        taDeskripsi.setLineWrap(true);
        taDeskripsi.setWrapStyleWord(true);
        JScrollPane spDeskripsi = new JScrollPane(taDeskripsi);

        int row = 0;
        addLabelAndField(panel, gbc, "Nama Kaos*:", tfNama, row++);
        addLabelAndCombo(panel, gbc, "Merek*:", cbMerek, row++); // Changed to Combo
        addLabelAndCombo(panel, gbc, "Kategori:", cbKategori, row++);
        addLabelAndCombo(panel, gbc, "Tipe:", cbTypeKaos, row++);

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Deskripsi:"), gbc);
        gbc.gridx = 1;
        panel.add(spDeskripsi, gbc);
        row++;

        // Foto Upload UI for Tambah Kaos
        JLabel lblFoto = new JLabel("", SwingConstants.CENTER);
        lblFoto.setPreferredSize(new Dimension(100, 100));
        lblFoto.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        final String[] pathFoto = { null };

        JButton btnPilihFoto = new JButton("Pilih Foto");
        styleButton(btnPilihFoto, GREEN_LIGHT);

        btnPilihFoto.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Gambar (*.jpg, *.jpeg, *.png)", "jpg", "jpeg", "png"));
            int res = chooser.showOpenDialog(dialog);
            if (res == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                pathFoto[0] = file.getAbsolutePath();
                ImageIcon icon = new ImageIcon(pathFoto[0]);
                Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                lblFoto.setIcon(new ImageIcon(img));
            }
        });

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Foto Utama:"), gbc);

        gbc.gridx = 1;
        JPanel photoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        photoPanel.add(lblFoto);
        photoPanel.add(btnPilihFoto);
        panel.add(photoPanel, gbc);

        // Button panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton btnSimpan = new JButton("Simpan");
        JButton btnBatal = new JButton("Batal");

        // Style tombol
        styleButton(btnSimpan, GREEN_PRIMARY);
        styleButton(btnBatal, new Color(153, 153, 153));

        btnSimpan.addActionListener(e -> {
            String nama = tfNama.getText().trim();
            String merek = cbMerek.getSelectedItem() != null ? cbMerek.getSelectedItem().toString() : ""; // Get from
                                                                                                          // Combo

            if (nama.isEmpty() || merek.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Nama dan merek harus diisi!");
                return;
            }

            // Ensure brand exists in table if new typed
            simpanMerekBaruJikaBelumAda(merek);

            simpanKaos(dialog, nama, merek, cbKategori.getSelectedItem().toString(),
                    cbTypeKaos.getSelectedItem().toString(), taDeskripsi.getText(), pathFoto[0]);
        });

        btnBatal.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnSimpan);
        btnPanel.add(btnBatal);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void tambahVarianKaos() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "⚠️ Pilih kaos terlebih dahulu!");
            return;
        }

        // Dapatkan info kaos yang dipilih
        String namaKaos = "";
        String kodeKaos = "";
        try (Connection conn = getConnection()) {
            String sql = "SELECT nama_kaos FROM KAOS_MASTER WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, selectedId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                namaKaos = rs.getString("nama_kaos");
                kodeKaos = "KM-" + String.format("%04d", selectedId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        JDialog dialog = new JDialog(this, "➕ Tambah Varian Kaos: " + namaKaos, true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 550);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Info kaos
        JLabel lblInfo = new JLabel("<html><b>Kaos:</b> " + namaKaos + " (" + kodeKaos + ")</html>");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Fields untuk varian
        JComboBox<String> cbType = new JComboBox<>(typeOptions);
        JComboBox<String> cbWarna = new JComboBox<>(warnaOptions);
        cbWarna.setEditable(true);

        JComboBox<String> cbSize = new JComboBox<>(sizeOptions);

        JTextField tfHarga = new JTextField();
        JTextField tfHargaPokok = new JTextField();
        JTextField tfStok = new JTextField("0");

        formatRupiah(tfHarga);
        formatRupiah(tfHargaPokok);

        // Preview gambar
        JLabel lblFoto = new JLabel("", SwingConstants.CENTER);
        lblFoto.setPreferredSize(new Dimension(150, 150));
        lblFoto.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        final String[] pathFoto = { null };

        JButton btnPilihFoto = new JButton("Pilih Foto");
        styleButton(btnPilihFoto, GREEN_LIGHT);

        btnPilihFoto.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Gambar (*.jpg, *.jpeg, *.png)", "jpg", "jpeg", "png"));
            int res = chooser.showOpenDialog(dialog);
            if (res == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                pathFoto[0] = file.getAbsolutePath();
                ImageIcon icon = new ImageIcon(pathFoto[0]);
                Image img = icon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                lblFoto.setIcon(new ImageIcon(img));
            }
        });

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        panel.add(lblInfo, gbc);

        gbc.gridwidth = 1;
        addLabelAndCombo(panel, gbc, "Type*:", cbType, row++);
        addLabelAndCombo(panel, gbc, "Warna*:", cbWarna, row++);
        addLabelAndCombo(panel, gbc, "Size*:", cbSize, row++);
        addLabelAndField(panel, gbc, "Harga Jual*:", tfHarga, row++);
        addLabelAndField(panel, gbc, "Harga Pokok*:", tfHargaPokok, row++);
        addLabelAndField(panel, gbc, "Stok*:", tfStok, row++);

        // Foto
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Foto:"), gbc);

        gbc.gridy = row++;
        panel.add(lblFoto, gbc);

        gbc.gridy = row++;
        panel.add(btnPilihFoto, gbc);

        // Button panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton btnSimpan = new JButton("Simpan Varian");
        JButton btnBatal = new JButton("Batal");

        styleButton(btnSimpan, GREEN_PRIMARY);
        styleButton(btnBatal, new Color(153, 153, 153));

        btnSimpan.addActionListener(e -> {
            if (validateVarianInput(tfHarga, tfHargaPokok, tfStok)) {
                simpanVarian(dialog, false,
                        cbType.getSelectedItem().toString(),
                        cbWarna.getSelectedItem().toString(),
                        cbSize.getSelectedItem().toString(),
                        tfHarga.getText(),
                        tfHargaPokok.getText(),
                        tfStok.getText(),
                        pathFoto[0]);
            }
        });

        btnBatal.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnSimpan);
        btnPanel.add(btnBatal);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void kelolaVarianKaos() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "⚠️ Pilih kaos terlebih dahulu!");
            return;
        }

        // Dapatkan info kaos
        String namaKaos = "";
        String kodeKaos = "";
        try (Connection conn = getConnection()) {
            String sql = "SELECT nama_kaos FROM KAOS_MASTER WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, selectedId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                namaKaos = rs.getString("nama_kaos");
                kodeKaos = "KM-" + String.format("%04d", selectedId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        JDialog dialog = new JDialog(this, "📋 Warna & Ukuran: " + namaKaos, true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);

        // Model tabel varian
        DefaultTableModel varianModel = new DefaultTableModel(
                new String[] { "ID", "Type", "Warna", "Size", "Harga Jual", "Harga Pokok", "Stok", "Foto" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1 || column == 6; // Type (1) and Stok (6) editable
            }
        };

        JTable tableVarian = new JTable(varianModel);
        tableVarian.setRowHeight(40);

        // Hide ID Column
        tableVarian.getColumnModel().getColumn(0).setMinWidth(0);
        tableVarian.getColumnModel().getColumn(0).setMaxWidth(0);
        tableVarian.getColumnModel().getColumn(0).setPreferredWidth(0);

        // Add ComboBox for Type Editor
        JComboBox<String> typeEditor = new JComboBox<>(typeOptions);
        tableVarian.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(typeEditor));

        // Load data varian
        loadVarianData(selectedId, varianModel);

        // Renderer untuk harga
        DefaultTableCellRenderer rupiahRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof Number) {
                    value = "Rp " + df.format(((Number) value).longValue());
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        };

        tableVarian.getColumnModel().getColumn(4).setCellRenderer(rupiahRenderer);
        tableVarian.getColumnModel().getColumn(5).setCellRenderer(rupiahRenderer);

        // Renderer untuk kolom Foto Varian (Index 7)
        tableVarian.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = new JLabel();
                if (value instanceof ImageIcon) {
                    label.setIcon((ImageIcon) value);
                    label.setHorizontalAlignment(JLabel.CENTER);
                } else {
                    label.setText((value != null) ? value.toString() : "");
                }
                if (isSelected) {
                    label.setBackground(table.getSelectionBackground());
                    label.setForeground(table.getSelectionForeground());
                } else {
                    label.setBackground(table.getBackground());
                    label.setForeground(table.getForeground());
                }
                label.setOpaque(true);
                return label;
            }
        });

        // Editor untuk kolom stok
        tableVarian.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(new JTextField()));

        // Panel header
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel lblHeader = new JLabel("<html><b>Kaos:</b> " + namaKaos + " (" + kodeKaos + ")</html>");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerPanel.add(lblHeader, BorderLayout.WEST);

        // Panel tombol
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton btnUpdateAll = new JButton("💾 Update Semua");
        JButton btnTambah = new JButton("➕ Tambah Varian");
        JButton btnEditFoto = new JButton("📷 Edit Foto");
        JButton btnHapus = new JButton("🗑️ Hapus Terpilih");
        JButton btnClose = new JButton("✕ Tutup");

        styleButton(btnUpdateAll, GREEN_PRIMARY);
        styleButton(btnTambah, new Color(52, 152, 219));
        styleButton(btnEditFoto, new Color(255, 152, 0)); // Orange color
        styleButton(btnHapus, new Color(231, 76, 60));
        styleButton(btnClose, new Color(153, 153, 153));

        btnUpdateAll.addActionListener(e -> updateAllVarian(tableVarian, varianModel, dialog));
        btnTambah.addActionListener(e -> {
            dialog.dispose();
            tambahVarianKaos();
        });

        // Action untuk Edit Foto varian
        btnEditFoto.addActionListener(e -> {
            int selectedRow = tableVarian.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog, "⚠️ Pilih varian yang akan diedit fotonya!");
                return;
            }
            int modelRow = tableVarian.convertRowIndexToModel(selectedRow);
            int varianId = (Integer) varianModel.getValueAt(modelRow, 0);
            String warna = varianModel.getValueAt(modelRow, 2).toString();
            String size = varianModel.getValueAt(modelRow, 3).toString();

            editFotoVarian(dialog, varianId, warna, size, () -> loadVarianData(selectedId, varianModel));
        });

        btnHapus.addActionListener(e -> hapusVarianTerpilih(tableVarian, varianModel, selectedId));
        btnClose.addActionListener(e -> dialog.dispose());

        // Double click untuk preview foto
        tableVarian.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tableVarian.rowAtPoint(e.getPoint());
                    int col = tableVarian.columnAtPoint(e.getPoint());

                    // Kolom foto adalah index 7
                    if (col == 7 && row >= 0) {
                        int modelRow = tableVarian.convertRowIndexToModel(row);
                        int varianId = (Integer) varianModel.getValueAt(modelRow, 0);
                        showFotoPreview(varianId);
                    }
                }
            }
        });

        btnPanel.add(btnUpdateAll);
        btnPanel.add(btnTambah);
        btnPanel.add(btnEditFoto);
        btnPanel.add(btnHapus);
        btnPanel.add(btnClose);

        dialog.add(headerPanel, BorderLayout.NORTH);
        dialog.add(new JScrollPane(tableVarian), BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // Method untuk edit foto varian
    private void editFotoVarian(JDialog parentDialog, int varianId, String warna, String size, Runnable onSuccess) {
        JDialog dialog = new JDialog(parentDialog, "📷 Edit Foto Varian: " + warna + " - " + size, true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(parentDialog);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Preview foto saat ini
        JLabel lblPreview = new JLabel("", SwingConstants.CENTER);
        lblPreview.setPreferredSize(new Dimension(200, 200));
        lblPreview.setBorder(BorderFactory.createTitledBorder("Foto Saat Ini"));

        final String[] newFotoPath = { null };

        // Load foto yang ada
        try (Connection conn = getConnection()) {
            String sql = "SELECT foto_varian FROM KAOS_VARIAN WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, varianId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String fotoPath = rs.getString("foto_varian");
                if (fotoPath != null && !fotoPath.isEmpty()) {
                    String absPath = util.ImageHelper.resolveImagePath(fotoPath);
                    if (new File(absPath).exists()) {
                        ImageIcon icon = new ImageIcon(absPath);
                        Image img = icon.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
                        lblPreview.setIcon(new ImageIcon(img));
                        newFotoPath[0] = absPath;
                    } else {
                        lblPreview.setText("Gambar tidak ditemukan");
                    }
                } else {
                    lblPreview.setText("Tidak ada foto");
                }
            }
        } catch (Exception ex) {
            lblPreview.setText("Error memuat foto");
            ex.printStackTrace();
        }

        // Tombol pilih foto baru
        JPanel btnSelectPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton btnPilih = new JButton("📁 Pilih Foto Baru");
        JButton btnHapusFoto = new JButton("🗑️ Hapus Foto");

        styleButton(btnPilih, new Color(52, 152, 219));
        styleButton(btnHapusFoto, new Color(231, 76, 60));

        btnPilih.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Gambar (*.jpg, *.jpeg, *.png)", "jpg", "jpeg", "png"));
            int res = chooser.showOpenDialog(dialog);
            if (res == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                newFotoPath[0] = file.getAbsolutePath();
                ImageIcon icon = new ImageIcon(newFotoPath[0]);
                Image img = icon.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
                lblPreview.setIcon(new ImageIcon(img));
                lblPreview.setText("");
            }
        });

        btnHapusFoto.addActionListener(e -> {
            newFotoPath[0] = "";
            lblPreview.setIcon(null);
            lblPreview.setText("Foto akan dihapus");
        });

        btnSelectPanel.add(btnPilih);
        btnSelectPanel.add(btnHapusFoto);

        // Tombol simpan dan batal
        JPanel btnActionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton btnSimpan = new JButton("💾 Simpan");
        JButton btnBatal = new JButton("✕ Batal");

        styleButton(btnSimpan, GREEN_PRIMARY);
        styleButton(btnBatal, new Color(153, 153, 153));

        btnSimpan.addActionListener(e -> {
            try (Connection conn = getConnection()) {
                String sql = "UPDATE KAOS_VARIAN SET foto_varian = ? WHERE id = ?";
                PreparedStatement pst = conn.prepareStatement(sql);
                String dbFotoPath = null;
                if (newFotoPath[0] != null && !newFotoPath[0].isEmpty()) {
                    dbFotoPath = copyImageToWeb(newFotoPath[0]);
                    pst.setString(1, dbFotoPath);
                } else {
                    pst.setNull(1, java.sql.Types.VARCHAR);
                }
                pst.setInt(2, varianId);
                pst.executeUpdate();

                JOptionPane.showMessageDialog(dialog, "✅ Foto varian berhasil diupdate!");
                dialog.dispose();
                if (onSuccess != null) {
                    onSuccess.run();
                }
                loadData(); // Refresh data utama
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "❌ Gagal mengupdate foto!\n" + ex.getMessage());
            }
        });

        btnBatal.addActionListener(e -> dialog.dispose());

        btnActionPanel.add(btnSimpan);
        btnActionPanel.add(btnBatal);

        mainPanel.add(lblPreview, BorderLayout.CENTER);
        mainPanel.add(btnSelectPanel, BorderLayout.NORTH);

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(btnActionPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // Method untuk preview foto varian
    private void showFotoPreview(int varianId) {
        try (Connection conn = getConnection()) {
            String sql = "SELECT warna, size, foto_varian FROM KAOS_VARIAN WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, varianId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String warna = rs.getString("warna");
                String size = rs.getString("size");
                String fotoPath = rs.getString("foto_varian");

                JDialog previewDialog = new JDialog(this, "📷 Preview Foto: " + warna + " - " + size, true);
                previewDialog.setLayout(new BorderLayout());
                previewDialog.setSize(450, 450);
                previewDialog.setLocationRelativeTo(this);

                JLabel lblImage = new JLabel("", SwingConstants.CENTER);

                if (fotoPath != null && !fotoPath.isEmpty()) {
                    String absPath = util.ImageHelper.resolveImagePath(fotoPath);
                    if (new File(absPath).exists()) {
                        ImageIcon icon = new ImageIcon(absPath);
                        Image img = icon.getImage().getScaledInstance(400, 400, Image.SCALE_SMOOTH);
                        lblImage.setIcon(new ImageIcon(img));
                    } else {
                        lblImage.setText("Gambar tidak ditemukan di:\n" + absPath);
                    }
                } else {
                    lblImage.setText("Tidak ada foto untuk varian ini");
                }

                JButton btnClose = new JButton("Tutup");
                styleButton(btnClose, GREEN_PRIMARY);
                btnClose.addActionListener(e -> previewDialog.dispose());

                JPanel btnPanel = new JPanel();
                btnPanel.add(btnClose);

                previewDialog.add(lblImage, BorderLayout.CENTER);
                previewDialog.add(btnPanel, BorderLayout.SOUTH);
                previewDialog.setVisible(true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "❌ Gagal memuat preview foto!");
        }
    }

    private void loadVarianData(int kaosId, DefaultTableModel varianModel) {
        varianModel.setRowCount(0);
        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM KAOS_VARIAN WHERE kaos_master_id = ? ORDER BY warna, size";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, kaosId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                String photoPath = rs.getString("foto_varian");
                ImageIcon photoIcon = null;
                if (photoPath != null && !photoPath.isEmpty()) {
                    try {
                        // Use tiny icon for table
                        String absPath = util.ImageHelper.resolveImagePath(photoPath);
                        if (new File(absPath).exists()) {
                            Image img = new ImageIcon(absPath).getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                            photoIcon = new ImageIcon(img);
                        }
                    } catch (Exception ex) {
                        System.err.println("Gagal load icon varian: " + ex.getMessage());
                    }
                }

                // Fallback default icon if null
                if (photoIcon == null) {
                    // Create a simple colored placeholder based on HEX
                    String hex = rs.getString("warna_hex");
                    Color placeholderColor = Color.LIGHT_GRAY;
                    if (hex != null && !hex.isEmpty()) {
                        try {
                            placeholderColor = Color.decode(hex);
                        } catch (Exception e) {
                        }
                    }

                    BufferedImage img = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = img.createGraphics();
                    g2.setColor(placeholderColor);
                    g2.fillRect(0, 0, 30, 30);
                    g2.setColor(Color.GRAY);
                    g2.drawRect(0, 0, 29, 29);
                    g2.dispose();
                    photoIcon = new ImageIcon(img);
                }

                String typeStr = rs.getString("type_varian");
                if (typeStr == null)
                    typeStr = "-";

                varianModel.addRow(new Object[] {
                        rs.getInt("id"),
                        typeStr,
                        rs.getString("warna"),
                        rs.getString("size"),
                        rs.getLong("harga"),
                        rs.getLong("harga_pokok"),
                        rs.getInt("stok"),
                        photoIcon
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAllVarian(JTable table, DefaultTableModel model, JDialog dialog) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            for (int i = 0; i < model.getRowCount(); i++) {
                int varianId = (Integer) model.getValueAt(i, 0);
                String typeVariant = model.getValueAt(i, 1).toString();
                int stokBaru = Integer.parseInt(model.getValueAt(i, 6).toString());

                String sql = "UPDATE KAOS_VARIAN SET stok = ?, type_varian = ? WHERE id = ?";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setInt(1, stokBaru);
                pst.setString(2, typeVariant);
                pst.setInt(3, varianId);
                pst.executeUpdate();
            }

            conn.commit();
            JOptionPane.showMessageDialog(dialog, "✅ Semua data (Stok & Type) berhasil diupdate!");
            loadData(); // Refresh data utama
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialog, "❌ Error: " + e.getMessage());
        }
    }

    private void hapusVarianTerpilih(JTable table, DefaultTableModel model, int currentKaosId) {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "⚠️ Pilih varian yang akan dihapus!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Hapus " + selectedRows.length + " varian terpilih?",
                "Konfirmasi Hapus",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION)
            return;

        try (Connection conn = getConnection()) {
            for (int viewRow : selectedRows) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                int varianId = (Integer) model.getValueAt(modelRow, 0);

                String sql = "DELETE FROM KAOS_VARIAN WHERE id = ?";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setInt(1, varianId);
                pst.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "✅ Varian berhasil dihapus!");
            loadVarianData(currentKaosId, model);
            loadData(); // Refresh data utama
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "❌ Error: " + e.getMessage());
        }
    }

    private void simpanVarian(JDialog dialog, boolean tambahLagi,
            String type, String warna, String size,
            String hargaStr, String hargaPokokStr,
            String stokStr, String fotoPath) {
        try (Connection conn = getConnection()) {
            long harga = parseRupiah(hargaStr);
            long hargaPokok = parseRupiah(hargaPokokStr);
            int stok = Integer.parseInt(stokStr);
            String warnaHex = getColorHex(warna);

            // Cek apakah varian sudah ada
            String checkSql = "SELECT id, stok FROM KAOS_VARIAN " +
                    "WHERE kaos_master_id = ? AND warna = ? AND size = ?";
            PreparedStatement checkPst = conn.prepareStatement(checkSql);
            checkPst.setInt(1, selectedId);
            checkPst.setString(2, warna);
            checkPst.setString(3, size);
            ResultSet rs = checkPst.executeQuery();

            if (rs.next()) {
                // Update jika sudah ada
                int varianId = rs.getInt("id");
                int stokLama = rs.getInt("stok");

                String updateSql = "UPDATE KAOS_VARIAN SET harga = ?, harga_pokok = ?, " +
                        "stok = stok + ?, foto_varian = COALESCE(?, foto_varian), warna_hex = ? WHERE id = ?";
                PreparedStatement updatePst = conn.prepareStatement(updateSql);
                updatePst.setLong(1, harga);
                updatePst.setLong(2, hargaPokok);
                updatePst.setInt(3, stok);
                updatePst.setString(4, fotoPath);
                updatePst.setString(5, warnaHex);
                updatePst.setInt(6, varianId);
                updatePst.executeUpdate();

                JOptionPane.showMessageDialog(dialog,
                        "✅ Stok varian berhasil ditambahkan!\n" +
                                "Stok baru: " + (stokLama + stok));
            } else {
                // Insert baru
                String kodeVarian = "KV-" + System.currentTimeMillis() / 1000; // Simple generation

                // Handle Image Copy
                String dbFotoPath = copyImageToWeb(fotoPath);

                String insertSql = "INSERT INTO KAOS_VARIAN (kaos_master_id, kode_varian, warna, size, " +
                        "harga, harga_pokok, stok, foto_varian, type_varian, warna_hex) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement insertPst = conn.prepareStatement(insertSql);
                insertPst.setInt(1, selectedId);
                insertPst.setString(2, kodeVarian);
                insertPst.setString(3, warna);
                insertPst.setString(4, size);
                insertPst.setLong(5, harga);
                insertPst.setLong(6, hargaPokok);
                insertPst.setInt(7, stok);
                insertPst.setString(8, dbFotoPath); // Save relative path
                insertPst.setString(9, type);
                insertPst.setString(10, warnaHex);
                insertPst.executeUpdate();

                JOptionPane.showMessageDialog(dialog,
                        "✅ Varian baru berhasil ditambahkan!\n" +
                                warna + " - " + size);
            }

            if (!tambahLagi) {
                dialog.dispose();
            }
            loadData(); // Refresh data utama

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialog,
                    "❌ Gagal menyimpan varian!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateVarianInput(JTextField tfHarga, JTextField tfHargaPokok, JTextField tfStok) {
        try {
            long harga = parseRupiah(tfHarga.getText());
            long hargaPokok = parseRupiah(tfHargaPokok.getText());
            int stok = Integer.parseInt(tfStok.getText());

            if (harga <= 0 || hargaPokok <= 0) {
                JOptionPane.showMessageDialog(this, "Harga harus lebih dari 0!");
                return false;
            }

            if (stok < 0) {
                JOptionPane.showMessageDialog(this, "Stok tidak boleh negatif!");
                return false;
            }

            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Input tidak valid!");
            return false;
        }
    }

    private void editKaos() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "⚠️ Pilih kaos yang akan diedit!");
            return;
        }

        try (Connection conn = getConnection()) {
            String sql = "SELECT k.id, k.nama_kaos, k.merek, k.foto_utama, k.type_kaos, k.deskripsi, kat.nama_kategori FROM KAOS_MASTER k "
                    +
                    "LEFT JOIN kategori kat ON k.kategori_id = kat.id " +
                    "WHERE k.id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, selectedId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                showEditDialog(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "❌ Gagal memuat data kaos!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showEditDialog(ResultSet rs) throws SQLException {
        // Need to add foto_utama to SELECT in editKaos calling method first!
        // editKaos queries: "SELECT k.id, k.nama_kaos, k.merek, kat.nama_kategori FROM
        // KAOS_MASTER k ..."
        // I need to update editKaos query too.
        JDialog dialog = new JDialog(this, "✏️ Edit Kaos", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        // Fields
        // No kode_kaos in Master

        JTextField tfNama = new JTextField(rs.getString("nama_kaos"), 20);
        // Replaced JTextField tfMerek with JComboBox cbMerek
        JComboBox<String> cbMerek = new JComboBox<>();
        cbMerek.setEditable(true);
        loadMerekFromDatabase(cbMerek);
        cbMerek.setSelectedItem(rs.getString("merek"));

        JComboBox<String> cbKategori = new JComboBox<>();
        loadKategoriFromDatabase(cbKategori);
        cbKategori.setSelectedItem(rs.getString("nama_kategori"));

        JComboBox<String> cbTypeKaos = new JComboBox<>(new String[] { "Unisex", "Pria", "Wanita", "Anak" });
        String currentType = rs.getString("type_kaos");
        if (currentType != null)
            cbTypeKaos.setSelectedItem(currentType);

        JTextArea taDeskripsi = new JTextArea(rs.getString("deskripsi"), 3, 20);
        taDeskripsi.setLineWrap(true);
        taDeskripsi.setWrapStyleWord(true);
        JScrollPane spDeskripsi = new JScrollPane(taDeskripsi);

        int row = 0;
        // addLabelAndField(panel, gbc, "Kode Kaos:", tfKode, row++);
        addLabelAndField(panel, gbc, "Nama Kaos*:", tfNama, row++);
        addLabelAndCombo(panel, gbc, "Merek*:", cbMerek, row++); // Changed to Combo
        addLabelAndCombo(panel, gbc, "Kategori:", cbKategori, row++);
        addLabelAndCombo(panel, gbc, "Tipe:", cbTypeKaos, row++);

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Deskripsi:"), gbc);
        gbc.gridx = 1;
        panel.add(spDeskripsi, gbc);
        row++;

        // Foto Upload UI for Edit Kaos
        JLabel lblFoto = new JLabel("", SwingConstants.CENTER);
        lblFoto.setPreferredSize(new Dimension(100, 100));
        lblFoto.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        final String[] pathFoto = { null };

        // Load existing photo
        try {
            String currentFoto = rs.getString("foto_utama");
            if (currentFoto != null && !currentFoto.isEmpty()) {
                pathFoto[0] = currentFoto; // Keep original path (relative) for logic
                // Display using resolved path
                ImageIcon icon = util.ImageHelper.loadImage(currentFoto, 100, 100);
                lblFoto.setIcon(icon);
            } else {
                lblFoto.setText("No Image");
            }
        } catch (SQLException ex) {
        }

        JButton btnPilihFoto = new JButton("Ganti Foto");
        styleButton(btnPilihFoto, GREEN_LIGHT);

        btnPilihFoto.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Gambar (*.jpg, *.jpeg, *.png)", "jpg", "jpeg", "png"));
            int res = chooser.showOpenDialog(dialog);
            if (res == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                pathFoto[0] = file.getAbsolutePath();
                ImageIcon icon = new ImageIcon(pathFoto[0]);
                Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                lblFoto.setIcon(new ImageIcon(img));
            }
        });

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Foto Utama:"), gbc);

        gbc.gridx = 1;
        JPanel photoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        photoPanel.add(lblFoto);
        photoPanel.add(btnPilihFoto);
        panel.add(photoPanel, gbc);

        // Panel tombol
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton btnUpdate = new JButton("Update");
        JButton btnBatal = new JButton("Batal");

        styleButton(btnUpdate, GREEN_PRIMARY);
        styleButton(btnBatal, new Color(153, 153, 153));

        btnUpdate.addActionListener(e -> {
            String nama = tfNama.getText().trim();
            String merek = cbMerek.getSelectedItem() != null ? cbMerek.getSelectedItem().toString() : "";

            if (nama.isEmpty() || merek.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Nama dan merek harus diisi!");
                return;
            }
            // Ensure brand exists in table if new typed
            simpanMerekBaruJikaBelumAda(merek);

            updateKaos(dialog, selectedId, nama, merek, cbKategori.getSelectedItem().toString(),
                    cbTypeKaos.getSelectedItem().toString(), taDeskripsi.getText(), pathFoto[0]);
        });

        btnBatal.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnUpdate);
        btnPanel.add(btnBatal);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showDetailKaos() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "⚠️ Pilih kaos untuk melihat detail!");
            return;
        }

        try (Connection conn = getConnection()) {
            String sql = "SELECT k.id, k.nama_kaos, k.merek, k.foto_utama, kat.nama_kategori FROM KAOS_MASTER k " +
                    "LEFT JOIN kategori kat ON k.kategori_id = kat.id " +
                    "WHERE k.id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, selectedId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                JDialog dialog = new JDialog(this, "📋 Detail Kaos Master", true);
                dialog.setLayout(new BorderLayout());
                dialog.setSize(700, 650);
                dialog.setLocationRelativeTo(this);

                JPanel panel = new JPanel(new BorderLayout(10, 10));
                panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                // Panel gambar kaos di sebelah kiri
                JPanel imagePanel = new JPanel(new BorderLayout());
                imagePanel.setPreferredSize(new Dimension(200, 200));
                imagePanel.setBorder(BorderFactory.createTitledBorder("Foto Kaos"));

                String fotoPath = rs.getString("foto_utama");
                JLabel lblImage = new JLabel();
                lblImage.setHorizontalAlignment(JLabel.CENTER);

                if (fotoPath != null && !fotoPath.isEmpty()) {
                    String resolvedPath = util.ImageHelper.resolveImagePath(fotoPath);
                    File imgFile = new File(resolvedPath);
                    if (imgFile.exists()) {
                        ImageIcon icon = new ImageIcon(resolvedPath);
                        Image scaled = icon.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
                        lblImage.setIcon(new ImageIcon(scaled));
                    } else {
                        lblImage.setText("Gambar tidak ditemukan");
                    }
                } else {
                    lblImage.setText("Tidak ada gambar");
                }
                imagePanel.add(lblImage, BorderLayout.CENTER);

                // Panel info kaos
                JPanel infoPanel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(3, 3, 3, 3);

                int row = 0;
                addDetailRow(infoPanel, gbc, "ID Master:", String.valueOf(rs.getInt("id")), row++);
                addDetailRow(infoPanel, gbc, "Nama Kaos:", rs.getString("nama_kaos"), row++);
                addDetailRow(infoPanel, gbc, "Merek:", rs.getString("merek"), row++);
                addDetailRow(infoPanel, gbc, "Kategori:", rs.getString("nama_kategori"), row++);

                // Tabel varian
                DefaultTableModel detailModel = new DefaultTableModel(
                        new String[] { "Kode Varian", "Warna", "Size", "Harga Jual", "Harga Pokok", "Stok" }, 0);

                String varianSql = "SELECT kode_varian, warna, size, harga, harga_pokok, stok " +
                        "FROM KAOS_VARIAN WHERE kaos_master_id = ? ORDER BY warna, size";
                PreparedStatement varianPst = conn.prepareStatement(varianSql);
                varianPst.setInt(1, selectedId);
                ResultSet varianRs = varianPst.executeQuery();

                int totalStok = 0;
                while (varianRs.next()) {
                    detailModel.addRow(new Object[] {
                            varianRs.getString("kode_varian"),
                            varianRs.getString("warna"),
                            varianRs.getString("size"),
                            "Rp " + df.format(varianRs.getLong("harga")),
                            "Rp " + df.format(varianRs.getLong("harga_pokok")),
                            varianRs.getInt("stok")
                    });
                    totalStok += varianRs.getInt("stok");
                }

                addDetailRow(infoPanel, gbc, "Total Varian:", String.valueOf(detailModel.getRowCount()), row++);
                addDetailRow(infoPanel, gbc, "Total Stok:", String.valueOf(totalStok), row++);

                JTable tableVarian = new JTable(detailModel);
                tableVarian.setRowHeight(25);

                JScrollPane scrollPane = new JScrollPane(tableVarian);
                scrollPane.setBorder(BorderFactory.createTitledBorder("Daftar Varian"));

                // Gabungkan imagePanel dan infoPanel dalam topPanel
                JPanel topPanel = new JPanel(new BorderLayout(10, 0));
                topPanel.add(imagePanel, BorderLayout.WEST);
                topPanel.add(infoPanel, BorderLayout.CENTER);

                panel.add(topPanel, BorderLayout.NORTH);
                panel.add(scrollPane, BorderLayout.CENTER);

                JButton btnClose = new JButton("Tutup");
                styleButton(btnClose, GREEN_PRIMARY);
                btnClose.addActionListener(e -> dialog.dispose());

                JPanel btnPanel = new JPanel();
                btnPanel.add(btnClose);

                dialog.add(panel, BorderLayout.CENTER);
                dialog.add(btnPanel, BorderLayout.SOUTH);
                dialog.setVisible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "❌ Gagal memuat detail kaos!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void hapusKaos() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "⚠️ Pilih kaos yang akan dihapus!");
            return;
        }

        int row = table.getSelectedRow();
        String namaKaos = model.getValueAt(table.convertRowIndexToModel(row), 2).toString();

        int confirm = JOptionPane.showConfirmDialog(this,
                "<html><b>Hapus Kaos dan Semua Variannya?</b><br><br>" +
                        "Nama: <b>" + namaKaos + "</b><br><br>" +
                        "Semua varian akan ikut terhapus!<br>" +
                        "Data yang dihapus tidak dapat dikembalikan!",
                "Konfirmasi Hapus",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION)
            return;

        try (Connection conn = getConnection()) {
            // Cek apakah varian dari kaos ini pernah digunakan dalam transaksi
            String checkSql = "SELECT COUNT(*) as count FROM DETAIL_TRANSAKSI dt " +
                    "JOIN KAOS_VARIAN kv ON dt.kaos_id = kv.id " +
                    "WHERE kv.kaos_master_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, selectedId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt("count") > 0) {
                int countTrx = rs.getInt("count");
                int confirmForce = JOptionPane.showConfirmDialog(this,
                        "<html>⚠️ <b>PERINGATAN TRANSAKSI TERKAIT!</b><br><br>" +
                                "Produk ini terdeteksi dalam <b>" + countTrx + "</b> baris transaksi.<br>" +
                                "Untuk menghapus produk ini, <b>SEMUA TRANSAKSI</b> yang memuat produk ini<br>" +
                                "akan ikut <b>DIHAPUS SECARA PERMANEN</b> untuk menjaga integritas data.<br><br>" +
                                "Apakah Anda yakin ingin menghapus '" + namaKaos + "' DAN riwayat transaksinya?</html>",
                        "Konfirmasi Hapus Data Test",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.ERROR_MESSAGE);

                if (confirmForce != JOptionPane.YES_OPTION) {
                    return;
                }

                // Logika Hapus Transaksi Terkait (FORCE DELETE)
                // 1. Ambil ID Transaksi yang terlibat
                String getTrxSql = "SELECT DISTINCT dt.transaksi_id FROM DETAIL_TRANSAKSI dt " +
                        "JOIN KAOS_VARIAN kv ON dt.kaos_id = kv.id " +
                        "WHERE kv.kaos_master_id = ?";
                PreparedStatement pstGet = conn.prepareStatement(getTrxSql);
                pstGet.setInt(1, selectedId);
                ResultSet rsTrx = pstGet.executeQuery();
                java.util.List<Integer> trxIds = new java.util.ArrayList<>();
                while (rsTrx.next()) {
                    trxIds.add(rsTrx.getInt("transaksi_id"));
                }

                if (!trxIds.isEmpty()) {
                    System.out.println("🗑️ Menghapus " + trxIds.size() + " transaksi terkait...");

                    // Buat string IN clause (?,?,?)
                    StringBuilder inClause = new StringBuilder();
                    for (int i = 0; i < trxIds.size(); i++) {
                        inClause.append("?");
                        if (i < trxIds.size() - 1)
                            inClause.append(",");
                    }

                    // 2. Hapus DETAIL_TRANSAKSI (Semua detail dari transaksi tersebut, bukan cuma
                    // item ini,
                    // asumsi: hapus transaksi total)
                    // Hapus Detail berdasarkan transaksi_id (bersih total)
                    String delDetail = "DELETE FROM DETAIL_TRANSAKSI WHERE transaksi_id IN (" + inClause + ")";
                    PreparedStatement pstDelDet = conn.prepareStatement(delDetail);
                    for (int i = 0; i < trxIds.size(); i++)
                        pstDelDet.setInt(i + 1, trxIds.get(i));
                    pstDelDet.executeUpdate();

                    // 3. Hapus TRANSAKSI Header
                    String delHeader = "DELETE FROM TRANSAKSI WHERE id IN (" + inClause + ")";
                    PreparedStatement pstDelHead = conn.prepareStatement(delHeader);
                    for (int i = 0; i < trxIds.size(); i++)
                        pstDelHead.setInt(i + 1, trxIds.get(i));
                    pstDelHead.executeUpdate();

                    System.out.println("✅ Transaksi terkait berhasil dihapus.");
                }
            }

            // Hapus varian
            String deleteVarianSql = "DELETE FROM KAOS_VARIAN WHERE kaos_master_id = ?";
            PreparedStatement deleteVarianStmt = conn.prepareStatement(deleteVarianSql);
            deleteVarianStmt.setInt(1, selectedId);
            deleteVarianStmt.executeUpdate();

            // Hapus dari KAOS_MASTER
            String deleteKaosSql = "DELETE FROM KAOS_MASTER WHERE id = ?";
            PreparedStatement deleteKaosStmt = conn.prepareStatement(deleteKaosSql);
            deleteKaosStmt.setInt(1, selectedId);
            int affectedRows = deleteKaosStmt.executeUpdate();

            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(this, "✅ Kaos dan transaksi terkait berhasil dihapus!");
                loadData();
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "❌ Gagal menghapus kaos!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================= HELPER METHODS =================
    private int getLowestAvailableId(Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT id FROM KAOS_MASTER ORDER BY id ASC");
        int expectedId = 1;
        while (rs.next()) {
            int id = rs.getInt("id");
            if (id != expectedId) {
                return expectedId;
            }
            expectedId++;
        }
        return expectedId;
    }

    private void simpanKaos(JDialog dialog, String nama, String merek, String kategori, String type, String deskripsi,
            String fotoPath) {
        try (Connection conn = getConnection()) {
            int kategoriId = getKategoriIdByName(conn, kategori);
            int newId = getLowestAvailableId(conn);

            // Handle Copy Image
            String dbFotoPath = copyImageToWeb(fotoPath);

            String sql = "INSERT INTO KAOS_MASTER (id, nama_kaos, merek, kategori_id, type_kaos, deskripsi, foto_utama) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, newId);
            pst.setString(2, nama);
            pst.setString(3, merek);
            pst.setInt(4, kategoriId);
            pst.setString(5, type);
            pst.setString(6, deskripsi);
            pst.setString(7, dbFotoPath); // Save relative web path
            pst.executeUpdate();

            JOptionPane.showMessageDialog(dialog,
                    "✅ Kaos Master berhasil ditambahkan!\n" +
                            "Kode: " + util.CodeGenerator.formatKaosMasterCode(newId) + "\n" +
                            "Nama: " + nama + "\n" +
                            "Silakan tambah varian (warna & size) untuk kaos ini.");

            dialog.dispose();
            loadData();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialog,
                    "❌ Gagal menambah kaos!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateKaos(JDialog dialog, int id, String nama, String merek, String kategori, String type,
            String deskripsi, String fotoPath) {
        try (Connection conn = getConnection()) {
            int kategoriId = getKategoriIdByName(conn, kategori);

            String sql = "UPDATE KAOS_MASTER SET nama_kaos=?, merek=?, kategori_id=?, type_kaos=?, deskripsi=?, foto_utama=COALESCE(?, foto_utama) WHERE id=?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, nama);
            pst.setString(2, merek);
            pst.setInt(3, kategoriId);
            pst.setString(4, type);
            pst.setString(5, deskripsi);

            // Handle Image Copy if Changed
            String dbFotoPath = null;
            if (fotoPath != null && !fotoPath.isEmpty()) {
                File f = new File(fotoPath);
                // Only copy if it's an absolute path (newly selected), otherwise (relative path
                // from DB) keep as is
                if (f.isAbsolute()) {
                    dbFotoPath = copyImageToWeb(fotoPath);
                } else {
                    dbFotoPath = fotoPath; // Keep existing relative path
                }
            }

            pst.setString(6, dbFotoPath);
            pst.setInt(7, id);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(dialog, "✅ Kaos Master berhasil diupdate!");
            dialog.dispose();
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialog,
                    "❌ Gagal update kaos!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addLabelAndField(JPanel panel, GridBagConstraints gbc,
            String label, JTextField field, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
        gbc.weightx = 0;
    }

    private void addLabelAndCombo(JPanel panel, GridBagConstraints gbc,
            String label, JComboBox<String> combo, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 4, 4, 4);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(combo, gbc);
        gbc.weightx = 0;
    }

    private void addDetailRow(JPanel panel, GridBagConstraints gbc,
            String label, String value, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        JLabel lbl = new JLabel("<html><b>" + label + "</b></html>");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(val, gbc);
    }

    private void styleButton(JButton btn, Color bgColor) {
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Remove fixed size to allow text to fit
        // btn.setPreferredSize(new Dimension(100, 35));
        btn.setMargin(new Insets(5, 15, 5, 15)); // Add padding instead
        if (btn.getPreferredSize().width < 100) {
            btn.setPreferredSize(new Dimension(100, 35)); // Min width
        } else {
            btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 35)); // Height fixed
        }

        // Robust painting
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);

        // Custom paint for consistency (prevents white glitch)
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
    }

    private JButton createIconButton(String iconName, String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        // Robust painting
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setBackground(bgColor);

        // Coba load icon
        ImageIcon icon = createImageIcon(iconName, 16, 16);
        if (icon != null) {
            btn.setIcon(icon);
            btn.setHorizontalTextPosition(SwingConstants.RIGHT);
            btn.setIconTextGap(8);
        }

        // Custom paint for consistency (prevents white glitch)
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

    private void loadKategoriFromDatabase(JComboBox<String> combo) {
        combo.removeAllItems();
        try (Connection conn = getConnection()) {
            String sql = "SELECT nama_kategori FROM kategori ORDER BY nama_kategori ASC";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                combo.addItem(rs.getString("nama_kategori"));
            }
            if (combo.getItemCount() == 0) {
                combo.addItem("Unisex");
            }
        } catch (Exception e) {
            e.printStackTrace();
            combo.addItem("Unisex");
        }
    }

    private int getKategoriIdByName(Connection conn, String kategoriName) throws SQLException {
        String sql = "SELECT id FROM kategori WHERE nama_kategori = ?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, kategoriName);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            return rs.getInt("id");
        } else {
            String insertSql = "INSERT INTO kategori (nama_kategori) VALUES (?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            insertStmt.setString(1, kategoriName);
            insertStmt.executeUpdate();

            ResultSet generatedKeys = insertStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
        }
        return 1;
    }

    private void showKategoriDialog() {
        JDialog dialog = new JDialog(this, "Kelola Kategori", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 500);
        dialog.setLocationRelativeTo(this);

        // Panel utama
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // === PANEL INPUT ===
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        JLabel lblKategori = new JLabel("Nama Kategori:");
        lblKategori.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JTextField tfKategori = new JTextField();
        tfKategori.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tfKategori.setPreferredSize(new Dimension(250, 35));

        JButton btnTambahKategori = new JButton("Tambah");
        styleButton(btnTambahKategori, GREEN_PRIMARY);

        // Layout untuk input panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        inputPanel.add(lblKategori, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        inputPanel.add(tfKategori, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        inputPanel.add(btnTambahKategori, gbc);

        // === TABEL KATEGORI ===
        DefaultTableModel kategoriModel = new DefaultTableModel(
                new String[] { "ID", "Nama Kategori", "Jumlah Kaos" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 2)
                    return Integer.class;
                return String.class;
            }
        };

        JTable tableKategori = new JTable(kategoriModel);
        tableKategori.setRowHeight(30);
        tableKategori.setSelectionBackground(GREEN_LIGHT);
        tableKategori.setSelectionForeground(TEXT_DARK);
        tableKategori.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableKategori.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Sembunyikan kolom ID
        tableKategori.getColumnModel().getColumn(0).setMinWidth(0);
        tableKategori.getColumnModel().getColumn(0).setMaxWidth(0);

        // Set lebar kolom
        tableKategori.getColumnModel().getColumn(1).setPreferredWidth(200);
        tableKategori.getColumnModel().getColumn(2).setPreferredWidth(80);

        // Center untuk kolom jumlah
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tableKategori.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(tableKategori);

        // === PANEL TOMBOL BAWAH ===
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonPanel.setOpaque(false);

        JButton btnEdit = new JButton("Edit");
        JButton btnHapus = new JButton("Hapus");
        JButton btnTutup = new JButton("Tutup");

        styleButton(btnEdit, new Color(52, 152, 219));
        styleButton(btnHapus, new Color(231, 76, 60));
        styleButton(btnTutup, new Color(153, 153, 153));

        buttonPanel.add(btnEdit);
        buttonPanel.add(btnHapus);
        buttonPanel.add(btnTutup);

        // === LAYOUT ===
        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);

        // === LOAD DATA ===
        loadKategoriData(kategoriModel);

        // === ACTION LISTENERS ===
        btnTambahKategori.addActionListener(e -> {
            String namaKategori = tfKategori.getText().trim();
            if (namaKategori.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Nama kategori harus diisi!",
                        "Error",
                        JOptionPane.WARNING_MESSAGE);
                tfKategori.requestFocus();
                return;
            }

            if (tambahKategori(namaKategori)) {
                tfKategori.setText("");
                loadKategoriData(kategoriModel);
                loadData();
            }
        });

        // Enter key di textbox untuk tambah
        tfKategori.addActionListener(e -> btnTambahKategori.doClick());

        btnEdit.addActionListener(e -> {
            int selectedRow = tableKategori.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Pilih kategori yang akan diedit!",
                        "Peringatan",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            int kategoriId = (Integer) kategoriModel.getValueAt(selectedRow, 0);
            String namaLama = kategoriModel.getValueAt(selectedRow, 1).toString();

            String namaBaru = JOptionPane.showInputDialog(dialog,
                    "Edit nama kategori:",
                    namaLama);

            if (namaBaru != null && !namaBaru.trim().isEmpty() && !namaBaru.equals(namaLama)) {
                if (editKategori(kategoriId, namaBaru.trim())) {
                    loadKategoriData(kategoriModel);
                    loadData();
                }
            }
        });

        btnHapus.addActionListener(e -> {
            int selectedRow = tableKategori.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Pilih kategori yang akan dihapus!",
                        "Peringatan",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            int kategoriId = (Integer) kategoriModel.getValueAt(selectedRow, 0);
            String namaKategori = kategoriModel.getValueAt(selectedRow, 1).toString();
            int jumlahKaos = (Integer) kategoriModel.getValueAt(selectedRow, 2);

            if (jumlahKaos > 0) {
                JOptionPane.showMessageDialog(dialog,
                        "Kategori tidak dapat dihapus!\n" +
                                "Masih ada " + jumlahKaos + " kaos dalam kategori '" + namaKategori + "'.\n" +
                                "Pindahkan kaos terlebih dahulu ke kategori lain.",
                        "Kategori Terpakai",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Hapus kategori: " + namaKategori + "?",
                    "Konfirmasi Hapus",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                if (hapusKategori(kategoriId)) {
                    loadKategoriData(kategoriModel);
                    loadData();
                }
            }
        });

        btnTutup.addActionListener(e -> dialog.dispose());

        // Double click untuk edit
        tableKategori.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    btnEdit.doClick();
                }
            }
        });

        // Efek hover untuk tombol
        addHoverEffect(btnTambahKategori, GREEN_PRIMARY);
        addHoverEffect(btnEdit, new Color(52, 152, 219));
        addHoverEffect(btnHapus, new Color(231, 76, 60));
        addHoverEffect(btnTutup, new Color(153, 153, 153));

        dialog.setVisible(true);
    }

    private void addHoverEffect(JButton btn, Color baseColor) {
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                btn.setBackground(baseColor.darker());
            }

            public void mouseExited(MouseEvent evt) {
                btn.setBackground(baseColor);
            }
        });
    }

    private void loadKategoriData(DefaultTableModel model) {
        model.setRowCount(0);

        try (Connection conn = getConnection()) {
            String sql = "SELECT k.id, k.nama_kategori, " +
                    "COUNT(ka.id) as jumlah_kaos " +
                    "FROM kategori k " +
                    "LEFT JOIN KAOS_MASTER ka ON k.id = ka.kategori_id " +
                    "GROUP BY k.id, k.nama_kategori " +
                    "ORDER BY k.nama_kategori ASC";

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getString("nama_kategori"),
                        rs.getInt("jumlah_kaos")
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Gagal memuat data kategori!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean tambahKategori(String namaKategori) {
        try (Connection conn = getConnection()) {
            // Cek duplikat
            String checkSql = "SELECT COUNT(*) FROM kategori WHERE LOWER(nama_kategori) = LOWER(?)";
            PreparedStatement checkPst = conn.prepareStatement(checkSql);
            checkPst.setString(1, namaKategori);
            ResultSet rs = checkPst.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this,
                        "Kategori '" + namaKategori + "' sudah ada!",
                        "Duplikat",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }

            String sql = "INSERT INTO kategori (nama_kategori) VALUES (?)";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, namaKategori);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "Kategori '" + namaKategori + "' berhasil ditambahkan!");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Gagal menambah kategori!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean editKategori(int kategoriId, String namaBaru) {
        try (Connection conn = getConnection()) {
            // Cek duplikat (kecuali diri sendiri)
            String checkSql = "SELECT COUNT(*) FROM kategori WHERE LOWER(nama_kategori) = LOWER(?) AND id != ?";
            PreparedStatement checkPst = conn.prepareStatement(checkSql);
            checkPst.setString(1, namaBaru);
            checkPst.setInt(2, kategoriId);
            ResultSet rs = checkPst.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this,
                        "Kategori '" + namaBaru + "' sudah ada!",
                        "Duplikat",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }

            String sql = "UPDATE kategori SET nama_kategori = ? WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, namaBaru);
            pst.setInt(2, kategoriId);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Kategori berhasil diupdate!");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Gagal mengupdate kategori!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean hapusKategori(int kategoriId) {
        try (Connection conn = getConnection()) {
            String sql = "DELETE FROM kategori WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, kategoriId);
            int affected = pst.executeUpdate();

            if (affected > 0) {
                JOptionPane.showMessageDialog(this, "Kategori berhasil dihapus!");
                return true;
            }
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Gagal menghapus kategori!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private ImageIcon createImageIcon(String filename, int width, int height) {
        try {
            // Coba load dari resources
            java.net.URL imgURL = getClass().getResource("/img/" + filename);
            if (imgURL != null) {
                ImageIcon icon = new ImageIcon(imgURL);
                Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }

            // Coba load dari file system
            File file = new File("src/img/" + filename);
            if (file.exists()) {
                ImageIcon icon = new ImageIcon(file.getPath());
                Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }

            // Coba load dari folder img di project root
            file = new File("img/" + filename);
            if (file.exists()) {
                ImageIcon icon = new ImageIcon(file.getPath());
                Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }

            System.out.println("⚠️ Gambar tidak ditemukan: " + filename);
        } catch (Exception e) {
            System.out.println("❌ Gagal load gambar: " + filename + " - " + e.getMessage());
        }
        return null;
    }

    private ImageIcon createBoxIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g2d.setColor(GREEN_PRIMARY);
        g2d.fillRoundRect(0, 0, width, height, 15, 15);

        // Gambar box/kotak
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3f));

        // Kotak utama
        int boxWidth = width * 2 / 3;
        int boxHeight = height * 2 / 3;
        int boxX = (width - boxWidth) / 2;
        int boxY = (height - boxHeight) / 2;

        g2d.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 5, 5);

        // Garis tutup box
        g2d.drawLine(boxX, boxY, boxX - 10, boxY - 10);
        g2d.drawLine(boxX + boxWidth, boxY, boxX + boxWidth - 10, boxY - 10);
        g2d.drawLine(boxX, boxY + boxHeight, boxX - 10, boxY + boxHeight - 10);

        g2d.dispose();
        return new ImageIcon(image);
    }

    private void loadMerekFromDatabase(JComboBox<String> cb) {
        cb.removeAllItems();
        try (Connection conn = getConnection()) {
            Statement st = conn.createStatement();
            // Use MEREK table
            ResultSet rs = st.executeQuery("SELECT nama_merek FROM MEREK ORDER BY nama_merek");
            while (rs.next()) {
                String m = rs.getString("nama_merek");
                if (m != null && !m.isEmpty())
                    cb.addItem(m);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void simpanMerekBaruJikaBelumAda(String merek) {
        if (merek == null || merek.trim().isEmpty()) {
            return;
        }
        try (Connection conn = getConnection()) {
            // Check MEREK table
            String checkSql = "SELECT COUNT(*) FROM MEREK WHERE LOWER(nama_merek) = LOWER(?)";
            PreparedStatement checkPst = conn.prepareStatement(checkSql);
            checkPst.setString(1, merek);
            ResultSet rs = checkPst.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                // Insert if not exists
                String insertSql = "INSERT INTO MEREK (nama_merek) VALUES (?)";
                PreparedStatement pst = conn.prepareStatement(insertSql);
                pst.setString(1, merek);
                pst.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showMerekDialog() {
        JDialog dialog = new JDialog(this, "Kelola Merek", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 500);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // === PANEL INPUT ===
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        JLabel lblMerek = new JLabel("Nama Merek:");
        lblMerek.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JTextField tfMerek = new JTextField();
        tfMerek.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tfMerek.setPreferredSize(new Dimension(250, 35));

        JButton btnTambahMerek = new JButton("Tambah");
        styleButton(btnTambahMerek, GREEN_PRIMARY);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        inputPanel.add(lblMerek, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        inputPanel.add(tfMerek, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        inputPanel.add(btnTambahMerek, gbc);

        // === TABEL MEREK ===
        DefaultTableModel merekModel = new DefaultTableModel(
                new String[] { "ID", "Nama Merek", "Jumlah Kaos" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 2)
                    return Integer.class;
                return String.class;
            }
        };

        JTable tableMerek = new JTable(merekModel);
        tableMerek.setRowHeight(30);
        tableMerek.setSelectionBackground(GREEN_LIGHT);
        tableMerek.setSelectionForeground(TEXT_DARK);
        tableMerek.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableMerek.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        tableMerek.getColumnModel().getColumn(0).setMinWidth(0);
        tableMerek.getColumnModel().getColumn(0).setMaxWidth(0);

        tableMerek.getColumnModel().getColumn(1).setPreferredWidth(200);
        tableMerek.getColumnModel().getColumn(2).setPreferredWidth(80);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tableMerek.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(tableMerek);

        // === PANEL TOMBOL BAWAH ===
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonPanel.setOpaque(false);

        JButton btnEdit = new JButton("Edit");
        JButton btnHapus = new JButton("Hapus");
        JButton btnTutup = new JButton("Tutup");

        styleButton(btnEdit, new Color(52, 152, 219));
        styleButton(btnHapus, new Color(231, 76, 60));
        styleButton(btnTutup, new Color(153, 153, 153));

        buttonPanel.add(btnEdit);
        buttonPanel.add(btnHapus);
        buttonPanel.add(btnTutup);

        // === LAYOUT ===
        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);

        // === LOAD DATA ===
        loadMerekData(merekModel);

        // === ACTION LISTENERS ===
        btnTambahMerek.addActionListener(e -> {
            String namaMerek = tfMerek.getText().trim();
            if (namaMerek.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Nama merek harus diisi!",
                        "Error",
                        JOptionPane.WARNING_MESSAGE);
                tfMerek.requestFocus();
                return;
            }

            if (tambahMerek(namaMerek)) {
                tfMerek.setText("");
                loadMerekData(merekModel);
                loadData(); // Refresh main table to update merek dropdowns
            }
        });

        tfMerek.addActionListener(e -> btnTambahMerek.doClick());

        btnEdit.addActionListener(e -> {
            int selectedRow = tableMerek.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Pilih merek yang akan diedit!",
                        "Peringatan",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Merek tidak punya ID terpisah, hanya nama. Edit langsung nama di KAOS_MASTER
            String namaLama = merekModel.getValueAt(selectedRow, 1).toString();

            String namaBaru = JOptionPane.showInputDialog(dialog,
                    "Edit nama merek:",
                    namaLama);

            if (namaBaru != null && !namaBaru.trim().isEmpty() && !namaBaru.equals(namaLama)) {
                if (editMerek(namaLama, namaBaru.trim())) {
                    loadMerekData(merekModel);
                    loadData();
                }
            }
        });

        btnHapus.addActionListener(e -> {
            int selectedRow = tableMerek.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Pilih merek yang akan dihapus!",
                        "Peringatan",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String namaMerek = merekModel.getValueAt(selectedRow, 1).toString();
            int jumlahKaos = (Integer) merekModel.getValueAt(selectedRow, 2);

            if (jumlahKaos > 0) {
                JOptionPane.showMessageDialog(dialog,
                        "Merek tidak dapat dihapus!\n" +
                                "Masih ada " + jumlahKaos + " kaos dengan merek '" + namaMerek + "'.\n" +
                                "Pindahkan kaos terlebih dahulu ke merek lain atau hapus kaos tersebut.",
                        "Merek Terpakai",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Hapus merek: " + namaMerek + "?",
                    "Konfirmasi Hapus",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                if (hapusMerek(namaMerek)) {
                    loadMerekData(merekModel);
                    loadData();
                }
            }
        });

        btnTutup.addActionListener(e -> dialog.dispose());

        tableMerek.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    btnEdit.doClick();
                }
            }
        });

        addHoverEffect(btnTambahMerek, GREEN_PRIMARY);
        addHoverEffect(btnEdit, new Color(52, 152, 219));
        addHoverEffect(btnHapus, new Color(231, 76, 60));
        addHoverEffect(btnTutup, new Color(153, 153, 153));

        dialog.setVisible(true);
    }

    private void loadMerekData(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = getConnection()) {
            // Count kaos usage for each merek in MEREK table
            String sql = "SELECT m.id, m.nama_merek, COUNT(k.id) as jumlah_kaos " +
                    "FROM MEREK m LEFT JOIN KAOS_MASTER k ON m.nama_merek = k.merek " +
                    "GROUP BY m.id, m.nama_merek ORDER BY m.nama_merek ASC";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getString("nama_merek"),
                        rs.getInt("jumlah_kaos")
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Gagal memuat data merek!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean tambahMerek(String namaMerek) {
        try (Connection conn = getConnection()) {
            // Check in MEREK table
            String checkSql = "SELECT COUNT(*) FROM MEREK WHERE LOWER(nama_merek) = LOWER(?)";
            PreparedStatement checkPst = conn.prepareStatement(checkSql);
            checkPst.setString(1, namaMerek);
            ResultSet rs = checkPst.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this,
                        "Merek '" + namaMerek + "' sudah ada!",
                        "Duplikat",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }

            // Insert into MEREK table
            String insertSql = "INSERT INTO MEREK (nama_merek) VALUES (?)";
            PreparedStatement pst = conn.prepareStatement(insertSql);
            pst.setString(1, namaMerek);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "Merek '" + namaMerek + "' berhasil ditambahkan!");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Gagal menambah merek!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean editMerek(String namaLama, String namaBaru) {
        try (Connection conn = getConnection()) {
            // Check duplicates in MEREK
            String checkSql = "SELECT COUNT(*) FROM MEREK WHERE LOWER(nama_merek) = LOWER(?) AND nama_merek != ?";
            PreparedStatement checkPst = conn.prepareStatement(checkSql);
            checkPst.setString(1, namaBaru);
            checkPst.setString(2, namaLama);
            ResultSet rs = checkPst.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this,
                        "Merek '" + namaBaru + "' sudah ada!",
                        "Duplikat",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }

            // Update MEREK table
            String updateMerekSql = "UPDATE MEREK SET nama_merek = ? WHERE nama_merek = ?";
            PreparedStatement pstMerek = conn.prepareStatement(updateMerekSql);
            pstMerek.setString(1, namaBaru);
            pstMerek.setString(2, namaLama);
            pstMerek.executeUpdate();

            // Also update KAOS_MASTER to reflect change
            String updateMasterSql = "UPDATE KAOS_MASTER SET merek = ? WHERE merek = ?";
            PreparedStatement pstMaster = conn.prepareStatement(updateMasterSql);
            pstMaster.setString(1, namaBaru);
            pstMaster.setString(2, namaLama);
            pstMaster.executeUpdate();

            JOptionPane.showMessageDialog(this, "Merek berhasil diupdate!");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Gagal mengupdate merek!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean hapusMerek(String namaMerek) {
        try (Connection conn = getConnection()) {
            // Check usage in KAOS_MASTER
            String checkSql = "SELECT COUNT(*) FROM KAOS_MASTER WHERE merek = ?";
            PreparedStatement checkPst = conn.prepareStatement(checkSql);
            checkPst.setString(1, namaMerek);
            ResultSet rs = checkPst.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this,
                        "Tidak dapat menghapus merek '" + namaMerek + "' karena masih digunakan oleh " + rs.getInt(1)
                                + " kaos.",
                        "Merek Terpakai",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }

            // Delete from MEREK table
            String deleteSql = "DELETE FROM MEREK WHERE nama_merek = ?";
            PreparedStatement pst = conn.prepareStatement(deleteSql);
            pst.setString(1, namaMerek);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Merek '" + namaMerek + "' berhasil dihapus!");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Gagal menghapus merek!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void addSampleDataIfEmpty() {
        try (Connection conn = getConnection()) {
            // Cek apakah ada data kaos
            String checkSql = "SELECT COUNT(*) as count FROM KAOS_MASTER";
            Statement checkStmt = conn.createStatement();
            ResultSet rs = checkStmt.executeQuery(checkSql);

            if (rs.next() && rs.getInt("count") == 0) {
                System.out.println("📝 Menambah data sample kaos dengan varian...");

                int option = JOptionPane.showConfirmDialog(this,
                        "Database kaos kosong. Tambah data sample untuk testing?",
                        "Data Sample",
                        JOptionPane.YES_NO_OPTION);

                if (option == JOptionPane.YES_OPTION) {
                    // Tambah kategori sample untuk distro
                    String insertKategori = "INSERT IGNORE INTO kategori (nama_kategori) VALUES " +
                            "('Pria'), ('Wanita'), ('Unisex'), ('Anak-anak'), ('Oversize')";
                    Statement stmtKategori = conn.createStatement();
                    stmtKategori.execute(insertKategori);

                    // Tambah kaos sample ke KAOS_MASTER
                    String insertKaos = "INSERT INTO KAOS_MASTER (id, nama_kaos, merek, kategori_id, foto_utama) VALUES "
                            +
                            "(1, 'Kaos Polo Pria Premium', 'DistroZone', 1, NULL), " +
                            "(2, 'Kaos Wanita Basic', 'FashionGirl', 2, NULL), " +
                            "(3, 'Kaos Oversize Streetwear', 'StreetStyle', 5, NULL)";
                    Statement stmtKaos = conn.createStatement();
                    stmtKaos.execute(insertKaos);

                    // Tambah varian ke KAOS_VARIAN
                    // Generate codes: KSP001-L-HIT, etc.
                    String insertVarian = "INSERT INTO KAOS_VARIAN (kaos_master_id, kode_varian, warna, size, harga, harga_pokok, stok) VALUES "
                            +
                            "(1, 'KSP001-HIT-L', 'Hitam', 'L', 120000, 80000, 15), " +
                            "(1, 'KSP001-PUT-L', 'Putih', 'L', 120000, 80000, 10), " +
                            "(1, 'KSP001-HIT-XL', 'Hitam', 'XL', 125000, 85000, 8), " +
                            "(1, 'KSP001-HIT-L-PJG', 'Hitam', 'L', 140000, 95000, 5), " + // Assume default type if not
                                                                                          // column
                            "(2, 'KSW001-MER-M', 'Merah', 'M', 95000, 60000, 20), " +
                            "(2, 'KSW001-PIN-S', 'Pink', 'S', 95000, 60000, 15), " +
                            "(3, 'KSU001-PUT-XL', 'Putih', 'XL', 110000, 70000, 12), " +
                            "(3, 'KSU001-ABU-XXL', 'Abu-abu', 'XXL', 115000, 75000, 8)";
                    Statement stmtVarian = conn.createStatement();
                    stmtVarian.execute(insertVarian);

                    System.out.println("✅ Data sample kaos dengan varian berhasil ditambahkan");
                    JOptionPane.showMessageDialog(this,
                            "✅ Data sample kaos dengan varian berhasil ditambahkan!",
                            "Sukses",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                System.out.println("✅ Database sudah berisi " + rs.getInt("count") + " kaos");
            }

        } catch (Exception e) {
            System.out.println("❌ Gagal menambah data sample: " + e.getMessage());
        }
    }

    // ================= UNIFIED FORM (MASTER + VARIANT) =================
    private void showUnifiedForm(Integer kaosId) {
        String title = (kaosId == null) ? "➕ Tambah Produk Baru" : "✏️ Edit Produk Lengkap";
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(900, 700); // Larger size for full table
        dialog.setLocationRelativeTo(this);

        // === 1. MASTER INFO PANEL (Top) ===
        JPanel masterPanel = new JPanel(new GridBagLayout());
        masterPanel.setBorder(BorderFactory.createTitledBorder("Informasi Utama"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        JTextField tfNama = new JTextField(20);
        // tfMerek replaced by cbMerek
        JComboBox<String> cbMerek = new JComboBox<>();
        cbMerek.setEditable(true);
        loadMerekFromDatabase(cbMerek);
        JComboBox<String> cbKategori = new JComboBox<>();
        loadKategoriFromDatabase(cbKategori);

        JComboBox<String> cbTypeKaos = new JComboBox<>(typeOptions);
        JTextArea taDeskripsi = new JTextArea(3, 20);
        taDeskripsi.setLineWrap(true);
        taDeskripsi.setWrapStyleWord(true);
        JScrollPane spDeskripsi = new JScrollPane(taDeskripsi);

        // Image Logic
        JLabel lblFoto = new JLabel("No Image", SwingConstants.CENTER);
        lblFoto.setPreferredSize(new Dimension(100, 100));
        lblFoto.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        final String[] pathFoto = { null };
        JButton btnPilihFoto = new JButton("Pilih Foto");

        btnPilihFoto.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Gambar (*.jpg, *.png)", "jpg", "jpeg", "png"));
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                pathFoto[0] = f.getAbsolutePath();
                ImageIcon icon = new ImageIcon(pathFoto[0]);
                Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                lblFoto.setIcon(new ImageIcon(img));
                lblFoto.setText("");
            }
        });

        // Layout Master
        int row = 0;
        addLabelAndField(masterPanel, gbc, "Nama Kaos*:", tfNama, row++);
        addLabelAndCombo(masterPanel, gbc, "Merek*:", cbMerek, row++);
        addLabelAndCombo(masterPanel, gbc, "Kategori:", cbKategori, row++);
        addLabelAndCombo(masterPanel, gbc, "Tipe:", cbTypeKaos, row++);

        gbc.gridx = 0;
        gbc.gridy = row;
        masterPanel.add(new JLabel("Deskripsi:"), gbc);
        gbc.gridx = 1;
        masterPanel.add(spDeskripsi, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        masterPanel.add(new JLabel("Foto Utama:"), gbc);
        gbc.gridx = 1;
        JPanel photoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        photoPanel.add(lblFoto);
        photoPanel.add(btnPilihFoto);
        masterPanel.add(photoPanel, gbc);

        // === 2. VARIANT TABLE (Center) ===
        // Cols: ID(Hidden), WarnaName, WarnaHex(HiddenText), WarnaVisual, Size, Harga,
        // Pokok, Stok, FotoPath(Hidden), FotoPreview
        // Cols: ID(Hidden), WarnaName, WarnaHex(HiddenText), WarnaVisual, Size, Harga,
        // Pokok, Stok, FotoPath(Hidden), FotoPreview
        String[] columns = { "ID", "Nama Warna", "Hex", "Color", "Size", "Type", "Harga Jual", "Harga Pokok", "Stok",
                "FotoPath", "Foto" };
        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Editable: WarnaName(1), Hex(2), Color(3-Click), Size(4), Type(5-No), Jual(6),
                // Pokok(7), Stok(8)
                // Non-editable: ID(0), Color(3), Type(5), FotoPath(9), Foto(10)
                return col != 0 && col != 3 && col != 5 && col != 9 && col != 10;
            }
        };
        tableModel.setColumnIdentifiers(columns);

        JTable tableVarian = new JTable(tableModel);
        tableVarian.setRowHeight(40);

        // Hide ID(0), Hex(2-Hidden but handy for edit?), FotoPath(8)
        // Actually, keep Hex visible but maybe small? Or hidden if Visual is enough.
        // User asked "biar website menangkap". It's cleaner to show Hex code too.
        tableVarian.getColumnModel().getColumn(0).setMinWidth(0);
        tableVarian.getColumnModel().getColumn(0).setMaxWidth(0);
        tableVarian.getColumnModel().getColumn(0).setWidth(0);

        tableVarian.getColumnModel().getColumn(9).setMinWidth(0);
        tableVarian.getColumnModel().getColumn(9).setMaxWidth(0);
        tableVarian.getColumnModel().getColumn(9).setWidth(0);

        // Editors
        JComboBox<String> cbSizeEditor = new JComboBox<>(sizeOptions);
        tableVarian.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(cbSizeEditor));

        // Numeric Editors for Harga(6), Pokok(7), Stok(8)
        JTextField tfNum = new JTextField();
        util.InputValidator.restrictToNumbers(tfNum);
        DefaultCellEditor numEditor = new DefaultCellEditor(tfNum);
        tableVarian.getColumnModel().getColumn(6).setCellEditor(numEditor);
        tableVarian.getColumnModel().getColumn(7).setCellEditor(numEditor);
        tableVarian.getColumnModel().getColumn(8).setCellEditor(numEditor);

        // Color Renderer (Col 3)
        tableVarian.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSel, boolean hasFocus,
                    int row, int col) {
                JLabel l = new JLabel();
                l.setOpaque(true);
                l.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                try {
                    String hex = (String) tableModel.getValueAt(row, 2); // Get Hex from col 2
                    if (hex != null && !hex.isEmpty()) {
                        l.setBackground(Color.decode(hex));
                        l.setToolTipText(hex);
                    } else {
                        l.setBackground(Color.WHITE);
                        l.setText("Pick");
                        l.setHorizontalAlignment(CENTER);
                    }
                } catch (Exception e) {
                    l.setBackground(Color.WHITE);
                }
                return l;
            }
        });

        // Photo Renderer (Col 10)
        tableVarian.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSel, boolean hasFocus,
                    int row, int col) {
                JLabel l = new JLabel();
                l.setHorizontalAlignment(CENTER);
                if (value instanceof Icon)
                    l.setIcon((Icon) value);
                else
                    l.setText("📸");
                if (isSel)
                    l.setBackground(table.getSelectionBackground());
                else
                    l.setBackground(table.getBackground());
                l.setOpaque(true);
                return l;
            }
        });

        // Click Listener for Color & Photo
        tableVarian.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = tableVarian.rowAtPoint(e.getPoint());
                int col = tableVarian.columnAtPoint(e.getPoint());
                if (row == -1)
                    return;

                // Col 3 = Color Picker
                if (col == 3) {
                    String currentHex = (String) tableModel.getValueAt(row, 2);
                    Color initial = Color.BLACK;
                    try {
                        if (currentHex != null)
                            initial = Color.decode(currentHex);
                    } catch (Exception ex) {
                    }

                    Color c = JColorChooser.showDialog(dialog, "Pilih Warna", initial);
                    if (c != null) {
                        String newHex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
                        tableModel.setValueAt(newHex, row, 2);
                        tableVarian.repaint();
                    }
                }

                // Col 10 = Photo Picker
                if (col == 10) {
                    JFileChooser ch = new JFileChooser();
                    ch.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "png"));
                    if (ch.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                        String path = ch.getSelectedFile().getAbsolutePath();
                        tableModel.setValueAt(path, row, 9);
                        ImageIcon ic = new ImageIcon(path);
                        tableModel.setValueAt(
                                new ImageIcon(ic.getImage().getScaledInstance(35, 35, Image.SCALE_SMOOTH)), row, 10);
                    }
                }
            }
        });

        JScrollPane spVarian = new JScrollPane(tableVarian);
        spVarian.setBorder(BorderFactory.createTitledBorder("Daftar Varian (Warna & Ukuran)"));

        // Toolbar
        JPanel tableTools = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddRow = new JButton("➕ Tambah Varian");
        JButton btnDelRow = new JButton("🗑️ Hapus Varian");
        styleButton(btnAddRow, new Color(52, 152, 219));
        styleButton(btnDelRow, new Color(231, 76, 60));

        btnAddRow.addActionListener(evtAdd -> {
            showBatchAddVariantDialog(dialog, tableModel);
        });

        // Sync Type selection to all rows
        cbTypeKaos.addActionListener(evtType -> {
            String type = (String) cbTypeKaos.getSelectedItem();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(type, i, 5); // Index 5 is Type
            }
        });
        btnDelRow.addActionListener(evtDel -> {
            int sr = tableVarian.getSelectedRow();
            if (sr != -1)
                tableModel.removeRow(sr);
        });
        tableTools.add(btnAddRow);
        tableTools.add(btnDelRow);

        // Panel Table (Tools + Table)
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(tableTools, BorderLayout.NORTH);
        tablePanel.add(spVarian, BorderLayout.CENTER);

        // === 3. BUTTONS (Bottom) ===
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnSimpan = new JButton("💾 SIMPAN SEMUA");
        JButton btnBatal = new JButton("Batal");
        styleButton(btnSimpan, GREEN_PRIMARY);
        btnSimpan.setPreferredSize(new Dimension(150, 40));

        btnSimpan.addActionListener(e -> {
            if (tableVarian.isEditing()) {
                tableVarian.getCellEditor().stopCellEditing();
            }
            saveUnifiedData(dialog, kaosId,
                    tfNama.getText(), (String) cbMerek.getSelectedItem(),
                    cbKategori.getSelectedItem().toString(),
                    cbTypeKaos.getSelectedItem().toString(),
                    taDeskripsi.getText(),
                    pathFoto[0],
                    tableModel);
        });
        btnBatal.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnSimpan);
        btnPanel.add(btnBatal);

        // === LAYOUT FINAL ===
        dialog.add(masterPanel, BorderLayout.NORTH);
        dialog.add(tablePanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        // Load Data
        if (kaosId != null) {
            try (Connection conn = getConnection()) {
                String sql = "SELECT * FROM KAOS_MASTER WHERE id=?";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setInt(1, kaosId);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    tfNama.setText(rs.getString("nama_kaos"));
                    cbMerek.setSelectedItem(rs.getString("merek"));
                    cbTypeKaos.setSelectedItem(rs.getString("type_kaos"));
                    taDeskripsi.setText(rs.getString("deskripsi"));
                    String img = rs.getString("foto_utama");
                    if (img != null) {
                        pathFoto[0] = util.ImageHelper.resolveImagePath(img);
                        ImageIcon ic = new ImageIcon(pathFoto[0]);
                        lblFoto.setIcon(new ImageIcon(ic.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
                        lblFoto.setText("");
                    }
                    // Kategori select
                    // Need to find combo item by ID? Or Name?
                    // Loader uses Name. Need to fetch name by ID.
                    int kId = rs.getInt("kategori_id");
                    // Quick fix: loop combo
                    // ... implementation detail: map DB kat_id to Combo String ...
                }

                String vSql = "SELECT * FROM KAOS_VARIAN WHERE kaos_master_id=?";
                PreparedStatement vPst = conn.prepareStatement(vSql);
                vPst.setInt(1, kaosId);
                ResultSet vRs = vPst.executeQuery();
                while (vRs.next()) {
                    String vp = vRs.getString("foto_varian");
                    ImageIcon vIcon = null;
                    String vAbs = null;
                    if (vp != null && !vp.isEmpty()) {
                        vAbs = util.ImageHelper.resolveImagePath(vp);
                        if (new File(vAbs).exists())
                            vIcon = new ImageIcon(
                                    new ImageIcon(vAbs).getImage().getScaledInstance(35, 35, Image.SCALE_SMOOTH));
                    }
                    String hex = vRs.getString("warna_hex");
                    if (hex == null)
                        hex = "#000000"; // Fallback

                    tableModel.addRow(new Object[] {
                            vRs.getInt("id"),
                            vRs.getString("warna"),
                            hex,
                            "", // Placeholder for Visual
                            vRs.getString("size"),
                            (String) cbTypeKaos.getSelectedItem(), // Type
                            vRs.getLong("harga"),
                            vRs.getLong("harga_pokok"),
                            vRs.getInt("stok"),
                            vAbs,
                            vIcon
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            // New Kaos
            String type = (String) cbTypeKaos.getSelectedItem();
            tableModel.addRow(new Object[] { 0, "", "#000000", "", "M", type, "0", "0", "10", null, null });
        }

        dialog.setVisible(true);
    }

    private void saveUnifiedData(JDialog dialog, Integer id, String nama, String merek, String kat, String type,
            String desc, String photoPath, DefaultTableModel model) {
        if (nama.isEmpty() || merek.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Nama dan Merek wajib diisi!");
            return;
        }

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int katId = getKategoriIdByName(conn, kat);
                int masterId;

                // 1. Save Master
                String dbPhoto = null;
                if (photoPath != null) {
                    if (new File(photoPath).isAbsolute())
                        dbPhoto = copyImageToWeb(photoPath);
                    else
                        dbPhoto = photoPath; // Should be impossible if logic consistent, but safe coverage
                    // Actually, if we loaded "assets/.." resolved to "C:/...", it is absolute.
                    // We need to know if it CHANGED.
                    // Simple logic: Always re-copy/synch is safe but wasteful.
                    // Better: If resolved path == loaded path, assume relative.
                    // For now, `copyImageToWeb` handles "if sourceFile exists -> copy".
                    // If we pass an already copied absolute path, it will copy it again to a new
                    // UUID.
                    // Ideally check if filename is already in assets...
                    // Let's rely on standard copy for now to ensure consistency.
                } else {
                    // If null, check if we had one?
                    // current logic: if photoPath is null, it means no photo set.
                }

                if (id == null) {
                    masterId = getLowestAvailableId(conn); // Or auto-increment
                    String sql = "INSERT INTO KAOS_MASTER (id, nama_kaos, merek, kategori_id, type_kaos, deskripsi, foto_utama) VALUES (?,?,?,?,?,?,?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setInt(1, masterId);
                    ps.setString(2, nama);
                    ps.setString(3, merek);
                    ps.setInt(4, katId);
                    ps.setString(5, type);
                    ps.setString(6, desc);
                    ps.setString(7, dbPhoto);
                    ps.executeUpdate();
                } else {
                    masterId = id;
                    String sql = "UPDATE KAOS_MASTER SET nama_kaos=?, merek=?, kategori_id=?, type_kaos=?, deskripsi=?, foto_utama=COALESCE(?, foto_utama) WHERE id=?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, nama);
                    ps.setString(2, merek);
                    ps.setInt(3, katId);
                    ps.setString(4, type);
                    ps.setString(5, desc);
                    ps.setString(6, dbPhoto);
                    ps.setInt(7, masterId);
                    ps.executeUpdate();
                }

                // 2. Save Variants
                // Get existing IDs to handle deletions
                Set<Integer> oldIds = new HashSet<>();
                if (id != null) {
                    ResultSet rs = conn.createStatement()
                            .executeQuery("SELECT id FROM KAOS_VARIAN WHERE kaos_master_id=" + masterId);
                    while (rs.next())
                        oldIds.add(rs.getInt(1));
                }

                Set<Integer> keptIds = new HashSet<>();

                // Get next sequence number
                // Handled by CodeGenerator now

                for (int i = 0; i < model.getRowCount(); i++) {
                    // Cols: 0:ID, 1:Warna, 2:Hex, 3:Vis, 4:Size, 5:Type, 6:Harga, 7:Pokok, 8:Stok,
                    // 9:Path, 10:Icon
                    Integer vId = (Integer) model.getValueAt(i, 0);
                    String warna = (String) model.getValueAt(i, 1);
                    String hex = (String) model.getValueAt(i, 2);
                    String size = (String) model.getValueAt(i, 4);
                    // Type at 5 is ignored for saving to Varian (it's in Master)
                    long harga = Long.parseLong(model.getValueAt(i, 6).toString());
                    long pokok = Long.parseLong(model.getValueAt(i, 7).toString());
                    int stok = Integer.parseInt(model.getValueAt(i, 8).toString());
                    String vPath = (String) model.getValueAt(i, 9);

                    String vDbPhoto = null;
                    if (vPath != null) {
                        if (new File(vPath).isAbsolute())
                            vDbPhoto = copyImageToWeb(vPath);
                        else
                            vDbPhoto = vPath;
                    }

                    if (vId == null || vId == 0) {
                        // Generate Sequential Code: KV-000-00 (MasterID-Seq)
                        String kv = util.CodeGenerator.generateVariantCode(conn, masterId);

                        // Add warna_hex to INSERT
                        String ins = "INSERT INTO KAOS_VARIAN (kaos_master_id, kode_varian, warna, warna_hex, size, harga, harga_pokok, stok, foto_varian) VALUES (?,?,?,?,?,?,?,?,?)";
                        PreparedStatement p = conn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS);
                        p.setInt(1, masterId);
                        p.setString(2, kv);
                        p.setString(3, warna);
                        p.setString(4, hex);
                        p.setString(5, size);
                        p.setLong(6, harga);
                        p.setLong(7, pokok);
                        p.setInt(8, stok);
                        p.setString(9, vDbPhoto);
                        p.executeUpdate();
                    } else {
                        if (oldIds.contains(vId)) {
                            keptIds.add(vId);
                            // Add warna_hex to UPDATE
                            String upd = "UPDATE KAOS_VARIAN SET warna=?, warna_hex=?, size=?, harga=?, harga_pokok=?, stok=?, foto_varian=COALESCE(?, foto_varian) WHERE id=?";
                            PreparedStatement p = conn.prepareStatement(upd);
                            p.setString(1, warna);
                            p.setString(2, hex);
                            p.setString(3, size);
                            p.setLong(4, harga);
                            p.setLong(5, pokok);
                            p.setInt(6, stok);
                            p.setString(7, vDbPhoto);
                            p.setInt(8, vId);
                            p.executeUpdate();
                        }
                    }
                }

                // Delete removed
                for (Integer old : oldIds) {
                    if (!keptIds.contains(old)) {
                        conn.createStatement().executeUpdate("DELETE FROM KAOS_VARIAN WHERE id=" + old);
                    }
                }

                conn.commit();
                JOptionPane.showMessageDialog(dialog, "✅ Data berhasil disimpan!");
                dialog.dispose();
                loadData();

            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(dialog, "❌ Gagal menyimpan: " + ex.getMessage());
        }
    }

    // ================= BATCH ADD VARIANT DIALOG =================
    private void showBatchAddVariantDialog(JDialog parent, DefaultTableModel model) {
        JDialog d = new JDialog(parent, "➕ Tambah Varian", true);
        d.setSize(400, 500);
        d.setLocationRelativeTo(parent);
        d.setLayout(new BorderLayout());

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;

        // 1. Warna
        gbc.gridwidth = 2; // Span 2 columns
        p.add(new JLabel("Pilih Warna:"), gbc);
        gbc.gridy++;
        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JTextField tfWarnaName = new JTextField(15);
        util.InputValidator.restrictToText(tfWarnaName);
        tfWarnaName.setToolTipText("Nama Warna (contoh: Hitam)");
        JLabel lblColorPreview = new JLabel();
        lblColorPreview.setOpaque(true);
        lblColorPreview.setBackground(Color.WHITE);
        lblColorPreview.setPreferredSize(new Dimension(25, 25));
        lblColorPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JButton btnPickColor = new JButton("Pilih");
        final String[] selectedHex = { "" };
        System.out.println("DEBUG: showBatchAddVariantDialog opened. selectedHex def: '" + selectedHex[0] + "'");

        btnPickColor.addActionListener(e -> {
            Color c = JColorChooser.showDialog(d, "Pilih Warna", Color.BLACK);
            if (c != null) {
                lblColorPreview.setBackground(c);
                String hex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
                selectedHex[0] = hex;
                // Auto-fill name if empty
                if (tfWarnaName.getText().trim().isEmpty()) {
                    tfWarnaName.setText(hex);
                }
            }
        });

        colorPanel.add(tfWarnaName);
        colorPanel.add(Box.createHorizontalStrut(5));
        colorPanel.add(lblColorPreview);
        colorPanel.add(Box.createHorizontalStrut(5));
        colorPanel.add(btnPickColor);
        p.add(colorPanel, gbc);

        // 2. Ukuran (Dropdown)
        gbc.gridy++;
        p.add(new JLabel("Pilih Ukuran:"), gbc);
        gbc.gridy++;
        JComboBox<String> cbSize = new JComboBox<>(sizeOptions);
        p.add(cbSize, gbc);

        // 3. Default Values
        gbc.gridy++;
        p.add(new JSeparator(), gbc);
        gbc.gridy++;
        gbc.gridwidth = 1; // Reset gridwidth for next components (addLabelAndField uses it if not reset,
                           // but better safe)
                           // Actually addLabelAndField sets gridwidth explicitly.

        JTextField tfHarga = new JTextField("0");
        JTextField tfPokok = new JTextField("0");
        JTextField tfStok = new JTextField("10");

        util.InputValidator.restrictToNumbers(tfHarga);
        util.InputValidator.restrictToNumbers(tfPokok);
        util.InputValidator.restrictToNumbers(tfStok);

        addLabelAndField(p, gbc, "Harga Jual:", tfHarga, gbc.gridy);
        gbc.gridy++;
        addLabelAndField(p, gbc, "Harga Pokok:", tfPokok, gbc.gridy);
        gbc.gridy++;
        addLabelAndField(p, gbc, "Stok Awal:", tfStok, gbc.gridy);
        gbc.gridy++;

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAdd = new JButton("Tambahkan");
        JButton btnCancel = new JButton("Batal");
        styleButton(btnAdd, GREEN_PRIMARY);

        btnAdd.addActionListener(e -> {
            String wName = tfWarnaName.getText().trim();
            if (wName.isEmpty()) {
                JOptionPane.showMessageDialog(d, "Nama warna harus diisi!");
                return;
            }

            String selectedSize = (String) cbSize.getSelectedItem();
            if (selectedSize == null || selectedSize.isEmpty()) {
                JOptionPane.showMessageDialog(d, "Pilih ukuran!");
                return;
            }

            // Sync Hex
            if (!wName.isEmpty() && (selectedHex[0] == null || selectedHex[0].isEmpty())) {
                selectedHex[0] = getColorHex(wName);
            } else if (selectedHex[0] == null) {
                selectedHex[0] = "#000000";
            }

            String type = "Lengan Pendek";
            if (model.getRowCount() > 0) {
                type = (String) model.getValueAt(0, 5);
            }

            model.addRow(new Object[] {
                    0,
                    wName,
                    selectedHex[0],
                    "",
                    selectedSize,
                    type,
                    tfHarga.getText(),
                    tfPokok.getText(),
                    tfStok.getText(),
                    null,
                    null
            });

            d.dispose();
        });

        btnCancel.addActionListener(e -> d.dispose());

        btnPanel.add(btnCancel);
        btnPanel.add(btnAdd);

        d.add(p, BorderLayout.CENTER);
        d.add(btnPanel, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    // ================= MAIN METHOD (untuk testing) =================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            KaosManagement frame = new KaosManagement(1, "Admin", "ADM001");
            frame.setVisible(true);
        });
    }
}