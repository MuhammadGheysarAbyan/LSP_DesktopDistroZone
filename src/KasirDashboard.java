import java.io.File;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO; // Import ImageIO

public class KasirDashboard extends JFrame {
    private int userId;
    private String namaUser;
    private String userCode;
    private JLabel lblTime;
    private Timer clockTimer;
    private Connection conn;

    // Colors - SAMA DENGAN WEB (Emerald Green Theme)
    private static final Color GREEN_PRIMARY = new Color(16, 185, 129); // #10B981
    private static final Color GREEN_DARK = new Color(15, 118, 110); // #0F766E
    private static final Color GREEN_LIGHT = new Color(52, 211, 153); // #34D399
    private static final Color BG_LIGHT = new Color(236, 253, 245); // #ECFDF5
    private static final Color TEXT_DARK = new Color(31, 41, 55); // #1F2937
    private static final Color TEXT_GRAY = new Color(100, 116, 139); // #64748B

    // Colors untuk menu cards - Sesuai tema web
    private static final Color COLOR_TRANSAKSI = new Color(14, 165, 233); // #0EA5E9 Sky
    private static final Color COLOR_LAPORAN = new Color(16, 185, 129); // #10B981 Emerald
    private static final Color COLOR_KAOS = new Color(139, 92, 246); // #8B5CF6 Violet
    private static final Color COLOR_INFO = new Color(245, 158, 11); // #F59E0B Amber
    private static final Color RED_LOGOUT = new Color(239, 68, 68); // #EF4444
    private static final Color RED_LOGOUT_HOVER = new Color(220, 38, 38); // #DC2626

    private static final DecimalFormat RUPIAH = new DecimalFormat("#,###");

    public KasirDashboard(int userId, String namaUser, String userCode) {
        this.userId = userId;
        this.namaUser = namaUser;
        this.userCode = userCode;

        setTitle("🏪 Dashboard Kasir - Distro Zone");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());

        connectDatabase();

