import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.*;

public class LoginForm extends JFrame {

    private JTextField txtUser;
    private JPasswordField txtPass;
    private JCheckBox showPass;
    private JComboBox<String> cmbRole;

    // ================= THEME (Sesuai Web - Emerald Green) =================
    private static final Color GREEN_PRIMARY = new Color(16, 185, 129); // #10B981
    private static final Color GREEN_DARK = new Color(15, 118, 110); // #0F766E
    private static final Color GREEN_LIGHT = new Color(52, 211, 153); // #34D399
    private static final Color BG_LIGHT = new Color(236, 253, 245); // #ECFDF5
    private static final Color TEXT_DARK = new Color(31, 41, 55); // #1F2937
    private static final Color TEXT_GRAY = new Color(100, 116, 139); // #64748B

    public LoginForm() {
        setTitle("🔑 DistroZone - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());

        // ================= BACKGROUND =================
        JPanel background = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(
                        0, 0, GREEN_PRIMARY,
                        0, getHeight(), Color.WHITE);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        background.setLayout(new GridBagLayout());
        add(background, BorderLayout.CENTER);

        // ================= CARD =================
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 235));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(420, 500));
        card.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // ================= LOGO =================
        gbc.gridy++;
        gbc.insets = new Insets(20, 0, 10, 0);

        ImageIcon logoIcon = loadLogoIcon();
        JLabel lblLogo = new JLabel(logoIcon, JLabel.CENTER);
        lblLogo.setPreferredSize(new Dimension(logoIcon.getIconWidth(), logoIcon.getIconHeight()));
        card.add(lblLogo, gbc);

        // ================= TITLE =================
        gbc.gridy++;
        gbc.insets = new Insets(10, 0, 5, 0);
        JLabel lblTitle = new JLabel("DISTROZONE", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(TEXT_DARK);
        card.add(lblTitle, gbc);

        gbc.insets = new Insets(5, 0, 5, 0);

        // ================= USERNAME =================
        gbc.gridy++;
        card.add(label("Username"), gbc);

        gbc.gridy++;
        txtUser = field();
        card.add(txtUser, gbc);

        // ================= PASSWORD =================
        gbc.gridy++;
        card.add(label("Password"), gbc);

        gbc.gridy++;
        txtPass = new JPasswordField();
        txtPass.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtPass.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        card.add(txtPass, gbc);

        // ================= ROLE =================
        gbc.gridy++;
        card.add(label("Login Sebagai"), gbc);

        gbc.gridy++;
        cmbRole = new JComboBox<>(new String[] { "admin", "kasir" });
        cmbRole.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        card.add(cmbRole, gbc);

        // ================= SHOW PASSWORD =================
        gbc.gridy++;
        showPass = new JCheckBox("Show Password");
        showPass.setOpaque(false);
        showPass.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        showPass.setForeground(TEXT_GRAY);
        showPass.addActionListener(e -> txtPass.setEchoChar(showPass.isSelected() ? (char) 0 : '•'));
        card.add(showPass, gbc);

        // ================= BUTTON LOGIN =================
        gbc.gridy++;
        gbc.insets = new Insets(20, 0, 0, 0);
        JButton btnLogin = loginButton();
        card.add(btnLogin, gbc);

        background.add(card);

        txtUser.addActionListener(e -> login());
        txtPass.addActionListener(e -> login());
    }

    private ImageIcon loadLogoIcon() {
        try {
            ImageIcon originalIcon = null;
            String[] possiblePaths = {
                    System.getProperty("user.dir") + "\\img\\logodistrozone.png",
                    System.getProperty("user.dir") + "/img/logodistrozone.png",
                    "logodistrozone.png",
                    "img/logodistrozone.png"
            };

            for (String path : possiblePaths) {
                originalIcon = new ImageIcon(path);
                if (originalIcon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                    break;
                }
            }

            if (originalIcon == null || originalIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                java.net.URL logoUrl = getClass().getResource("/img/logodistrozone.png");
                if (logoUrl != null) {
                    originalIcon = new ImageIcon(logoUrl);
                }
            }

            if (originalIcon != null && originalIcon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                int logoSize = 100;
                Image img = originalIcon.getImage().getScaledInstance(
                        logoSize, logoSize, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            } else {
                return createDefaultLogo();
            }

        } catch (Exception e) {
            return createDefaultLogo();
        }
    }

    private ImageIcon createDefaultLogo() {
        int size = 100;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(GREEN_PRIMARY);
        g2d.fillOval(5, 5, size - 10, size - 10);

        g2d.setColor(GREEN_DARK);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(5, 5, size - 10, size - 10);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 32));

        FontMetrics fm = g2d.getFontMetrics();
        String text = "DZ";
        int x = (size - fm.stringWidth(text)) / 2;
        int y = ((size - fm.getHeight()) / 2) + fm.getAscent();

        g2d.drawString(text, x, y);
        g2d.dispose();

        return new ImageIcon(img);
    }

    // ================= LOGIN LOGIC =================
    private void login() {
        String username = txtUser.getText().trim();
        String password = String.valueOf(txtPass.getPassword());
        String role = cmbRole.getSelectedItem().toString();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Username dan Password wajib diisi!",
                    "Validasi",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = """
                    SELECT id, user_code, username, nama, role, password, platform
                    FROM users
                    WHERE username = ?
                    AND LOWER(role) = LOWER(?)
                    AND status = 'active'
                    AND (platform = 'desktop' OR platform = 'all' OR platform IS NULL)
                    LIMIT 1
                """;

        System.out.println("=== DEBUG LOGIN INFO ===");
        System.out.println("Username: " + username);
        System.out.println("Password (plain): " + password.replaceAll(".", "*"));
        System.out.println("Role: " + role);
        System.out.println("SQL Query: " + sql);

        try (Connection conn = getConnection()) {
            if (conn == null) {
                JOptionPane.showMessageDialog(this,
                        "Koneksi database gagal!",
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            System.out.println("Database connected: " + conn.getCatalog());

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, role);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int userId = rs.getInt("id");
                        String nama = rs.getString("nama");
                        String userCode = rs.getString("user_code");
                        String dbUsername = rs.getString("username");
                        String dbRole = rs.getString("role");
                        String storedPassword = rs.getString("password");

                        System.out.println("=== DEBUG USER DATA ===");
                        System.out.println("User ID: " + userId);
                        System.out.println("Nama: " + nama);
                        System.out.println("User Code: " + userCode);
                        System.out.println("DB Username: " + dbUsername);
                        System.out.println("DB Role: " + dbRole);
                        System.out.println("Stored Password: " + storedPassword);
                        System.out.println("Stored Password Length: "
                                + (storedPassword != null ? storedPassword.length() : "null"));

                        // PASTIKAN NAMA METHOD INI BENAR!
                        boolean valid = verifyPassword(password, storedPassword); // Jika menggunakan method umum

                        if (valid) {
                            System.out.println("=== LOGIN BERHASIL ===");
                            System.out.println("User: " + nama + " (" + dbRole + ")");

                            SwingUtilities.invokeLater(() -> {
                                dispose();

                                if ("admin".equalsIgnoreCase(dbRole)) {
                                    new AdminDashboard(userId, nama, userCode).setVisible(true);
                                } else {
                                    new KasirDashboard(userId, nama, userCode).setVisible(true);
                                }
                            });

                        } else {
                            System.out.println("=== LOGIN GAGAL - Password tidak cocok ===");
                            JOptionPane.showMessageDialog(this,
                                    "Username atau password salah!",
                                    "Login Gagal",
                                    JOptionPane.ERROR_MESSAGE);
                        }

                    } else {
                        System.out.println("=== LOGIN GAGAL - User tidak ditemukan ===");
                        JOptionPane.showMessageDialog(this,
                                "Username, password, atau role tidak valid!",
                                "Login Gagal",
                                JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Database Error: " + e.getMessage(),
                    "System Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error: " + e.getMessage(),
                    "System Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================= METHOD VERIFIKASI PASSWORD UMUM =================
    private boolean verifyPassword(String password, String storedPassword) {
        try {
            if (storedPassword == null || storedPassword.isEmpty()) {
                System.out.println("Stored password kosong!");
                return false;
            }

            System.out.println("=== DEBUG PASSWORD VERIFICATION ===");
            // System.out.println("Stored: " + storedPassword); // Caution: logging plain
            // password

            // Compare plain text
            if (password.equals(storedPassword)) {
                System.out.println("Password cocok (Plain Text)");
                return true;
            }

            System.out.println("Password tidak cocok");
            return false;

        } catch (Exception e) {
            System.out.println("Password verification error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ================= METHOD KONEKSI DATABASE =================
    private Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL Driver loaded");

            String url = "jdbc:mysql://localhost:3306/distrozone_db";
            String user = "root";
            String password = "";

            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("Database connection established");
            return conn;

        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "MySQL Driver tidak ditemukan!",
                    "Driver Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        } catch (SQLException e) {
            System.err.println("Database connection failed!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Koneksi database gagal!\n" + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    // ================= COMPONENT HELPERS =================
    private JLabel label(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(TEXT_GRAY);
        return lbl;
    }

    private JTextField field() {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        return tf;
    }

    private JButton loginButton() {
        JButton btn = new JButton("LOGIN") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color backgroundColor = GREEN_PRIMARY;

                if (getModel().isPressed()) {
                    backgroundColor = GREEN_DARK;
                } else if (getModel().isRollover()) {
                    backgroundColor = GREEN_LIGHT;
                }

                g2.setColor(backgroundColor);
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(new Color(180, 180, 180));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                // Nonaktifkan border default
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setPreferredSize(new Dimension(0, 40));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(10, 30, 10, 30));

        btn.addActionListener(e -> login());

        return btn;
    }

    // ================= MAIN =================
    public static void main(String[] args) {
        System.out.println("=== APP STARTING ===");
        System.out.println("Working Directory: " + System.getProperty("user.dir"));
        System.out.println("Java Version: " + System.getProperty("java.version"));

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                LoginForm loginForm = new LoginForm();
                loginForm.setVisible(true);
                System.out.println("Login form created successfully");

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Error starting application: " + e.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}