import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.io.File;
import java.awt.image.BufferedImage;

public class DaftarKaos extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private DecimalFormat df;
    private JTextField tfSearch;
    private JComboBox<String> cbKategoriFilter;
    private TableRowSorter<DefaultTableModel> sorter;

    // User info
    private int userId;
    private String namaUser;
    private String userCode;

    // Warna tema hijau Distro Zone (Emerald Green - Sesuai Web)
    private static final Color GREEN_PRIMARY = new Color(16, 185, 129); // #10B981
    private static final Color GREEN_DARK = new Color(15, 118, 110); // #0F766E

    private static final Color OUT_OF_STOCK = new Color(239, 68, 68); // #EF4444 Red
    private static final Color LOW_STOCK = new Color(245, 158, 11); // #F59E0B Amber

    public DaftarKaos(int userId, String namaUser, String userCode) {
        this.userId = userId;
        this.namaUser = namaUser;
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

        setTitle("👕 Daftar Kaos - Kasir: " + namaUser);
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

        JLabel lblHeader = new JLabel("Daftar Kaos");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblHeader.setForeground(Color.WHITE);

        headerPanel.add(lblIcon);
        headerPanel.add(lblHeader);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ================= PANEL SEARCH & FILTER =================
        JPanel searchFilterPanel = new JPanel(new BorderLayout(10, 10));
        searchFilterPanel.setOpaque(false);
        searchFilterPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setOpaque(false);

        tfSearch = new JTextField();
        tfSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tfSearch.setPreferredSize(new Dimension(400, 35));
        tfSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREEN_DARK, 2),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        tfSearch.setToolTipText("Cari berdasarkan nama, merek, kategori...");

        JButton btnCari = createIconButton("search.png", "Cari", GREEN_PRIMARY);
        btnCari.setPreferredSize(new Dimension(100, 35));

        searchPanel.add(new JLabel("Pencarian:"));
        searchPanel.add(tfSearch);
        searchPanel.add(btnCari);

        // Filter kategori
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        filterPanel.setOpaque(false);

        cbKategoriFilter = new JComboBox<>();
        cbKategoriFilter.addItem("Semua Kategori");
        loadKategoriForFilter();
        cbKategoriFilter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbKategoriFilter.setPreferredSize(new Dimension(200, 35));

        filterPanel.add(new JLabel("Filter Kategori:"));
        filterPanel.add(cbKategoriFilter);

        searchFilterPanel.add(searchPanel, BorderLayout.WEST);
        searchFilterPanel.add(filterPanel, BorderLayout.EAST);

        // ================= TABEL KAOS =================
        model = new DefaultTableModel(
                new String[] { "Kode", "Nama Kaos", "Merek", "Type", "Warna", "Size",
                        "Kategori", "Harga Jual", "Stok", "Status", "Foto" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 10)
                    return ImageIcon.class; // Foto
                if (columnIndex == 5 || columnIndex == 8)
                    return String.class; // Size & Stok
                return String.class;
            }
        };

        table = new JTable(model);
        table.setRowHeight(60);
        table.setSelectionBackground(GREEN_PRIMARY);
        table.setSelectionForeground(Color.WHITE);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Custom Green Header Renderer
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(GREEN_PRIMARY);
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 13));
                setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
                setHorizontalAlignment(SwingConstants.CENTER);
                return this;
            }
        };

        // Apply header renderer to all columns
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(100); // Kode
        table.getColumnModel().getColumn(1).setPreferredWidth(200); // Nama
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Merek
        table.getColumnModel().getColumn(3).setPreferredWidth(80); // Type
        table.getColumnModel().getColumn(4).setPreferredWidth(80); // Warna
        table.getColumnModel().getColumn(5).setPreferredWidth(60); // Size
        table.getColumnModel().getColumn(6).setPreferredWidth(120); // Kategori
        table.getColumnModel().getColumn(7).setPreferredWidth(120); // Harga
        table.getColumnModel().getColumn(8).setPreferredWidth(60); // Stok
        table.getColumnModel().getColumn(9).setPreferredWidth(100); // Status
        table.getColumnModel().getColumn(10).setPreferredWidth(80); // Foto

        // Center align untuk kolom tertentu
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Type
        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer); // Size
        table.getColumnModel().getColumn(8).setCellRenderer(centerRenderer); // Stok
        // Status has custom renderer below

        // Right align untuk harga
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        table.getColumnModel().getColumn(7).setCellRenderer(rightRenderer); // Harga

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
        table.getColumnModel().getColumn(10).setCellRenderer(imageRenderer);

        // Renderer untuk kolom Status (warna berbeda berdasarkan stok)
        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    String status = value.toString();
                    if (status.contains("HABIS")) {
                        c.setForeground(OUT_OF_STOCK);
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } else if (status.contains("RENDAH")) {
                        c.setForeground(LOW_STOCK);
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } else if (status.contains("TERSEDIA")) {
                        c.setForeground(GREEN_DARK);
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    }
                }

                ((JLabel) c).setHorizontalAlignment(JLabel.CENTER);
                return c;
            }
        };
        table.getColumnModel().getColumn(9).setCellRenderer(statusRenderer);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(GREEN_DARK, 2, true));

        // ================= FILTER REALTIME =================
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        Runnable filterAction = () -> {
            String text = tfSearch.getText().trim();
            String kategori = cbKategoriFilter.getSelectedItem().toString();

            RowFilter<DefaultTableModel, Integer> filter = new RowFilter<DefaultTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                    boolean textMatch = true;
                    boolean kategoriMatch = true;

                    // Filter teks
                    if (!text.isEmpty()) {
                        textMatch = false;
                        // Cari di kolom: Kode (0), Nama (1), Merek (2), Warna (4)
                        for (int i : new int[] { 0, 1, 2, 4 }) {
                            String value = entry.getStringValue(i).toLowerCase();
                            if (value.contains(text.toLowerCase())) {
                                textMatch = true;
                                break;
                            }
                        }
                    }

                    // Filter kategori
                    if (!kategori.equals("Semua Kategori")) {
                        String kategoriValue = entry.getStringValue(6);
                        kategoriMatch = kategoriValue.equals(kategori);
                    }

                    return textMatch && kategoriMatch;
                }
            };

            sorter.setRowFilter(filter);
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

        cbKategoriFilter.addActionListener(e -> filterAction.run());
        btnCari.addActionListener(e -> filterAction.run());

        // ================= PANEL TOMBOL =================
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setOpaque(false);

        // Hanya tombol refresh dan kembali (no CRUD)
        JButton btnDetail = createIconButton("detail.png", "Detail", new Color(155, 89, 182));
        JButton btnRefresh = createIconButton("refresh.png", "Refresh", new Color(46, 204, 113));
        JButton btnBack = createIconButton("back.png", "Kembali", new Color(153, 153, 153));

        btnPanel.add(btnDetail);
        btnPanel.add(btnRefresh);
        btnPanel.add(btnBack);

        // ================= LAYOUT UTAMA =================
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(new Color(255, 255, 255, 245));
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREEN_PRIMARY, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        contentPanel.add(searchFilterPanel, BorderLayout.NORTH);

        // Remove individual border from ScrollPane to avoid double border
        sp.setBorder(null);
        contentPanel.add(sp, BorderLayout.CENTER);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // ================= ACTIONS =================
        btnDetail.addActionListener(e -> showDetailKaos());

        btnRefresh.addActionListener(e -> {
            tfSearch.setText("");
            cbKategoriFilter.setSelectedIndex(0);
            sorter.setRowFilter(null);
            loadData();
        });

        btnBack.addActionListener(e -> {
            dispose();
            new KasirDashboard(userId, namaUser, userCode).setVisible(true);
        });

        // Double click untuk detail
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showDetailKaos();
                }
            }
        });
    }

    private void initData() {
        loadData();
    }

    // ================= DATABASE CONNECTION =================
    private Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/distrozone_db";
        String user = "root";
        String password = "";
        return DriverManager.getConnection(url, user, password);
    }

    // ================= LOAD DATA KAOS =================
    private void loadData() {
        new Thread(() -> {
            System.out.println("DEBUG: Loading daftar kaos with new schema...");

            SwingUtilities.invokeLater(() -> model.setRowCount(0));

            System.out.println("🔄 Memuat data kaos untuk kasir...");

            try (Connection conn = getConnection()) {
                // Updated query to join KAOS_VARIAN and KAOS_MASTER
                String sql = "SELECT kv.kode_varian, km.nama_kaos, km.merek, km.type_kaos, " +
                        "kv.warna, kv.size, kat.nama_kategori, kv.harga, kv.stok, " +
                        "kv.foto_varian, km.foto_utama " +
                        "FROM KAOS_VARIAN kv " +
                        "JOIN KAOS_MASTER km ON kv.kaos_master_id = km.id " +
                        "LEFT JOIN kategori kat ON km.kategori_id = kat.id " +
                        "WHERE kv.stok >= 0 " +
                        "ORDER BY km.nama_kaos ASC, kv.warna ASC, kv.size ASC";

                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql);

                while (rs.next()) {
                    String kodeKaos = rs.getString("kode_varian");
                    String namaKaos = rs.getString("nama_kaos");
                    String merek = rs.getString("merek");
                    String type = rs.getString("type_kaos");
                    String warna = rs.getString("warna");
                    String size = rs.getString("size");
                    String kategori = rs.getString("nama_kategori");
                    long harga = rs.getLong("harga");
                    int stok = rs.getInt("stok");

                    // Prefer varian photo, fallback to master
                    // Prefer varian photo (Specific Color) -> Fallback to master
                    String fotoPath = rs.getString("foto_varian");
                    if (fotoPath == null || fotoPath.isEmpty()) {
                        fotoPath = rs.getString("foto_utama");
                    }

                    ImageIcon imgIcon = util.ImageHelper.loadImage(fotoPath, 50, 50);

                    // Handle null values
                    if (kodeKaos == null || kodeKaos.trim().isEmpty())
                        kodeKaos = "-";
                    if (namaKaos == null)
                        namaKaos = "-";
                    if (merek == null)
                        merek = "-";
                    if (type == null)
                        type = "-";
                    if (warna == null)
                        warna = "-";
                    if (size == null)
                        size = "-";
                    if (kategori == null)
                        kategori = "Tanpa Kategori";

                    // Tentukan status stok
                    String statusStok;
                    if (stok <= 0) {
                        statusStok = "HABIS";
                    } else if (stok <= 5) {
                        statusStok = "RENDAH";
                    } else {
                        statusStok = "TERSEDIA";
                    }

                    final Object[] rowData = new Object[] {
                            kodeKaos,
                            namaKaos,
                            merek,
                            type,
                            warna,
                            size,
                            kategori,
                            "Rp " + df.format(harga),
                            stok,
                            statusStok,
                            imgIcon // IMG
                    };

                    SwingUtilities.invokeLater(() -> model.addRow(rowData));
                }

                System.out.println("✅ Data loaded.");

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "❌ Gagal memuat data kaos!\n" + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    // ================= LOAD KATEGORI UNTUK FILTER =================
    private void loadKategoriForFilter() {
        try (Connection conn = getConnection()) {
            String sql = "SELECT DISTINCT nama_kategori FROM kategori ORDER BY nama_kategori ASC";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                String kategori = rs.getString("nama_kategori");
                if (kategori != null) {
                    cbKategoriFilter.addItem(kategori);
                }
            }

            // Tambah opsi untuk kaos tanpa kategori
            cbKategoriFilter.addItem("Tanpa Kategori");

        } catch (Exception e) {
            System.out.println("❌ Gagal memuat kategori: " + e.getMessage());
            cbKategoriFilter.addItem("Unisex");
            cbKategoriFilter.addItem("Pria");
            cbKategoriFilter.addItem("Wanita");
        }
    }

    // ================= SHOW DETAIL KAOS (Read-Only) =================
    private void showDetailKaos() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "⚠️ Pilih kaos untuk melihat detail!");
            return;
        }

        String kodeVarian = (String) model.getValueAt(table.convertRowIndexToModel(row), 0);

        try (Connection conn = getConnection()) {
            String sql = "SELECT kv.kode_varian, km.nama_kaos, km.merek, km.type_kaos, " +
                    "kv.warna, kv.size, kat.nama_kategori, kv.harga, kv.harga_pokok, kv.stok, " +
                    "kv.foto_varian, km.foto_utama " +
                    "FROM KAOS_VARIAN kv " +
                    "JOIN KAOS_MASTER km ON kv.kaos_master_id = km.id " +
                    "LEFT JOIN kategori kat ON km.kategori_id = kat.id " +
                    "WHERE kv.kode_varian = ?";

            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, kodeVarian);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                JDialog dialog = new JDialog(this, "📋 Detail Kaos", true);
                dialog.setLayout(new BorderLayout());
                dialog.setSize(450, 500);
                dialog.setLocationRelativeTo(this);

                JPanel panel = new JPanel(new GridBagLayout());
                panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, 5, 5, 5);
                gbc.anchor = GridBagConstraints.WEST;

                int rowNum = 0;
                addDetailRow(panel, gbc, "Kode Varian:", rs.getString("kode_varian"), rowNum++);
                addDetailRow(panel, gbc, "Nama Kaos:", rs.getString("nama_kaos"), rowNum++);
                addDetailRow(panel, gbc, "Merek:", rs.getString("merek") != null ? rs.getString("merek") : "-",
                        rowNum++);
                addDetailRow(panel, gbc, "Type:", rs.getString("type_kaos") != null ? rs.getString("type_kaos") : "-",
                        rowNum++);
                addDetailRow(panel, gbc, "Warna:", rs.getString("warna") != null ? rs.getString("warna") : "-",
                        rowNum++);
                addDetailRow(panel, gbc, "Size:", rs.getString("size") != null ? rs.getString("size") : "-", rowNum++);
                addDetailRow(panel, gbc, "Kategori:",
                        rs.getString("nama_kategori") != null ? rs.getString("nama_kategori") : "Tanpa Kategori",
                        rowNum++);
                addDetailRow(panel, gbc, "Harga Jual:", "Rp " + df.format(rs.getLong("harga")), rowNum++);
                // Harga Pokok usually hidden for cashier, but let's keep it if logic requires,
                // or maybe hide it?
                // The original code showed it, so I will keep it but maybe we should review if
                // cashier should see it.
                // Assuming "DaftarKaos" is for Kasir, maybe hide pokok?
                // addDetailRow(panel, gbc, "Harga Pokok:", "Rp " +
                // df.format(rs.getLong("harga_pokok")), rowNum++);

                addDetailRow(panel, gbc, "Stok:", String.valueOf(rs.getInt("stok")), rowNum++);

                // Tentukan status stok
                int stok = rs.getInt("stok");
                String statusStok;
                if (stok <= 0) {
                    statusStok = "❌ HABIS";
                } else if (stok <= 5) {
                    statusStok = "⚠️ RENDAH";
                } else {
                    statusStok = "✅ TERSEDIA";
                }
                addDetailRow(panel, gbc, "Status Stok:", statusStok, rowNum++);

                String fotoPath = rs.getString("foto_varian");
                if (fotoPath == null || fotoPath.isEmpty()) {
                    fotoPath = rs.getString("foto_utama");
                }

                if (fotoPath != null && !fotoPath.trim().isEmpty()) {
                    try {
                        ImageIcon icon = util.ImageHelper.loadImage(fotoPath, 120, 150);
                        gbc.gridx = 0;
                        gbc.gridy = rowNum++;
                        gbc.gridwidth = 2;
                        gbc.anchor = GridBagConstraints.CENTER;
                        panel.add(new JLabel("Foto:"), gbc);

                        gbc.gridy = rowNum++;
                        JLabel lblFoto = new JLabel(icon);
                        lblFoto.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                        panel.add(lblFoto, gbc);
                    } catch (Exception e) {
                        System.out.println("Gagal load foto: " + e.getMessage());
                    }
                }

                JButton btnClose = new JButton("Tutup");
                styleButton(btnClose, GREEN_PRIMARY);
                btnClose.addActionListener(e -> dialog.dispose());

                JPanel btnPanel = new JPanel();
                btnPanel.add(btnClose);

                dialog.add(panel, BorderLayout.CENTER);
                dialog.add(btnPanel, BorderLayout.SOUTH);
                dialog.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Kaos tidak ditemukan!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "❌ Gagal memuat detail kaos!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================= HELPER METHODS =================
    private void addDetailRow(JPanel panel, GridBagConstraints gbc,
            String label, String value, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel lbl = new JLabel("<html><b>" + label + "</b></html>");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(val, gbc);
    }

    private JButton createIconButton(String iconName, String text, Color bgColor) {
        ImageIcon icon = createImageIcon(iconName, 16, 16);

        JButton btn = new JButton(text);
        if (icon != null) {
            btn.setIcon(icon);
            btn.setHorizontalTextPosition(SwingConstants.RIGHT);
            btn.setIconTextGap(8);
        }

        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 35));
        btn.setHorizontalAlignment(SwingConstants.CENTER);

        // Custom paint dengan rounded corners (Flat Design - No 3D)
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, javax.swing.JComponent c) {
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

    private void styleButton(JButton btn, Color bgColor) {
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(100, 35));

        // Custom paint dengan rounded corners (Flat Design - No 3D)
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, javax.swing.JComponent c) {
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

    // IMAGE METHODS
    // Logic moved to util.ImageHelper to ensure consistent image loading across
    // Desktop app (and sync with Web)
    // No local implementations needed here.

    // ================= MAIN METHOD =================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            DaftarKaos frame = new DaftarKaos(2, "Kasir Utama", "KSR001");
            frame.setVisible(true);
        });
    }
}