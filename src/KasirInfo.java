import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;

public class KasirInfo extends JFrame {
    private int userId;
    private String namaUser;
    private String userCode;
    private Connection conn;

    // Warna tema hijau Distro Zone (Emerald Green - Sesuai Web)
    private static final Color GREEN_PRIMARY = new Color(16, 185, 129); // #10B981
    private static final Color GREEN_DARK = new Color(15, 118, 110); // #0F766E
    private static final Color GREEN_LIGHT = new Color(52, 211, 153); // #34D399
    private static final Color TEXT_DARK = new Color(31, 41, 55); // #1F2937
    private static final Color TEXT_GRAY = new Color(100, 116, 139); // #64748B
    private static final Color BG_LIGHT = new Color(236, 253, 245); // #ECFDF5

    public KasirInfo(int userId, String namaUser, String userCode) {
        this.userId = userId;
        this.namaUser = namaUser;
        this.userCode = userCode;

        setTitle("👤 Info Kasir - " + namaUser);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        connectDatabase();
        initUI();
        loadKasirData();
    }

    private void connectDatabase() {
        try {
            String url = "jdbc:mysql://localhost:3306/distrozone_db";
            String user = "root";
            String password = "";
            conn = DriverManager.getConnection(url, user, password);
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
        // Main Panel dengan gradient background
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
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        JLabel lblTitle = new JLabel(" Info Profil Kasir", JLabel.CENTER);
        ImageIcon headerIcon = loadImageIcon("user.png", 50); // Static Header Icon
        if (headerIcon != null)
            lblTitle.setIcon(headerIcon);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblTitle.setForeground(Color.WHITE);
        headerPanel.add(lblTitle, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ================= CONTENT PANEL =================
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 20, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        // Panel container untuk form (1 kotak saja - sesuai permintaan user)
        JPanel formContainer = new JPanel(new BorderLayout());
        formContainer.setOpaque(false);
        formContainer.setPreferredSize(new Dimension(700, 620));

        // ================= PANEL UTAMA: INFO PROFIL + DETAIL =================
        JPanel combinedPanel = createCombinedPanel();
        combinedPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREEN_DARK, 2),
                BorderFactory.createEmptyBorder(30, 30, 30, 30)));

        formContainer.add(combinedPanel, BorderLayout.CENTER);

        contentPanel.add(formContainer, gbc);

        // ================= BUTTON PANEL =================
        gbc.gridy = 1;
        gbc.insets = new Insets(20, 0, 0, 0);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setOpaque(false);

        JButton btnBack = createIconButton(" Kembali", TEXT_GRAY);
        ImageIcon backIcon = loadImageIcon("back.png", 20);
        if (backIcon != null)
            btnBack.setIcon(backIcon);

        btnBack.setPreferredSize(new Dimension(200, 45));
        btnBack.setFont(new Font("Segoe UI", Font.BOLD, 14));

        btnBack.addActionListener(e -> {
            closeDatabase();
            dispose();
            new KasirDashboard(userId, namaUser, userCode).setVisible(true);
        });

        buttonPanel.add(btnBack);

