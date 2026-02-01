import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.*;
// import java.util.HashMap; // Removed unused import

public class KaryawanManagement extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private Integer selectedId = null;
    private String adminName;
    private String userCode;
    private int userId;

    // WARNA THEME DISTRO ZONE - EMERALD GREEN (Sesuai Web)
    private static final Color GREEN_PRIMARY = new Color(16, 185, 129); // #10B981
    private static final Color GREEN_DARK = new Color(15, 118, 110); // #0F766E
    private static final Color GREEN_LIGHT = new Color(52, 211, 153); // #34D399
    private static final Color TEXT_DARK = new Color(31, 41, 55); // #1F2937
    private static final Color HEADER_GRAY = new Color(100, 116, 139); // #64748B

    // Base path untuk web server (sesuaikan dengan lokasi XAMPP htdocs)
    private static final String WEB_BASE_PATH = "C:/xampp/htdocs/distrozoneweb/";

    public KaryawanManagement(int userId, String adminName, String userCode) {
        this.userId = userId;
        this.adminName = adminName;
        this.userCode = userCode;

        setTitle("👤 Manajemen Karyawan - Distro Zone");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();
        loadData();
    }

    private void initUI() {
        // ================= PANEL UTAMA DENGAN GRADIENT =================
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
        mainPanel.setLayout(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        // ================= HEADER DENGAN ICON =================
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        headerPanel.setOpaque(false);

        ImageIcon headerIcon = createImageIcon("user.png", 50, 50);
        if (headerIcon == null) {
            headerIcon = createUsersIcon(50, 50);
        }

        JLabel lblIcon = new JLabel(headerIcon);
        JLabel lblHeader = new JLabel("Manajemen Karyawan");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblHeader.setForeground(Color.WHITE);

        headerPanel.add(lblIcon);
        headerPanel.add(lblHeader);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ================= PANEL SEARCH =================
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchPanel.setOpaque(false);

        JTextField tfSearch = new JTextField();
        tfSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tfSearch.setPreferredSize(new Dimension(400, 35));
        tfSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREEN_DARK, 2),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        tfSearch.setToolTipText("Cari berdasarkan nama, username, atau role...");

        JButton btnCari = createIconButton("search.png", "Cari", GREEN_PRIMARY);
        btnCari.setPreferredSize(new Dimension(120, 35));

        searchPanel.add(new JLabel("Pencarian:"));
        searchPanel.add(tfSearch);
        searchPanel.add(btnCari);

        // ================= TABEL KARYAWAN =================
        model = new DefaultTableModel(
                new String[] { "ID", "Kode", "Username", "Nama", "Role", "Email", "No Telp", "NIK", "Alamat", "Shift",
                        "Status", "Foto" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 11)
                    return ImageIcon.class;
                return Object.class;
            }
        };

        table = new JTable(model);
        table.setRowHeight(60);
        table.setSelectionBackground(GREEN_PRIMARY);
        table.setSelectionForeground(Color.WHITE);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));

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

        // Sembunyikan kolom ID
        TableColumn idCol = table.getColumnModel().getColumn(0);
        idCol.setMinWidth(0);
        idCol.setMaxWidth(0);
        idCol.setPreferredWidth(0);

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
        table.getColumnModel().getColumn(11).setCellRenderer(imageRenderer);

        // Center align untuk beberapa kolom
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Kode
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Role
        table.getColumnModel().getColumn(9).setCellRenderer(centerRenderer); // Shift
        table.getColumnModel().getColumn(10).setCellRenderer(centerRenderer); // Status
        table.getColumnModel().getColumn(7).setCellRenderer(centerRenderer); // NIK

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(GREEN_DARK, 2, true));

        // ================= PANEL TOMBOL =================
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setOpaque(false);

        JButton btnTambah = createIconButton("add.png", "Tambah", GREEN_PRIMARY);
        JButton btnEdit = createIconButton("edit.png", "Edit", new Color(52, 152, 219));
        JButton btnDetail = createIconButton("detail.png", "Detail", new Color(155, 89, 182));
        JButton btnHapus = createIconButton("delete.png", "Hapus", new Color(231, 76, 60));
        JButton btnRefresh = createIconButton("refresh.png", "Refresh", new Color(46, 204, 113));
        JButton btnBack = createIconButton("back.png", "Kembali", new Color(153, 153, 153));

        btnPanel.add(btnTambah);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDetail);
        btnPanel.add(btnHapus);
        btnPanel.add(btnRefresh);
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
        btnTambah.addActionListener(e -> {
            table.clearSelection();
            selectedId = null;
            tambahKaryawan();
        });

        btnEdit.addActionListener(e -> editKaryawan());
        btnDetail.addActionListener(e -> showDetailKaryawan());
        btnHapus.addActionListener(e -> hapusKaryawan());

        btnRefresh.addActionListener(e -> {
            tfSearch.setText("");
            loadData();
        });

        btnBack.addActionListener(e -> {
            dispose();
            new AdminDashboard(userId, adminName, userCode).setVisible(true);
        });

        // Search functionality
        btnCari.addActionListener(e -> filterTable(tfSearch.getText().trim()));

        tfSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filterTable(tfSearch.getText().trim());
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filterTable(tfSearch.getText().trim());
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filterTable(tfSearch.getText().trim());
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
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showDetailKaryawan();
                }
            }
        });
    }

    private void filterTable(String searchText) {
        if (searchText.isEmpty()) {
            table.setRowSorter(null);
        } else {
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText, 2, 3, 4)); // Username, Nama, Role
        }
    }

    /**
     * Menyelesaikan path gambar - mengkonversi path relatif dari web ke path
     * absolut.
     * Path dari web biasanya seperti: assets/uploads/users/filename.jpg
     * Path dari desktop biasanya sudah absolut: C:\path\to\file.jpg
     */
    private String resolveImagePath(String path) {
        if (path == null || path.isEmpty())
            return null;

        // Jika sudah path absolut (Windows atau Unix), langsung return
        if (path.startsWith("/") || path.contains(":")) {
            return path;
        }

        // Jika path relatif (dari web), konversi ke path absolut dengan web base path
        // Hapus leading slash jika ada
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        String fullPath = WEB_BASE_PATH + cleanPath;

        // Normalisasi path separator untuk Windows
        fullPath = fullPath.replace("/", File.separator).replace("\\", File.separator);

        System.out.println("📂 Resolving image path: " + path + " -> " + fullPath);
        return fullPath;
    }

    /**
     * Mengekstrak nama shift pendek dari deskripsi lengkap.
     * Contoh: "Shift 1 (08:00-16:00)" -> "Shift 1"
     */
    private String extractShortShift(String fullShift) {
        if (fullShift == null || fullShift.isEmpty())
            return null;

        // Jika mengandung kurung, ambil bagian sebelum kurung
        int parenIndex = fullShift.indexOf('(');
        if (parenIndex > 0) {
            return fullShift.substring(0, parenIndex).trim();
        }

        // Jika sudah pendek, langsung return
        return fullShift.trim();
    }

    // ================= DATABASE CONNECTION =================
    private Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/distrozone_db";
        String user = "root";
        String password = "";
        return DriverManager.getConnection(url, user, password);
    }

    // ================= LOAD DATA =================
    private void loadData() {
        model.setRowCount(0);
        selectedId = null;

        try (Connection conn = getConnection()) {
            String sql = "SELECT id, user_code, username, nama, role, email, no_telp, shift, status, foto, nik, alamat "
                    +
                    "FROM users WHERE role != 'customer' ORDER BY id DESC";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt("id");
                String userCode = rs.getString("user_code");
                String username = rs.getString("username");
                String nama = rs.getString("nama");
                String role = rs.getString("role");
                String email = rs.getString("email");
                String noTelp = rs.getString("no_telp");
                String shift = rs.getString("shift");
                String status = rs.getString("status");
                String fotoPath = rs.getString("foto");
                String nik = rs.getString("nik");
                String alamat = rs.getString("alamat");

                // Handle null values
                if (email == null)
                    email = "-";
                if (noTelp == null)
                    noTelp = "-";
                if (shift == null)
                    shift = "-";
                if (status == null)
                    status = "active";
                if (nik == null)
                    nik = "-";
                if (alamat == null)
                    alamat = "-";

                // Handle foto - resolve path dari web jika perlu
                ImageIcon imgIcon = createPlaceholderUserIcon();
                if (fotoPath != null && !fotoPath.trim().isEmpty()) {
                    try {
                        String resolvedPath = resolveImagePath(fotoPath);
                        File file = new File(resolvedPath);
                        if (file.exists()) {
                            Image img = new ImageIcon(resolvedPath).getImage()
                                    .getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                            imgIcon = new ImageIcon(img);
                        } else {
                            System.out.println("⚠️ File foto tidak ditemukan: " + resolvedPath);
                        }
                    } catch (Exception ex) {
                        System.out.println("❌ Gagal load foto: " + ex.getMessage());
                    }
                }

                model.addRow(new Object[] {
                        id,
                        userCode,
                        username,
                        nama,
                        role.toUpperCase(),
                        email,
                        noTelp,
                        nik,
                        alamat,
                        shift,
                        status.equalsIgnoreCase("active") ? "AKTIF" : "NONAKTIF",
                        imgIcon
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "❌ Gagal memuat data karyawan!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================= CRUD OPERATIONS =================
    private void tambahKaryawan() {
        JDialog dialog = new JDialog(this, "➕ Tambah Karyawan Baru", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 700);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Fields
        JTextField tfNama = new JTextField(20);
        util.InputValidator.restrictToText(tfNama); // Name text only
        JTextField tfUsername = new JTextField(20);

        JPasswordField pfPassword = new JPasswordField();
        JCheckBox cbShowPassword = new JCheckBox("Show Password");

        JTextField tfNIK = new JTextField(20);
        util.InputValidator.restrictToNumbersAndLength(tfNIK, 16); // NIK max 16 chars

        JTextField tfEmail = new JTextField(20);
        JTextField tfNoTelp = new JTextField(20);
        util.InputValidator.restrictToNumbersAndLength(tfNoTelp, 13); // Phone max 13 chars
        JTextArea taAlamat = new JTextArea(3, 20);
        taAlamat.setLineWrap(true);
        taAlamat.setWrapStyleWord(true);
        JScrollPane scrollAlamat = new JScrollPane(taAlamat);
        scrollAlamat.setPreferredSize(new Dimension(200, 60));

        String[] roleOptions = { "admin", "kasir" };
        JComboBox<String> cbRole = new JComboBox<>(roleOptions);

        String[] shiftOptions = { "Shift 1 (08:00-16:00)", "Shift 2 (16:00-24:00)", "Shift 3 (00:00-08:00)", "24 Jam" };
        JComboBox<String> cbShift = new JComboBox<>(shiftOptions);

        String[] statusOptions = { "active", "inactive" };
        JComboBox<String> cbStatus = new JComboBox<>(statusOptions);

        JLabel lblFoto = new JLabel();
        lblFoto.setPreferredSize(new Dimension(150, 150));
        lblFoto.setHorizontalAlignment(JLabel.CENTER);
        lblFoto.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        final String[] pathFoto = { null };

        JButton btnPilihFoto = new JButton("📁 Pilih Foto");
        btnPilihFoto.setBackground(GREEN_LIGHT);
        btnPilihFoto.setForeground(Color.WHITE);
        btnPilihFoto.setFocusPainted(false);

        // Action untuk show/hide password
        cbShowPassword.addActionListener(e -> {
            if (cbShowPassword.isSelected()) {
                pfPassword.setEchoChar((char) 0);
            } else {
                pfPassword.setEchoChar('•');
            }
        });

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

        // Add components
        int row = 0;
        addLabelAndField(panel, gbc, "Nama Lengkap*:", tfNama, row++);
        addLabelAndField(panel, gbc, "Username*:", tfUsername, row++);
        addLabelAndPassword(panel, gbc, "Password*:", pfPassword, cbShowPassword, row++);
        addLabelAndField(panel, gbc, "NIK:", tfNIK, row++);
        addLabelAndCombo(panel, gbc, "Role*:", cbRole, row++);
        addLabelAndField(panel, gbc, "Email:", tfEmail, row++);
        addLabelAndField(panel, gbc, "No Telp:", tfNoTelp, row++);

        // Alamat
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Alamat:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(scrollAlamat, gbc);

        addLabelAndCombo(panel, gbc, "Shift:", cbShift, row++);
        addLabelAndCombo(panel, gbc, "Status*:", cbStatus, row++);

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
        JButton btnSimpan = new JButton("Simpan");
        JButton btnBatal = new JButton("Batal");

        styleButton(btnSimpan, GREEN_PRIMARY);
        styleButton(btnBatal, new Color(153, 153, 153));

        btnSimpan.addActionListener(e -> {
            if (validateInput(tfNama, tfUsername, pfPassword)) {
                // Additional validations
                String nik = tfNIK.getText().trim();
                String email = tfEmail.getText().trim();
                String telp = tfNoTelp.getText().trim();

                if (!nik.isEmpty() && nik.length() != 16) {
                    JOptionPane.showMessageDialog(dialog, "NIK harus 16 digit!");
                    return;
                }
                if (!email.isEmpty() && !util.InputValidator.isValidEmail(email)) {
                    JOptionPane.showMessageDialog(dialog, "Email tidak valid (harus mengandung '@')!");
                    return;
                }
                if (!telp.isEmpty() && (telp.length() < 10 || telp.length() > 13)) {
                    JOptionPane.showMessageDialog(dialog, "Nomor Telepon harus 10-13 digit!");
                    return;
                }

                String password = new String(pfPassword.getPassword()); // Plain text
                simpanKaryawan(dialog, tfNama.getText().trim(), tfUsername.getText().trim(),
                        password, nik,
                        cbRole.getSelectedItem().toString(), email,
                        telp, taAlamat.getText().trim(),
                        cbShift.getSelectedItem().toString(), cbStatus.getSelectedItem().toString(),
                        pathFoto[0]);
            }
        });

        btnBatal.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnSimpan);
        btnPanel.add(btnBatal);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void editKaryawan() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "⚠️ Pilih karyawan yang akan diedit!");
            return;
        }

        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM users WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, selectedId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                showEditDialog(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "❌ Gagal memuat data karyawan!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showEditDialog(ResultSet rs) throws SQLException {
        JDialog dialog = new JDialog(this, "✏️ Edit Karyawan", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 700); // Lebih tinggi untuk menampung foto di bawah
        dialog.setLocationRelativeTo(this);

        // Panel utama
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel untuk form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        // Fields with current values
        JTextField tfUserCode = new JTextField(rs.getString("user_code"));
        tfUserCode.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tfUserCode.setPreferredSize(new Dimension(250, 25));

        JTextField tfNama = new JTextField(rs.getString("nama"), 20);
        tfNama.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tfNama.setPreferredSize(new Dimension(250, 25));
        util.InputValidator.restrictToText(tfNama);

        JTextField tfUsername = new JTextField(rs.getString("username"), 20);
        tfUsername.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tfUsername.setPreferredSize(new Dimension(250, 25));

        // ======= FIX: Password Field dengan ukuran yang tepat =======
        JPasswordField pfPassword = new JPasswordField();
        pfPassword.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pfPassword.setText("");
        pfPassword.setPreferredSize(new Dimension(250, 25)); // FIX: Set preferred size

        JCheckBox cbShowPassword = new JCheckBox("Tampilkan Password");
        cbShowPassword.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JLabel lblPasswordInfo = new JLabel("<html><small>Kosongkan jika tidak ingin mengubah password</small></html>");
        lblPasswordInfo.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblPasswordInfo.setForeground(Color.GRAY);

        JTextField tfNIK = new JTextField(rs.getString("nik") != null ? rs.getString("nik") : "", 20);
        tfNIK.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tfNIK.setPreferredSize(new Dimension(250, 25));
        util.InputValidator.restrictToNumbersAndLength(tfNIK, 16);
        util.InputValidator.restrictToNumbersAndLength(tfNIK, 16);

        JTextField tfEmail = new JTextField(rs.getString("email") != null ? rs.getString("email") : "", 20);
        tfEmail.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tfEmail.setPreferredSize(new Dimension(250, 25));

        JTextField tfNoTelp = new JTextField(rs.getString("no_telp") != null ? rs.getString("no_telp") : "", 20);
        tfNoTelp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tfNoTelp.setPreferredSize(new Dimension(250, 25));
        util.InputValidator.restrictToNumbersAndLength(tfNoTelp, 13);
        util.InputValidator.restrictToNumbersAndLength(tfNoTelp, 13);

        JTextArea taAlamat = new JTextArea(3, 20);
        taAlamat.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        taAlamat.setLineWrap(true);
        taAlamat.setWrapStyleWord(true);
        taAlamat.setText(rs.getString("alamat") != null ? rs.getString("alamat") : "");
        JScrollPane scrollAlamat = new JScrollPane(taAlamat);
        scrollAlamat.setPreferredSize(new Dimension(250, 50)); // FIX: Ukuran konsisten

        String[] roleOptions = { "admin", "kasir" };
        JComboBox<String> cbRole = new JComboBox<>(roleOptions);
        cbRole.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbRole.setSelectedItem(rs.getString("role"));
        cbRole.setPreferredSize(new Dimension(250, 25));

        String[] shiftOptions = { "Shift 1 (08:00-16:00)", "Shift 2 (16:00-24:00)", "Shift 3 (00:00-08:00)", "24 Jam" };
        JComboBox<String> cbShift = new JComboBox<>(shiftOptions);
        cbShift.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        String shift = rs.getString("shift");
        if (shift != null) {
            for (int i = 0; i < shiftOptions.length; i++) {
                if (shiftOptions[i].startsWith(shift)) {
                    cbShift.setSelectedIndex(i);
                    break;
                }
            }
        }
        cbShift.setPreferredSize(new Dimension(250, 25));

        String[] statusOptions = { "active", "inactive" };
        JComboBox<String> cbStatus = new JComboBox<>(statusOptions);
        cbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbStatus.setSelectedItem(rs.getString("status"));
        cbStatus.setPreferredSize(new Dimension(250, 25));

        // Add components ke form
        int row = 0;
        addFormRow(formPanel, gbc, "Kode User:", tfUserCode, row++);
        tfUserCode.setEditable(false);

        addFormRow(formPanel, gbc, "Nama Lengkap*:", tfNama, row++);
        addFormRow(formPanel, gbc, "Username*:", tfUsername, row++);

        // ======= FIX: Password dengan layout yang lebih baik =======
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(lblPassword, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(pfPassword, gbc);
        gbc.weightx = 0;
        row++;

        // Checkbox show password
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(cbShowPassword, gbc);
        row++;

        // Password info
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        formPanel.add(lblPasswordInfo, gbc);
        gbc.gridwidth = 1;
        row++;

        addFormRow(formPanel, gbc, "NIK:", tfNIK, row++);
        addFormRow(formPanel, gbc, "Role*:", cbRole, row++);
        addFormRow(formPanel, gbc, "Email:", tfEmail, row++);
        addFormRow(formPanel, gbc, "No Telp:", tfNoTelp, row++);

        // Alamat
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel lblAlamat = new JLabel("Alamat:");
        lblAlamat.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(lblAlamat, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(scrollAlamat, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        row++;

        addFormRow(formPanel, gbc, "Shift:", cbShift, row++);
        addFormRow(formPanel, gbc, "Status*:", cbStatus, row++);

        mainPanel.add(formPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15))); // Spacer

        // Panel untuk foto (di bawah form)
        JPanel fotoPanel = new JPanel();
        fotoPanel.setLayout(new BoxLayout(fotoPanel, BoxLayout.Y_AXIS));
        fotoPanel.setOpaque(false);

        JLabel lblFotoTitle = new JLabel("Foto Karyawan");
        lblFotoTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFotoTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        fotoPanel.add(lblFotoTitle);
        fotoPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        imagePanel.setOpaque(false);

        JLabel lblFoto = new JLabel();
        lblFoto.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1)); // Border abu-abu
        lblFoto.setBackground(Color.WHITE);
        lblFoto.setOpaque(true);
        final String[] pathFoto = { rs.getString("foto") };

        // Load current image if exists
        if (pathFoto[0] != null && !pathFoto[0].trim().isEmpty()) {
            try {
                File file = new File(pathFoto[0]);
                if (file.exists()) {
                    ImageIcon icon = new ImageIcon(pathFoto[0]);
                    Image img = icon.getImage();
                    // Proporsi vertikal
                    int width = 120;
                    int height = 150;
                    Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    lblFoto.setIcon(new ImageIcon(scaledImg));
                } else {
                    lblFoto.setIcon(createPlaceholderUserIcon());
                }
            } catch (Exception e) {
                System.out.println("Gagal load foto: " + e.getMessage());
                lblFoto.setIcon(createPlaceholderUserIcon());
            }
        } else {
            lblFoto.setIcon(createPlaceholderUserIcon());
        }

        imagePanel.add(lblFoto);
        fotoPanel.add(imagePanel);
        fotoPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton btnPilihFoto = new JButton("📁 Ganti Foto");
        btnPilihFoto.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPilihFoto.setBackground(GREEN_LIGHT);
        btnPilihFoto.setForeground(Color.WHITE);
        btnPilihFoto.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnPilihFoto.setFocusPainted(false);
        btnPilihFoto.setPreferredSize(new Dimension(120, 30));
        btnPilihFoto.setMaximumSize(new Dimension(120, 30));

        btnPilihFoto.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Gambar (*.jpg, *.jpeg, *.png)", "jpg", "jpeg", "png"));
            int res = chooser.showOpenDialog(dialog);
            if (res == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                pathFoto[0] = file.getAbsolutePath();
                ImageIcon icon = new ImageIcon(pathFoto[0]);
                Image img = icon.getImage();
                // Proporsi vertikal
                int width = 120;
                int height = 150;
                Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                lblFoto.setIcon(new ImageIcon(scaledImg));
            }
        });

        fotoPanel.add(btnPilihFoto);
        mainPanel.add(fotoPanel);

        // Action untuk show/hide password
        cbShowPassword.addActionListener(e -> {
            if (cbShowPassword.isSelected()) {
                pfPassword.setEchoChar((char) 0);
            } else {
                pfPassword.setEchoChar('•');
            }
        });

        // Button panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton btnUpdate = new JButton("Update");
        JButton btnBatal = new JButton("Batal");

        styleButton(btnUpdate, GREEN_PRIMARY);
        styleButton(btnBatal, new Color(153, 153, 153));

        btnUpdate.addActionListener(e -> {
            if (validateInput(tfNama, tfUsername, null)) {
                // Additional validations
                String nik = tfNIK.getText().trim();
                String email = tfEmail.getText().trim();
                String telp = tfNoTelp.getText().trim();

                if (!nik.isEmpty() && nik.length() != 16) {
                    JOptionPane.showMessageDialog(dialog, "NIK harus 16 digit!");
                    return;
                }
                if (!email.isEmpty() && !util.InputValidator.isValidEmail(email)) {
                    JOptionPane.showMessageDialog(dialog, "Email tidak valid (harus mengandung '@')!");
                    return;
                }
                if (!telp.isEmpty() && (telp.length() < 10 || telp.length() > 13)) {
                    JOptionPane.showMessageDialog(dialog, "Nomor Telepon harus 10-13 digit!");
                    return;
                }

                String password = null;
                if (pfPassword.getPassword().length > 0) {
                    password = new String(pfPassword.getPassword()); // Plain text
                }

                updateKaryawan(dialog, selectedId, tfNama.getText().trim(), tfUsername.getText().trim(),
                        password, nik,
                        cbRole.getSelectedItem().toString(), email,
                        telp, taAlamat.getText().trim(),
                        cbShift.getSelectedItem().toString(), cbStatus.getSelectedItem().toString(),
                        pathFoto[0]);
            }
        });

        btnBatal.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnUpdate);
        btnPanel.add(btnBatal);

        // Tambah scroll pane
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // Helper method untuk form row dengan ukuran yang konsisten
    private void addFormRow(JPanel panel, GridBagConstraints gbc,
            String label, JComponent component, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        // Set preferred size untuk konsistensi
        if (component instanceof JTextField || component instanceof JComboBox) {
            component.setPreferredSize(new Dimension(250, 25));
        }
        panel.add(component, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0;
    }

    private void showDetailKaryawan() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "⚠️ Pilih karyawan untuk melihat detail!");
            return;
        }

        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM users WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, selectedId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                JDialog dialog = new JDialog(this, "📋 Detail Karyawan", true);
                dialog.setLayout(new BorderLayout());
                dialog.setSize(500, 550);
                dialog.setLocationRelativeTo(this);

                // Panel utama dengan BoxLayout vertical
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

                // Panel untuk data teks (grid layout)
                JPanel dataPanel = new JPanel(new GridBagLayout());
                dataPanel.setOpaque(false);
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(4, 4, 4, 4);
                gbc.anchor = GridBagConstraints.WEST;

                int row = 0;
                addDetailRowCompact(dataPanel, gbc, "Kode User:", rs.getString("user_code"), row++);
                addDetailRowCompact(dataPanel, gbc, "Username:", rs.getString("username"), row++);
                addDetailRowCompact(dataPanel, gbc, "Nama Lengkap:", rs.getString("nama"), row++);
                addDetailRowCompact(dataPanel, gbc, "NIK:", rs.getString("nik") != null ? rs.getString("nik") : "-",
                        row++);
                addDetailRowCompact(dataPanel, gbc, "Role:", rs.getString("role").toUpperCase(), row++);
                addDetailRowCompact(dataPanel, gbc, "Email:",
                        rs.getString("email") != null ? rs.getString("email") : "-", row++);
                addDetailRowCompact(dataPanel, gbc, "No Telp:",
                        rs.getString("no_telp") != null ? rs.getString("no_telp") : "-", row++);

                // Alamat
                gbc.gridx = 0;
                gbc.gridy = row++;
                gbc.gridwidth = 1;
                gbc.anchor = GridBagConstraints.NORTHWEST;
                dataPanel.add(new JLabel("<html><b>Alamat:</b></html>"), gbc);

                gbc.gridx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 1.0;
                JTextArea taAlamat = new JTextArea(2, 20);
                taAlamat.setText(rs.getString("alamat") != null ? rs.getString("alamat") : "-");
                taAlamat.setEditable(false);
                taAlamat.setLineWrap(true);
                taAlamat.setWrapStyleWord(true);
                taAlamat.setBackground(dataPanel.getBackground());
                taAlamat.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                JScrollPane scrollAlamat = new JScrollPane(taAlamat);
                scrollAlamat.setPreferredSize(new Dimension(250, 40));
                dataPanel.add(scrollAlamat, gbc);
                gbc.weightx = 0;
                row++;

                addDetailRowCompact(dataPanel, gbc, "Shift:",
                        rs.getString("shift") != null ? rs.getString("shift") : "-", row++);
                addDetailRowCompact(dataPanel, gbc, "Status:",
                        rs.getString("status").equalsIgnoreCase("active") ? "✅ AKTIF" : "❌ NONAKTIF", row++);

                // Tambahkan dataPanel ke panel utama
                panel.add(dataPanel);
                panel.add(Box.createRigidArea(new Dimension(0, 15))); // Spacer

                // Panel untuk foto (full width di bawah)
                String fotoPath = rs.getString("foto");
                JPanel fotoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                fotoPanel.setOpaque(false);

                if (fotoPath != null && !fotoPath.trim().isEmpty()) {
                    try {
                        File file = new File(fotoPath);
                        if (file.exists()) {
                            // Label foto
                            JLabel lblFotoTitle = new JLabel("Foto Karyawan");
                            lblFotoTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
                            fotoPanel.add(lblFotoTitle);

                            fotoPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer kecil

                            // Panel untuk gambar dengan border
                            JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                            imagePanel.setOpaque(false);

                            ImageIcon icon = new ImageIcon(fotoPath);
                            Image img = icon.getImage();
                            int width = 140;
                            int height = 170; // Lebih tinggi untuk proporsi vertikal
                            Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                            JLabel lblFoto = new JLabel(new ImageIcon(scaledImg));
                            lblFoto.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1)); // Border abu-abu
                            lblFoto.setBackground(Color.WHITE);
                            lblFoto.setOpaque(true);

                            imagePanel.add(lblFoto);
                            fotoPanel.add(imagePanel);
                        }
                    } catch (Exception e) {
                        System.out.println("Gagal load foto: " + e.getMessage());
                        JLabel lblNoFoto = new JLabel("❌ Foto tidak dapat ditampilkan");
                        lblNoFoto.setForeground(Color.RED);
                        fotoPanel.add(lblNoFoto);
                    }
                } else {
                    JLabel lblNoFoto = new JLabel("📷 Tidak ada foto");
                    lblNoFoto.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                    lblNoFoto.setForeground(Color.GRAY);
                    fotoPanel.add(lblNoFoto);
                }

                panel.add(fotoPanel);

                JButton btnClose = new JButton("Tutup");
                styleButton(btnClose, GREEN_PRIMARY);
                btnClose.addActionListener(e -> dialog.dispose());

                JPanel btnPanel = new JPanel();
                btnPanel.add(btnClose);

                // Tambahkan scroll pane jika konten terlalu panjang
                JScrollPane scrollPane = new JScrollPane(panel);
                scrollPane.setBorder(null);
                scrollPane.getVerticalScrollBar().setUnitIncrement(12);

                dialog.add(scrollPane, BorderLayout.CENTER);
                dialog.add(btnPanel, BorderLayout.SOUTH);
                dialog.setVisible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "❌ Gagal memuat detail karyawan!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method helper untuk detail row compact
    private void addDetailRowCompact(JPanel panel, GridBagConstraints gbc,
            String label, String value, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        JLabel lbl = new JLabel("<html><b>" + label + "</b></html>");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11)); // Font lebih kecil
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 11)); // Font lebih kecil
        panel.add(val, gbc);
    }

    private void hapusKaryawan() {
        if (selectedId == null) {
            JOptionPane.showMessageDialog(this, "⚠️ Pilih karyawan yang akan dihapus!");
            return;
        }

        int row = table.getSelectedRow();
        String kodeUser = model.getValueAt(table.convertRowIndexToModel(row), 1).toString();
        String namaUser = model.getValueAt(table.convertRowIndexToModel(row), 3).toString();

        int confirm = JOptionPane.showConfirmDialog(this,
                "<html><b>Hapus Karyawan?</b><br><br>" +
                        "Kode: <b>" + kodeUser + "</b><br>" +
                        "Nama: <b>" + namaUser + "</b><br><br>" +
                        "Data yang dihapus tidak dapat dikembalikan!",
                "Konfirmasi Hapus",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION)
            return;

        try (Connection conn = getConnection()) {
            // Cek apakah karyawan pernah digunakan dalam transaksi
            String checkSql = "SELECT COUNT(*) FROM transaksi WHERE kasir_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, selectedId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this,
                        "❌ Karyawan tidak dapat dihapus!\n" +
                                "Karyawan ini sudah pernah melakukan transaksi.\n" +
                                "Untuk menjaga integritas data, hapus tidak diperbolehkan.",
                        "Karyawan Terpakai",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String sql = "DELETE FROM users WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, selectedId);
            int affectedRows = pst.executeUpdate();

            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(this, "✅ Karyawan berhasil dihapus!");
                loadData();
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "❌ Gagal menghapus karyawan!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================= HELPER METHODS =================
    private void simpanKaryawan(JDialog dialog, String nama, String username, String password,
            String nik, String role, String email, String noTelp,
            String alamat, String shift, String status, String fotoPath) {
        try (Connection conn = getConnection()) {
            String userCode = util.CodeGenerator.generateUserCode(conn, role);

            String sql = "INSERT INTO users (user_code, username, nama, password, role, email, no_telp, alamat, shift, status, foto, nik, created_at, updated_at) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, userCode);
            pst.setString(2, username);
            pst.setString(3, nama);
            pst.setString(4, password); // Password disimpan plain text sesuai request
            pst.setString(5, role);
            pst.setString(6, email.isEmpty() ? null : email);
            pst.setString(7, noTelp.isEmpty() ? null : noTelp);
            pst.setString(8, alamat.isEmpty() ? null : alamat);
            pst.setString(9, shift);
            pst.setString(10, status);
            pst.setString(11, fotoPath);
            pst.setString(12, nik.isEmpty() ? null : nik);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(dialog,
                    "✅ Karyawan berhasil ditambahkan!\n" +
                            "Kode: " + userCode + "\n" +
                            "Username: " + username);

            dialog.dispose();
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialog,
                    "❌ Gagal menambah karyawan!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateKaryawan(JDialog dialog, int id, String nama, String username, String password,
            String nik, String role, String email, String noTelp,
            String alamat, String shift, String status, String fotoPath) {
        try (Connection conn = getConnection()) {
            String sql;
            PreparedStatement pst;

            // Ambil role lama untuk mempertahankan kode
            String oldRole = "";
            String oldUserCode = "";
            String getRoleSql = "SELECT role, user_code FROM users WHERE id = ?";
            PreparedStatement getRoleStmt = conn.prepareStatement(getRoleSql);
            getRoleStmt.setInt(1, id);
            ResultSet rs = getRoleStmt.executeQuery();
            if (rs.next()) {
                oldRole = rs.getString("role");
                oldUserCode = rs.getString("user_code");
            }

            // Jika role berubah, generate kode baru
            String newUserCode = oldUserCode;
            if (!oldRole.equals(role)) {
                newUserCode = util.CodeGenerator.generateUserCode(conn, role);
            }

            if (password == null || password.isEmpty()) {
                // Update tanpa mengubah password
                sql = "UPDATE users SET user_code=?, username=?, nama=?, role=?, email=?, no_telp=?, alamat=?, shift=?, status=?, "
                        +
                        "foto=COALESCE(?, foto), nik=?, updated_at=NOW() WHERE id=?";
                pst = conn.prepareStatement(sql);
                pst.setString(1, newUserCode);
                pst.setString(2, username);
                pst.setString(3, nama);
                pst.setString(4, role);
                pst.setString(5, email.isEmpty() ? null : email);
                pst.setString(6, noTelp.isEmpty() ? null : noTelp);
                pst.setString(7, alamat.isEmpty() ? null : alamat);
                pst.setString(8, shift);
                pst.setString(9, status);
                pst.setString(10, fotoPath);
                pst.setString(11, nik.isEmpty() ? null : nik);
                pst.setInt(12, id);
            } else {
                // Update dengan password baru
                sql = "UPDATE users SET user_code=?, username=?, nama=?, password=?, role=?, email=?, no_telp=?, alamat=?, shift=?, status=?, "
                        +
                        "foto=COALESCE(?, foto), nik=?, updated_at=NOW() WHERE id=?";
                pst = conn.prepareStatement(sql);
                pst.setString(1, newUserCode);
                pst.setString(2, username);
                pst.setString(3, nama);
                pst.setString(4, password);
                pst.setString(5, role);
                pst.setString(6, email.isEmpty() ? null : email);
                pst.setString(7, noTelp.isEmpty() ? null : noTelp);
                pst.setString(8, alamat.isEmpty() ? null : alamat);
                pst.setString(9, shift);
                pst.setString(10, status);
                pst.setString(11, fotoPath);
                pst.setString(12, nik.isEmpty() ? null : nik);
                pst.setInt(13, id);
            }

            pst.executeUpdate();

            JOptionPane.showMessageDialog(dialog, "✅ Karyawan berhasil diupdate!");
            dialog.dispose();
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialog,
                    "❌ Gagal update karyawan!\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // internal helper generated removed in favor of util.CodeGenerator

    private boolean validateInput(JTextField tfNama, JTextField tfUsername, JPasswordField pfPassword) {
        if (tfNama.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "❌ Nama lengkap harus diisi!");
            return false;
        }
        if (tfUsername.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "❌ Username harus diisi!");
            return false;
        }
        // Hanya validasi password untuk form tambah
        if (pfPassword != null && pfPassword.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, "❌ Password harus diisi!");
            return false;
        }
        return true;
    }

    // ================= UI HELPER METHODS =================
    private JButton createIconButton(String iconName, String text, Color bgColor) {
        // Load with fallback
        ImageIcon icon = createImageIcon(iconName, 16, 16);

        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isRollover()) {
                    g2.setColor(bgColor.darker());
                } else {
                    g2.setColor(bgColor);
                }

                // KOTAK (Square)
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.dispose();
                super.paintComponent(g);
            }
        };
        if (icon != null) {
            btn.setIcon(icon);
            btn.setHorizontalTextPosition(SwingConstants.RIGHT);
            btn.setIconTextGap(8);
        }

        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 35));
        btn.setHorizontalAlignment(SwingConstants.CENTER);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.repaint();
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.repaint();
            }
        });
        return btn;
    }

    private void styleButton(JButton btn, Color bgColor) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(100, 35));

        // Use a custom paintComponent for consistent styling
        // We override the button's UI or add a painter, but since we can't easily
        // override inline
        // without changing the caller, we will set properties and rely on a cleaner
        // look or
        // try to force a simple background if the custom paint is too complex to inject
        // here.
        // HOWEVER, the robust way is to use the same paintComponent approach as
        // dashboards.
        // Since we cannot change the class of 'btn' here (it's passed in), we will just
        // set the background/opaque behavior carefully.

        btn.setOpaque(true);
        btn.setBackground(bgColor);
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(bgColor.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(bgColor);
            }
        });
    }

    private void addLabelAndField(JPanel panel, GridBagConstraints gbc,
            String label, JTextField field, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private void addLabelAndPassword(JPanel panel, GridBagConstraints gbc,
            String label, JPasswordField passwordField,
            JCheckBox showPasswordCheck, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(passwordField, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 1;
        panel.add(showPasswordCheck, gbc);
    }

    private void addLabelAndCombo(JPanel panel, GridBagConstraints gbc,
            String label, JComboBox<String> combo, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        panel.add(combo, gbc);
    }

    private void addDetailRow(JPanel panel, GridBagConstraints gbc,
            String label, String value, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        panel.add(new JLabel("<html><b>" + label + "</b></html>"), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        panel.add(new JLabel(value), gbc);
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

    private ImageIcon createUsersIcon(int width, int height) {
        // Try to load user group icon, fallback to createPlaceholderUserIcon
        ImageIcon icon = createImageIcon("users.png", width, height);
        if (icon != null)
            return icon;

        return createPlaceholderUserIcon();
    }

    private ImageIcon createPlaceholderUserIcon() {
        // Try to load single user icon
        ImageIcon icon = createImageIcon("user.png", 50, 50);
        if (icon != null)
            return icon;

        int width = 50;
        int height = 50;

        // Simple fallback circle
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(width, height,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(GREEN_LIGHT);
        g2d.fillOval(0, 0, width, height);
        g2d.dispose();
        return new ImageIcon(image);
    }

    // ================= MAIN METHOD =================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            KaryawanManagement frame = new KaryawanManagement(1, "Admin", "ADM001");
            frame.setVisible(true);
        });
    }
}