        // Background Panel dengan gradient hijau seperti LoginForm
        JPanel background = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, GREEN_PRIMARY, 0, getHeight(), Color.WHITE);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        background.setLayout(new BorderLayout());
        add(background, BorderLayout.CENTER);

        background.add(createTopPanel(), BorderLayout.NORTH);
        background.add(createCenterPanel(), BorderLayout.CENTER);

        startClock();
    }

    private void connectDatabase() {
        try {
            String url = "jdbc:mysql://localhost:3306/distrozone_db";
            String user = "root";
            String password = "";
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Database Kasir connected ✅");
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
                System.out.println("Database connection closed ✅");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= HELPER IMAGES =================
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
        // Default fallback: user.png (Silhouette) instead of Logo
        ImageIcon defaultIcon = loadImageIcon("user.png", size);
        if (defaultIcon == null)
            defaultIcon = loadLogoIcon(size); // Ultimate fallback

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
                    System.out.println("DEBUG: User Photo Path: " + foto + " -> Resolved: " + resolved);

                    if (resolved != null) {
                        ImageIcon icon = new ImageIcon(resolved);
                        if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                            Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                            return new ImageIcon(createRoundedImage(img, size));
                        } else {
                            System.out.println("DEBUG: Failed to load user image (Status not COMPLETE): " + resolved);
                        }
                    }
                }
            }
            rs.close();
            pst.close();
        } catch (Exception e) {
            System.out.println("Gagal load foto user: " + e.getMessage());
            e.printStackTrace();
        }
        return defaultIcon;
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 40, 15, 40));

        // Panel kiri (Logo + Welcome)
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setOpaque(false);

        // Load logo dengan ukuran lebih besar (Brand Logo) - sama dengan Admin
        // Dashboard
        ImageIcon logoIcon = loadLogoIcon(80); // 80x80 pixels
        JLabel lblLogo = new JLabel(logoIcon);
        lblLogo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
        leftPanel.add(lblLogo);

        JPanel welcomePanel = new JPanel();
        welcomePanel.setOpaque(false);
        welcomePanel.setLayout(new BoxLayout(welcomePanel, BoxLayout.Y_AXIS));

        JLabel lblWelcome = new JLabel("Selamat datang, " + namaUser);
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblWelcome.setForeground(Color.WHITE);

        JLabel lblRole = new JLabel("Kasir • ID: " + userCode);
        lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblRole.setForeground(new Color(255, 255, 255, 180));

        welcomePanel.add(lblWelcome);
        welcomePanel.add(lblRole);

        leftPanel.add(welcomePanel);

        // Panel kanan (Jam)
        lblTime = new JLabel();
        lblTime.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTime.setForeground(Color.WHITE);
        lblTime.setHorizontalAlignment(SwingConstants.RIGHT);

        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(lblTime, BorderLayout.EAST);

        return topPanel;
    }

    // ================= METHOD UNTUK LOAD LOGO =================

    private ImageIcon loadLogoIcon(int size) {
        try {
            // Priority 1: Check known absolute path
            File absFile = new File(
                    "c:/Users/antartika2/OneDrive/Dokumen/NetBeansProjects/DistroZone/src/img/distrozonelogoBG.jpeg");
            if (absFile.exists()) {
                Image img = ImageIO.read(absFile);
                return new ImageIcon(createRoundedImage(img, size));
            }

            // Priority 2: Check standard relative paths
            String[] probablePaths = {
                    "src/img/distrozonelogoBG.jpeg",
                    "img/distrozonelogoBG.jpeg",
                    "build/classes/img/distrozonelogoBG.jpeg"
            };

            for (String path : probablePaths) {
                File file = new File(path);
                if (file.exists()) {
                    Image img = ImageIO.read(file);
                    return new ImageIcon(createRoundedImage(img, size));
                }
            }

            // Priority 3: Check resources
            java.net.URL logoUrl = getClass().getResource("/img/distrozonelogoBG.jpeg");
            if (logoUrl != null) {
                Image img = ImageIO.read(logoUrl);
                return new ImageIcon(createRoundedImage(img, size));
            }

            // Fallback default
            return createDefaultLogo(size);

        } catch (Exception e) {
            e.printStackTrace();
            return createDefaultLogo(size);
        }
    }

    // ================= METHOD UNTUK LOAD IMAGE ICON =================
    private ImageIcon loadImageIcon(String filename, int size) {
        try {
            System.out.println("=== DEBUG LOAD IMAGE: " + filename + " ===");

            // Option 1: Coba load dari folder img di luar src
            String imagePath = System.getProperty("user.dir") + "/img/" + filename;
            System.out.println("Mencari gambar di: " + imagePath);
            ImageIcon originalIcon = new ImageIcon(imagePath);

            // Check jika gambar berhasil di-load
            if (originalIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                System.out.println("Gagal load dari " + imagePath + ", mencoba alternatif...");

                // Option 2: Coba dari resources
                java.net.URL imageUrl = getClass().getResource("/img/" + filename);
                if (imageUrl != null) {
                    originalIcon = new ImageIcon(imageUrl);
                    System.out.println("Mencoba dari resources: " + imageUrl);
                }
            }

            // Jika masih tidak ditemukan, coba direktori parent
            if (originalIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                System.out.println("Mencoba dari parent directory...");
                originalIcon = new ImageIcon(filename);
            }

            // Jika berhasil di-load
            if (originalIcon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                System.out.println(filename + " berhasil di-load!");

                // Scale gambar ke ukuran yang diinginkan
                Image img = originalIcon.getImage().getScaledInstance(
                        size, size, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            } else {
                // Jika semua gagal, return null
                System.out.println(filename + " tidak ditemukan, menggunakan fallback...");
                return null;
            }

        } catch (Exception e) {
            System.out.println("Error load " + filename + ": " + e.getMessage());
            return null;
        }
    }

    // ================= METHOD UNTUK BUAT SQUARE IMAGE (KOTAK) =================
    // ================= METHOD UNTUK BUAT ROUNDED IMAGE (LENGKUNG)
    // =================
    private BufferedImage createRoundedImage(Image image, int size) {
        BufferedImage roundedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = roundedImage.createGraphics();

        // Enable anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Buat bentuk rounded
        g2.setClip(new RoundRectangle2D.Float(0, 0, size, size, 20, 20)); // 20px radius
        g2.drawImage(image.getScaledInstance(size, size, Image.SCALE_SMOOTH), 0, 0, null);

        g2.dispose();
        return roundedImage;
    }

    // ================= METHOD UNTUK BUAT LOGO DEFAULT =================
    private ImageIcon createDefaultLogo(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background dengan rounded corners
        int cornerRadius = 20;
        g2d.setColor(GREEN_PRIMARY);
        g2d.fillRoundRect(0, 0, size, size, cornerRadius, cornerRadius);

        // Draw border
        g2d.setColor(GREEN_DARK);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(2, 2, size - 4, size - 4, cornerRadius, cornerRadius);

        // Draw text
        g2d.setColor(Color.WHITE);
        int fontSize = size / 3;
        g2d.setFont(new Font("Arial", Font.BOLD, fontSize));

        // Center text
        FontMetrics fm = g2d.getFontMetrics();
        String text = "DZ";
        int x = (size - fm.stringWidth(text)) / 2;
        int y = ((size - fm.getHeight()) / 2) + fm.getAscent();

        g2d.drawString(text, x, y);

        g2d.dispose();

        return new ImageIcon(img);
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 20));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 40, 40, 40));

        centerPanel.add(createMenuCard(), BorderLayout.CENTER);
        centerPanel.add(createBottomPanel(), BorderLayout.SOUTH);
        return centerPanel;
    }

    private JPanel createMenuCard() {
        JPanel card = new JPanel(new GridLayout(2, 2, 25, 25)); // Ubah ke 2x2 grid
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        // Menu untuk Kasir - SEKARANG 4
        JButton btnTransaksi = createMenuButton(
                "Transaksi Baru",
                "BUAT TRANSAKSI",
                COLOR_TRANSAKSI,
                loadImageIcon("chart.png", 70));
        JButton btnLaporan = createMenuButton(
                "Laporan Kasir",
                "LIHAT LAPORAN",
                COLOR_LAPORAN,
                loadImageIcon("report.png", 70));
        JButton btnKaos = createMenuButton(
                "Daftar Kaos", // Reverted to Read Only
                "LIHAT PRODUK",
                COLOR_KAOS,
                loadImageIcon("tshirt.png", 70));
        JButton btnInfo = createMenuButton(
                "Info Kasir",
                "PROFIL",
                COLOR_INFO,
                loadImageIcon("user.png", 70)); // Static Icon

        btnTransaksi.addActionListener(e -> openTransaksiForm());
        btnLaporan.addActionListener(e -> openLaporanKasir());
        btnKaos.addActionListener(e -> openDaftarKaos());
        btnInfo.addActionListener(e -> showKasirInfo());

        card.add(btnTransaksi);
        card.add(btnLaporan);
        card.add(btnKaos);
        card.add(btnInfo);

        return card;
    }

    private JButton createMenuButton(String text, String subtitle, Color bgColor, ImageIcon icon) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Flat background - no shadow, no gradient
                Color backgroundColor = getModel().isRollover() ? bgColor.brighter() : bgColor;
                g2.setColor(backgroundColor);
                g2.fillRoundRect(0, 0, w, h, 24, 24);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setLayout(new BorderLayout());
        btn.setPreferredSize(new Dimension(180, 140));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel untuk icon
        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.setOpaque(false);
        if (icon != null) {
            JLabel iconLabel = new JLabel(icon, JLabel.CENTER);
            iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            iconPanel.add(iconLabel, BorderLayout.CENTER);
        } else {
            // Fallback jika icon tidak ditemukan
            JLabel fallbackLabel = new JLabel("💰", JLabel.CENTER);
            fallbackLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
            fallbackLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            iconPanel.add(fallbackLabel, BorderLayout.CENTER);
        }

        // Panel untuk text
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);
        textPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JLabel lblText = new JLabel(text, JLabel.CENTER);
        lblText.setForeground(Color.WHITE);
        lblText.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JLabel lblSubtitle = new JLabel(subtitle, JLabel.CENTER);
        lblSubtitle.setForeground(new Color(255, 255, 255, 200));
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        textPanel.add(lblText);
        textPanel.add(lblSubtitle);

        // Main content panel
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        content.add(iconPanel, BorderLayout.CENTER);
        content.add(textPanel, BorderLayout.SOUTH);

        btn.add(content, BorderLayout.CENTER);

        // Efek hover sederhana - hanya ubah warna background
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.repaint();
            }
        });

        return btn;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        // Panel tengah untuk statistik
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 25, 0)); // Hanya 3 statistik
        statsPanel.setOpaque(false);
        statsPanel.setPreferredSize(new Dimension(800, 80));

        // Statistik untuk Kasir - HANYA 3
        statsPanel.add(createInfoCard("Transaksi Hari Ini", getKasirTransactionToday(), COLOR_TRANSAKSI));
        statsPanel.add(createInfoCard("Pendapatan Hari Ini", getKasirRevenueToday(), COLOR_LAPORAN));
        statsPanel.add(createInfoCard("Shift", getKasirShift(), COLOR_INFO));

        // Panel kanan untuk tombol logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(200, 80));

        // Tombol logout warna merah
        JButton btnLogout = createLogoutButton();
        rightPanel.add(btnLogout);

        bottomPanel.add(statsPanel, BorderLayout.CENTER);
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        return bottomPanel;
    }

    private JButton createLogoutButton() {
        JButton btnLogout = new JButton("Logout") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color backgroundColor;
                if (getModel().isPressed()) {
                    backgroundColor = RED_LOGOUT_HOVER.darker();
                } else if (getModel().isRollover()) {
                    backgroundColor = RED_LOGOUT_HOVER;
                } else {
                    backgroundColor = RED_LOGOUT;
                }

                // Gambar background kotak (tanpa rounded corners)
                g2.setColor(backgroundColor);
                g2.fillRect(0, 0, getWidth(), getHeight());

                super.paintComponent(g2);
                g2.dispose();
            }
        };

        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setContentAreaFilled(false);
        btnLogout.setPreferredSize(new Dimension(120, 40));
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        btnLogout.addActionListener(e -> logout());
        return btnLogout;
    }

    private JPanel createInfoCard(String title, String value, Color accentColor) {
        // Simple card tanpa background putih - sama dengan Admin Dashboard
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblValue.setForeground(TEXT_DARK);
        lblValue.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTitle.setForeground(new Color(120, 120, 120));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(lblValue);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(lblTitle);

        card.add(textPanel, BorderLayout.CENTER);

        JPanel accentWrapper = new JPanel(new BorderLayout());
        accentWrapper.setOpaque(false);
        accentWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        JPanel accent = new JPanel();
        accent.setBackground(accentColor);
        accent.setPreferredSize(new Dimension(4, 0));
        accentWrapper.add(accent, BorderLayout.CENTER);

        card.add(accentWrapper, BorderLayout.WEST);

        return card;
    }

    // Method untuk statistik Kasir
    private String getKasirTransactionToday() {
        String result = "0";
        try {
            if (conn != null && !conn.isClosed()) {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM TRANSAKSI WHERE DATE(tanggal) = CURDATE() AND kasir_id = ? AND status = 'completed'");
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                    result = rs.getString(1);
                rs.close();
                ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            result = "Error";
        }
        return result;
    }

    private String getKasirRevenueToday() {
        String result = "Rp 0";
        try {
            if (conn != null && !conn.isClosed()) {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT SUM(total) FROM TRANSAKSI WHERE DATE(tanggal) = CURDATE() AND kasir_id = ? AND status = 'completed'");
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    double total = rs.getDouble(1);
                    if (!rs.wasNull()) {
                        result = "Rp " + RUPIAH.format(total);
                    }
                }
                rs.close();
                ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            result = "Error";
        }
        return result;
    }

    private String getKasirShift() {
        String result = "Aktif";
        try {
            if (conn != null && !conn.isClosed()) {
                PreparedStatement ps = conn.prepareStatement("SELECT shift FROM users WHERE id = ?");
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String shift = rs.getString("shift");
                    if (shift != null && !shift.isEmpty()) {
                        result = shift;
                    }
                }
                rs.close();
                ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            result = "Error";
        }
        return result;
    }

    private void startClock() {
        clockTimer = new Timer(1000, e -> updateClock());
        clockTimer.start();
        updateClock();
    }

    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        String timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String dateStr = now.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        lblTime.setText("<html><div style='text-align: right'>" + timeStr + "<br>" + dateStr + "</div></html>");
    }

    private void openTransaksiForm() {
        closeDatabase();
        SwingUtilities.invokeLater(() -> {
            try {
                TransaksiForm transaksiForm = new TransaksiForm(userId, namaUser, userCode);
                transaksiForm.setVisible(true);
                dispose();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error membuka Transaksi Form: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void openDaftarKaos() {
        closeDatabase();
        SwingUtilities.invokeLater(() -> {
            try {
                // Revert ke DaftarKaos (Read Only)
                DaftarKaos daftarKaos = new DaftarKaos(userId, namaUser, userCode);
                daftarKaos.setVisible(true);
                dispose();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error membuka Daftar Kaos: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void openLaporanKasir() {
        closeDatabase();
        SwingUtilities.invokeLater(() -> {
            try {
                LaporanKasir laporanKasir = new LaporanKasir(userId, namaUser, userCode);
                laporanKasir.setVisible(true);
                dispose();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error membuka Laporan Kasir: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void showKasirInfo() {
        closeDatabase();
        SwingUtilities.invokeLater(() -> {
            try {
                KasirInfo kasirInfo = new KasirInfo(userId, namaUser, userCode);
                kasirInfo.setVisible(true);
                dispose();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error membuka Info Kasir: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void logout() {
        // Stop timer
        if (clockTimer != null) {
            clockTimer.stop();
        }

        // Tutup koneksi database
        closeDatabase();

        // Tutup dashboard
        dispose();

        // Kembali ke LoginForm
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }

    // ==================== TAMBAHKAN METHOD MAIN() DI SINI ====================
    /**
     * Method main untuk menjalankan aplikasi langsung dari KasirDashboard
     * Digunakan untuk testing dan development
     */
    public static void main(String[] args) {
        // Menggunakan SwingUtilities.invokeLater untuk thread safety
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    // Set look and feel agar sesuai dengan OS
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                    // Data dummy untuk testing (sesuaikan dengan database Anda)
                    int userId = 2; // ID kasir
                    String namaUser = "Kasir Utama"; // Nama kasir
                    String userCode = "KSR001"; // Kode user kasir

                    System.out.println("=== MEMULAI KASIR DASHBOARD ===");
                    System.out.println("User ID: " + userId);
                    System.out.println("Nama: " + namaUser);
                    System.out.println("Kode: " + userCode);

                    // Buat dan tampilkan dashboard
                    KasirDashboard dashboard = new KasirDashboard(userId, namaUser, userCode);
                    dashboard.setVisible(true);

                    System.out.println("Dashboard berhasil ditampilkan!");

                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            "Error saat menjalankan aplikasi:\n" + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
    // ==================== AKHIR METHOD MAIN() ====================
}