        contentPanel.add(buttonPanel, gbc);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel);
    }

    private JPanel createCombinedPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(new Color(255, 255, 255, 230));

        // Title
        JLabel lblTitle = new JLabel("INFO PROFIL KASIR", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(GREEN_DARK);
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        // Profile Section (Top)
        JPanel profileSection = new JPanel(new BorderLayout(0, 10));
        profileSection.setOpaque(false);

        // Profile Icon
        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        iconPanel.setOpaque(false);
        JLabel lblProfileIcon = new JLabel();
        ImageIcon userIcon = loadImageIcon("user.png", 80);
        if (userIcon != null) {
            lblProfileIcon.setIcon(userIcon);
        } else {
            lblProfileIcon.setText("👤");
            lblProfileIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
        }
        iconPanel.add(lblProfileIcon);

        // Basic Info (Nama, Kode, Status)
        JPanel basicInfoPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        basicInfoPanel.setOpaque(false);

        JLabel lblNama = new JLabel(namaUser, SwingConstants.CENTER);
        lblNama.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblNama.setForeground(TEXT_DARK);
        lblNama.setName("nama_lengkap");

        JLabel lblKode = new JLabel("Kode: " + userCode, SwingConstants.CENTER);
        lblKode.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblKode.setForeground(TEXT_GRAY);
        lblKode.setName("kode_user");

        JLabel lblStatus = new JLabel("Status: Loading...", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(100, 100, 100));
        lblStatus.setName("status");

        basicInfoPanel.add(lblNama);
        basicInfoPanel.add(lblKode);
        basicInfoPanel.add(lblStatus);

        profileSection.add(iconPanel, BorderLayout.NORTH);
        profileSection.add(basicInfoPanel, BorderLayout.CENTER);

        // Detail Info Section (Bottom)
        JPanel detailSection = new JPanel(new GridBagLayout());
        detailSection.setOpaque(false);
        detailSection.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(GREEN_LIGHT, 1),
                "Detail Informasi",
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                GREEN_DARK));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Row 1: Email
        gbc.gridx = 0;
        gbc.gridy = 0;
        detailSection.add(createInfoLabel("Email"), gbc);
        gbc.gridx = 1;
        detailSection.add(createInfoValue("", "email"), gbc);

        // Row 2: No. Telepon
        gbc.gridx = 0;
        gbc.gridy = 1;
        detailSection.add(createInfoLabel("No. Telepon"), gbc);
        gbc.gridx = 1;
        detailSection.add(createInfoValue("", "no_telp"), gbc);

        // Row 3: Alamat
        gbc.gridx = 0;
        gbc.gridy = 2;
        detailSection.add(createInfoLabel("Alamat"), gbc);
        gbc.gridx = 1;
        detailSection.add(createInfoValue("", "alamat"), gbc);

        // Row 4: Shift Kerja
        gbc.gridx = 0;
        gbc.gridy = 3;
        detailSection.add(createInfoLabel("Shift"), gbc);
        gbc.gridx = 1;
        detailSection.add(createInfoValue("", "shift"), gbc);

        // Row 5: Tanggal Bergabung
        gbc.gridx = 0;
        gbc.gridy = 4;
        detailSection.add(createInfoLabel("Bergabung"), gbc);
        gbc.gridx = 1;
        detailSection.add(createInfoValue("", "join_date"), gbc);

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(profileSection, BorderLayout.CENTER);
        panel.add(detailSection, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(new Color(255, 255, 255, 230));

        // Title
        JLabel lblTitle = new JLabel("INFO PROFIL", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(GREEN_DARK);
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Profile Icon - Simplified with Image
        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        iconPanel.setOpaque(false);
        iconPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

        JLabel lblProfileIcon = new JLabel();
        ImageIcon userIcon = loadImageIcon("user.png", 100); // Static User Icon
        if (userIcon != null) {
            lblProfileIcon.setIcon(userIcon);
        } else {
            // Fallback if image not found
            lblProfileIcon.setText("👤");
            lblProfileIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 80));
            lblProfileIcon.setForeground(GREEN_DARK);
        }

        iconPanel.add(lblProfileIcon);

        // Basic Info
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 0, 10));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel lblNama = new JLabel(namaUser, SwingConstants.CENTER);
        lblNama.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblNama.setForeground(TEXT_DARK);
        lblNama.setName("nama_lengkap");

        JLabel lblKode = new JLabel("Kode: " + userCode, SwingConstants.CENTER);
        lblKode.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblKode.setForeground(TEXT_GRAY);
        lblKode.setName("kode_user");

        JLabel lblStatus = new JLabel("Status: Loading...", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblStatus.setForeground(new Color(100, 100, 100));
        lblStatus.setName("status");

        infoPanel.add(lblNama);
        infoPanel.add(lblKode);
        infoPanel.add(lblStatus);

        // Stats Section
        JPanel statsPanel = createStatsPanel();
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(iconPanel, BorderLayout.CENTER);
        panel.add(infoPanel, BorderLayout.SOUTH);
        panel.add(statsPanel, BorderLayout.AFTER_LAST_LINE);

        return panel;
    }

    private JPanel createStatsPanel() {
        return new JPanel(); // Removed as per request ("jelek bgt")
    }

    private JPanel createStatCard(String icon, String title, String value) {
        return new JPanel(); // Removed
    }

    private JPanel createDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(new Color(255, 255, 255, 230));

        // Title
        JLabel lblTitle = new JLabel("📝 DETAIL INFORMASI", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(33, 150, 243));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Info Grid
        JPanel infoGrid = new JPanel(new GridBagLayout());
        infoGrid.setOpaque(false);
        infoGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 10, 12, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Row 1: Email
        gbc.gridx = 0;
        gbc.gridy = 0;
        infoGrid.add(createInfoLabel("📧 EMAIL"), gbc);

        gbc.gridx = 1;
        infoGrid.add(createInfoValue("", "email"), gbc);

        // Row 2: No. Telepon
        gbc.gridx = 0;
        gbc.gridy = 1;
        infoGrid.add(createInfoLabel("📱 NO. TELEPON"), gbc);

        gbc.gridx = 1;
        infoGrid.add(createInfoValue("", "no_telp"), gbc);

        // Row 3: Alamat
        gbc.gridx = 0;
        gbc.gridy = 2;
        infoGrid.add(createInfoLabel("📍 ALAMAT"), gbc);

        gbc.gridx = 1;
        infoGrid.add(createInfoValue("", "alamat"), gbc);

        // Row 4: Shift Kerja
        gbc.gridx = 0;
        gbc.gridy = 3;
        infoGrid.add(createInfoLabel("🏪 SHIFT KERJA"), gbc);

        gbc.gridx = 1;
        infoGrid.add(createInfoValue("", "shift"), gbc);

        // Row 5: Bergabung Sejak
        gbc.gridx = 0;
        gbc.gridy = 4;
        infoGrid.add(createInfoLabel("📅 BERGABUNG SEJAK"), gbc);

        gbc.gridx = 1;
        infoGrid.add(createInfoValue("", "join_date"), gbc);

        // Row 6: Role
        gbc.gridx = 0;
        gbc.gridy = 5;
        infoGrid.add(createInfoLabel("👤 ROLE"), gbc);

        gbc.gridx = 1;
        infoGrid.add(createInfoValue("Kasir", "role"), gbc);

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(infoGrid, BorderLayout.CENTER);

        return panel;
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(TEXT_DARK);
        label.setPreferredSize(new Dimension(180, 30));
        return label;
    }

    private JLabel createInfoValue(String text, String fieldName) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(new Color(60, 60, 60));
        label.setName(fieldName);
        label.setPreferredSize(new Dimension(250, 30));
        return label;
    }

    private JButton createIconButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
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

    private ImageIcon loadImageIcon(String filename, int size) {
        try {
            // Option 1: Coba dari resources
            java.net.URL imageUrl = getClass().getResource("/img/" + filename);
            if (imageUrl != null) {
                return new ImageIcon(
                        new ImageIcon(imageUrl).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
            }

            // Option 2: Coba load dari file system
            String[] probablePaths = {
                    "src/img/" + filename,
                    "img/" + filename
            };

            for (String path : probablePaths) {
                java.io.File file = new java.io.File(path);
                if (file.exists()) {
                    return new ImageIcon(
                            new ImageIcon(path).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ================= HELPER IMAGES (Ported from KasirDashboard)
    // =================
    private static final String WEB_BASE_PATH = "C:/xampp/htdocs/distrozoneweb/";

    private String resolveImagePath(String path) {
        if (path == null || path.isEmpty())
            return null;
        File f = new File(path);
        if (f.isAbsolute() && f.exists())
            return path;

        // Cek path lokal project
        String[] localPaths = {
                "src/img/" + path, "img/" + path, "build/classes/img/" + path,
                System.getProperty("user.dir") + File.separator + "img" + File.separator + path
        };

        for (String localPath : localPaths) {
            File localFile = new File(localPath);
            if (localFile.exists())
                return localFile.getAbsolutePath();
        }

        // Cek XAMPP / Web Path (Fallback)
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        if (!cleanPath.contains(":")) {
            String fullPath = WEB_BASE_PATH + cleanPath;
            fullPath = fullPath.replace("/", File.separator).replace("\\", File.separator);
            File webFile = new File(fullPath);
            if (webFile.exists())
                return fullPath;
        }

        return path;
    }

    private ImageIcon getUserPhoto(int userId, int size) {
        ImageIcon defaultIcon = loadImageIcon("user.png", size);
        if (conn == null)
            connectDatabase();

        try {
            String sql = "SELECT foto FROM users WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String foto = rs.getString("foto");
                if (foto != null && !foto.isEmpty()) {
                    String resolved = resolveImagePath(foto);
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
        g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, size, size, 20, 20)); // Rounded Square
        g2.drawImage(image.getScaledInstance(size, size, Image.SCALE_SMOOTH), 0, 0, null);
        g2.dispose();
        return roundedImage;
    }

    private void loadKasirData() {
        try {
            if (conn == null || conn.isClosed()) {
                connectDatabase();
            }

            String sql = "SELECT user_code, nama, email, no_telp, shift, status, " +
                    "alamat, DATE(created_at) as join_date, role " +
                    "FROM users WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Update semua label value
                updateLabelValue("nama_lengkap", rs.getString("nama"));
                updateLabelValue("kode_user", "Kode: " + rs.getString("user_code"));
                updateLabelValue("email", rs.getString("email") != null ? rs.getString("email") : "-");
                updateLabelValue("no_telp", rs.getString("no_telp") != null ? rs.getString("no_telp") : "-");
                updateLabelValue("alamat", rs.getString("alamat") != null ? rs.getString("alamat") : "-");
                updateLabelValue("shift", rs.getString("shift") != null ? rs.getString("shift") : "Belum diatur");
                updateLabelValue("role", rs.getString("role") != null ? rs.getString("role") : "Kasir");

                // Status dengan warna
                String status = rs.getString("status");
                String statusText = status.equals("active") ? "AKTIF" : "NON-AKTIF";
                updateLabelValue("status", "Status: " + statusText);

                // Format tanggal
                java.sql.Date joinDate = rs.getDate("join_date");
                String joinDateStr = "-";
                if (joinDate != null) {
                    joinDateStr = new SimpleDateFormat("dd MMMM yyyy").format(joinDate);
                }
                updateLabelValue("join_date", joinDateStr);

                // Update window title
                setTitle("👤 Info Kasir - " + rs.getString("nama"));

                // Load stats data
                loadStatsData(rs.getString("user_code"));

            } else {
                JOptionPane.showMessageDialog(this,
                        "Data kasir tidak ditemukan!",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Gagal memuat data kasir:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadStatsData(String userCode) {
        try {
            // Hitung total transaksi
            String transaksiSql = "SELECT COUNT(*) as total FROM transaksi WHERE kasir_id = ?";
            PreparedStatement ps1 = conn.prepareStatement(transaksiSql);
            ps1.setInt(1, userId);
            ResultSet rs1 = ps1.executeQuery();

            int totalTransaksi = 0;
            double totalPendapatan = 0;
            int totalKaosTerjual = 0;

            if (rs1.next()) {
                totalTransaksi = rs1.getInt("total");
            }
            rs1.close();
            ps1.close();

            // Hitung total pendapatan
            String pendapatanSql = "SELECT COALESCE(SUM(total), 0) as total FROM transaksi WHERE kasir_id = ?";
            PreparedStatement ps2 = conn.prepareStatement(pendapatanSql);
            ps2.setInt(1, userId);
            ResultSet rs2 = ps2.executeQuery();

            if (rs2.next()) {
                totalPendapatan = rs2.getDouble("total");
            }
            rs2.close();
            ps2.close();

            // Hitung total kaos terjual
            String kaosSql = "SELECT COALESCE(SUM(dt.qty), 0) as total " +
                    "FROM detail_transaksi dt " +
                    "JOIN transaksi t ON dt.transaksi_id = t.id " +
                    "WHERE t.kasir_id = ?";
            PreparedStatement ps3 = conn.prepareStatement(kaosSql);
            ps3.setInt(1, userId);
            ResultSet rs3 = ps3.executeQuery();

            if (rs3.next()) {
                totalKaosTerjual = rs3.getInt("total");
            }
            rs3.close();
            ps3.close();

            // Update stats (ini akan mengupdate secara manual karena tidak menggunakan
            // label)
            // Untuk implementasi lengkap, Anda perlu menyimpan referensi ke stat labels

        } catch (SQLException e) {
            System.out.println("Gagal load stats: " + e.getMessage());
        }
    }

    private void updateLabelValue(String fieldName, String value) {
        // Cari label berdasarkan name di seluruh komponen frame
        updateLabelInContainer(getContentPane(), fieldName, value);
    }

    private void updateLabelInContainer(Container container, String fieldName, String value) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel && fieldName.equals(((JLabel) comp).getName())) {
                ((JLabel) comp).setText(value);

                // Set warna khusus untuk status
                if (fieldName.equals("status")) {
                    if (value.contains("AKTIF")) {
                        comp.setForeground(GREEN_DARK);
                    } else if (value.contains("NON-AKTIF")) {
                        comp.setForeground(new Color(231, 76, 60));
                    }
                    ((JLabel) comp).setFont(comp.getFont().deriveFont(Font.BOLD));
                }
                return;
            } else if (comp instanceof Container) {
                updateLabelInContainer((Container) comp, fieldName, value);
            }
        }
    }

    @Override
    public void dispose() {
        closeDatabase();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            KasirInfo frame = new KasirInfo(2, "Kasir Utama", "KSR001");
            frame.setVisible(true);
        });
    }
}