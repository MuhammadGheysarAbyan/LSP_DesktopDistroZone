import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.awt.image.BufferedImage;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

public class PengaturanOperasional extends JFrame {
    // Checkboxes for days (Minggu - Sabtu)
    private JCheckBox[] chkOfflineDays;
    private JCheckBox[] chkOnlineDays;
    private JTextField txtOfflineOpen, txtOfflineClose;
    private JTextField txtOnlineOpen, txtOnlineClose;
    private JTextArea txtHolidays; // Field baru untuk hari libur - Changed to JTextArea
    private JButton btnSave;
    private Connection conn;

    // Tambah field untuk identitas admin
    private int userId;
    private String adminName;
    private String userCode;

    // Warna tema hijau Distro Zone (Emerald Green - Sesuai Web)
    private static final Color GREEN_PRIMARY = new Color(16, 185, 129); // #10B981
    private static final Color GREEN_DARK = new Color(15, 118, 110); // #0F766E
    private static final Color GREEN_LIGHT = new Color(52, 211, 153); // #34D399
    private static final Color TEXT_DARK = new Color(31, 41, 55); // #1F2937
    private static final Color BACKGROUND = new Color(236, 253, 245); // #ECFDF5

    // ================= KONSTRUKTOR BARU =================
    public PengaturanOperasional(int userId, String adminName, String userCode) {
        this.userId = userId;
        this.adminName = adminName;
        this.userCode = userCode;

        setTitle("🕐 Pengaturan Jam Operasional - ");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // FULL SCREEN

        initComponents();
        loadData();
        setVisible(true);
    }

    // ================= KONSTRUKTOR LAMA (untuk kompatibilitas) =================
    public PengaturanOperasional(String usernameAdmin) {
        this(0, usernameAdmin, "ADM001"); // Default values untuk kompatibilitas
    }

    private void initComponents() {
        setLocationRelativeTo(null);

        // Koneksi database
        connectDatabase();

        // Main Panel dengan background gradient
        JPanel mainPanel = new JPanel(new BorderLayout()) {
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
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        // ================= HEADER PANEL DENGAN ICON =================
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        // Title Panel dengan icon setting.png
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        titlePanel.setOpaque(false);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        // Load icon setting.png
        ImageIcon settingsIcon = loadSettingsIcon();
        JLabel lblIcon = new JLabel(settingsIcon);
        lblIcon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        JLabel lblTitle = new JLabel("Pengaturan Jam Operasional");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblTitle.setForeground(Color.WHITE);

        titlePanel.add(lblIcon);
        titlePanel.add(lblTitle);

        headerPanel.add(titlePanel, BorderLayout.CENTER);

        // ================= CONTENT PANEL =================
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 30, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        // Panel container untuk form (3 kolom sekarang)
        JPanel formContainer = new JPanel(new GridLayout(1, 3, 20, 0));
        formContainer.setOpaque(false);
        formContainer.setPreferredSize(new Dimension(1100, 400));

        // Panel Offline
        JPanel offlinePanel = createOperationPanel("OPERASIONAL OFFLINE", "offline");
        offlinePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREEN_DARK, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        // Panel Online
        JPanel onlinePanel = createOperationPanel("OPERASIONAL ONLINE", "online");
        onlinePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 150, 243), 2), // Blue untuk online
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        // Panel Hari Libur
        JPanel holidayPanel = createHolidayPanel("HARI LIBUR");
        holidayPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 152, 0), 2), // Orange untuk libur
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        formContainer.add(offlinePanel);
        formContainer.add(onlinePanel);
        formContainer.add(holidayPanel);

        contentPanel.add(formContainer, gbc);

        // ================= BUTTON PANEL =================
        gbc.gridy = 1;
        gbc.insets = new Insets(30, 0, 0, 0);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setOpaque(false);

        // Tombol dengan icon
        btnSave = createIconButton("Simpan", "save.png", GREEN_PRIMARY);
        btnSave.addActionListener(e -> saveData());

        JButton btnCancel = createIconButton("Batal", "cancel.png", new Color(231, 76, 60));
        btnCancel.addActionListener(e -> dispose());

        JButton btnBack = createIconButton("Kembali", "back.png", new Color(153, 153, 153));
        btnBack.addActionListener(e -> {
            dispose();
            new AdminDashboard(userId, adminName, userCode).setVisible(true);
        });

        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnBack);

        contentPanel.add(buttonPanel, gbc);

        // Layout utama
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    // ================= METHOD UNTUK LOAD ICON SETTING.PNG =================
    private ImageIcon loadSettingsIcon() {
        try {
            // Coba load dari file setting.png
            ImageIcon originalIcon = new ImageIcon("setting.png");

            // Jika file tidak ditemukan, coba dari resources
            if (originalIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                originalIcon = new ImageIcon(getClass().getResource("/img/setting.png"));
            }

            // Jika masih tidak ditemukan, buat icon default
            if (originalIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                return createDefaultSettingsIcon();
            }

            // Scale icon ke ukuran 40x40
            Image img = originalIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            return new ImageIcon(img);

        } catch (Exception e) {
            System.out.println("Gagal load setting.png, menggunakan icon default");
            return createDefaultSettingsIcon();
        }
    }

    // ================= METHOD UNTUK BUAT ICON DEFAULT SETTINGS =================
    private ImageIcon createDefaultSettingsIcon() {
        // Buat gambar default 40x40
        ImageIcon icon = new ImageIcon(createDefaultSettingsImage());
        return icon;
    }

    private Image createDefaultSettingsImage() {
        int width = 40;
        int height = 40;

        // Create a buffered image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background KOTAK (Square)
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Draw border KOTAK
        g2d.setColor(GREEN_DARK);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRect(0, 0, width, height);

        // Draw gear/settings icon
        g2d.setColor(GREEN_DARK);

        // Draw center circle (keep this circle as it is part of the icon
        // representation)
        g2d.fillOval(width / 2 - 4, height / 2 - 4, 8, 8);

        // Draw gear teeth - keep rectangular
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            int x1 = (int) (width / 2 + Math.cos(angle) * 12);
            int y1 = (int) (height / 2 + Math.sin(angle) * 12);
            int x2 = (int) (width / 2 + Math.cos(angle) * 16);
            int y2 = (int) (height / 2 + Math.sin(angle) * 16);

            g2d.fillRect(x1 - 2, y1 - 2, 4, y2 - y1 + 4);
        }

        g2d.dispose();
        return image;
    }

    private void connectDatabase() {
        try {
            String url = "jdbc:mysql://localhost:3306/distrozone_db";
            String user = "root";
            String password = "";
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Database connected ✅");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Koneksi database gagal!\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createOperationPanel(String title, String type) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(255, 255, 255, 230));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(type.equals("offline") ? GREEN_DARK : new Color(33, 150, 243), 2),
                BorderFactory.createEmptyBorder(20, 30, 20, 30)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(type.equals("offline") ? GREEN_DARK : new Color(33, 150, 243));
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblTitle, gbc);

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        // Jam Buka
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(createLabel("Jam Buka (HH:MM):"), gbc);

        JTextField txtOpen = createTextField();
        txtOpen.setToolTipText("Format: 08:00");
        txtOpen.setPreferredSize(new Dimension(180, 35));
        util.InputValidator.restrictToTimeFormat(txtOpen); // Only allow HH:MM format
        gbc.gridx = 1;
        panel.add(txtOpen, gbc);

        // Jam Tutup
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(createLabel("Jam Tutup (HH:MM):"), gbc);

        JTextField txtClose = createTextField();
        txtClose.setToolTipText("Format: 21:00");
        txtClose.setPreferredSize(new Dimension(180, 35));
        util.InputValidator.restrictToTimeFormat(txtClose); // Only allow HH:MM format
        gbc.gridx = 1;
        panel.add(txtClose, gbc);

        // Hari Tutup (Checkboxes)
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(createLabel("Hari Tutup (Libur Rutin):"), gbc);

        gbc.gridy = 4;
        JPanel daysPanel = new JPanel(new GridLayout(2, 4, 10, 10)); // 2 rows, 4 cols
        daysPanel.setOpaque(false);

        String[] dayNames = { "Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu" };
        JCheckBox[] chkDays = new JCheckBox[7];

        for (int i = 0; i < 7; i++) {
            chkDays[i] = new JCheckBox(dayNames[i]);
            chkDays[i].setOpaque(false);
            chkDays[i].setFont(new Font("Segoe UI", Font.PLAIN, 13));
            daysPanel.add(chkDays[i]);
        }
        panel.add(daysPanel, gbc);

        // Deskripsi tambahan
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        if (type.equals("offline")) {
            JLabel lblDesc = new JLabel(
                    "<html><small><i>🏪 Toko Fisik DistroZone<br>• Centang hari yang <b>TUTUP</b><br>• Sinkron dengan Website</i></small></html>");
            lblDesc.setForeground(new Color(100, 100, 100));
            lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            panel.add(lblDesc, gbc);
        } else {
            JLabel lblDesc = new JLabel(
                    "<html><small><i>🛒 Penjualan Web/Online<br>• Centang hari <b>SLOW RESPONSE</b><br>• Sinkron dengan Website</i></small></html>");
            lblDesc.setForeground(new Color(100, 100, 100));
            lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            panel.add(lblDesc, gbc);
        }

        // Store references
        if (type.equals("offline")) {
            chkOfflineDays = chkDays;
            txtOfflineOpen = txtOpen;
            txtOfflineClose = txtClose;
        } else {
            chkOnlineDays = chkDays;
            txtOnlineOpen = txtOpen;
            txtOnlineClose = txtClose;
        }

        return panel;
    }

    private JPanel createHolidayPanel(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(255, 255, 255, 230));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 152, 0), 2),
                BorderFactory.createEmptyBorder(20, 30, 20, 30)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 10, 12, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(255, 152, 0));
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblTitle, gbc);

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        // Hari Libur
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(createLabel("Hari Libur (tambah lewat tombol kalender):"), gbc);

        gbc.gridy = 2;

        JPanel pnlHolidayInput = new JPanel(new BorderLayout(5, 0));
        pnlHolidayInput.setOpaque(false);

        txtHolidays = new JTextArea();
        txtHolidays.setLineWrap(true);
        txtHolidays.setWrapStyleWord(true);
        txtHolidays.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtHolidays.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        txtHolidays.setToolTipText("Klik tombol kalender untuk menambah tanggal libur");
        txtHolidays.setEditable(false); // Hanya bisa input lewat date picker
        txtHolidays.setBackground(new Color(250, 250, 250));

        JButton btnAddDate = new JButton();
        // Try to load calendar icon, fallback to riwayat.png
        ImageIcon calIcon = new ImageIcon(getClass().getResource("/img/calendar.png"));
        // Note: Using riwayat.png as placeholder for calendar if calendar.png doesn't
        // exist
        try {
            java.net.URL calUrl = getClass().getResource("/img/calendar.png");
            if (calUrl != null)
                calIcon = new ImageIcon(calUrl);
        } catch (Exception e) {
        }

        if (calIcon != null) {
            Image img = calIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            btnAddDate.setIcon(new ImageIcon(img));
        } else {
            btnAddDate.setText("+"); // Fallback text if no image
        }

        btnAddDate.setContentAreaFilled(false); // Transparent background
        btnAddDate.setBorderPainted(false);
        btnAddDate.setFocusPainted(false);
        btnAddDate.setToolTipText("Tambah Tanggal Libur");
        btnAddDate.setPreferredSize(new Dimension(40, 40));

        btnAddDate.addActionListener(e -> {
            java.awt.Window win = javax.swing.SwingUtilities.getWindowAncestor(this);
            SimpleDatePicker picker;
            if (win instanceof Frame)
                picker = new SimpleDatePicker((Frame) win);
            else if (win instanceof Dialog)
                picker = new SimpleDatePicker((Dialog) win);
            else
                picker = new SimpleDatePicker((Frame) null); // Fallback

            picker.setVisible(true);
            String date = picker.getSelectedDate();
            if (date != null && !date.isEmpty()) {
                String current = txtHolidays.getText().trim();
                // Check if date already exists
                if (current.contains(date)) {
                    JOptionPane.showMessageDialog(this, "Tanggal sudah ada!", "Duplikat", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                // Check if empty or ends with comma
                if (!current.isEmpty() && !current.endsWith(",")) {
                    current += ", ";
                }
                txtHolidays.setText(current + date);
                txtHolidays.revalidate();
                txtHolidays.repaint();
            }
        });

        // Button to clear/remove dates
        JButton btnClearDates = new JButton();
        btnClearDates.setText("X");
        btnClearDates.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClearDates.setForeground(Color.WHITE);
        btnClearDates.setBackground(new Color(220, 53, 69));
        btnClearDates.setContentAreaFilled(true);
        btnClearDates.setBorderPainted(false);
        btnClearDates.setFocusPainted(false);
        btnClearDates.setToolTipText("Hapus Semua Tanggal Libur");
        btnClearDates.setPreferredSize(new Dimension(40, 40));
        btnClearDates.setOpaque(true);

        btnClearDates.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Hapus semua tanggal libur?", "Konfirmasi",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                txtHolidays.setText("");
            }
        });

        JScrollPane scrollPane = new JScrollPane(txtHolidays);
        scrollPane.setPreferredSize(new Dimension(200, 80));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(255, 152, 0), 1)); // Border on scrollpane

        pnlHolidayInput.add(scrollPane, BorderLayout.CENTER);

        JPanel pnlBtnObj = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        pnlBtnObj.setOpaque(false);
        pnlBtnObj.add(btnAddDate);
        pnlBtnObj.add(btnClearDates);
        pnlHolidayInput.add(pnlBtnObj, BorderLayout.EAST);

        panel.add(pnlHolidayInput, gbc);

        // Deskripsi
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JLabel lblDesc = new JLabel(
                "<html><small><i>📌 Untuk validasi transaksi:<br>• Cek hari libur sebelum transaksi<br>• Tampilkan info di dashboard<br>• Beri warning saat hari libur</i></small></html>");
        lblDesc.setForeground(new Color(100, 100, 100));
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        panel.add(lblDesc, gbc);

        // Info penggunaan
        gbc.gridy = 4;
        JLabel lblUsage = new JLabel(
                "<html><small><b>Digunakan untuk:</b><br>Validasi transaksi offline<br>Informasi di dashboard admin<br>Pemberitahuan sistem</small></html>");
        lblUsage.setForeground(new Color(44, 62, 80));
        lblUsage.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(lblUsage, gbc);

        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(TEXT_DARK);
        return label;
    }

    private JTextField createTextField() {
        JTextField textField = new JTextField();
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setPreferredSize(new Dimension(180, 35));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREEN_DARK, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return textField;
    }

    // ================= METHOD UNTUK BUAT TOMBOL DENGAN ICON =================
    private JButton createIconButton(String text, String iconName, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btn.setPreferredSize(new Dimension(140, 40));

        // Coba load icon
        try {
            ImageIcon originalIcon = new ImageIcon(iconName);

            // Jika file tidak ditemukan, coba dari resources
            if (originalIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                originalIcon = new ImageIcon(getClass().getResource("/img/" + iconName));
            }

            // Jika masih tidak ditemukan, buat icon default
            if (originalIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                originalIcon = createDefaultButtonIcon(iconName);
            }

            // Scale icon ke ukuran 20x20
            Image img = originalIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(img));
            btn.setHorizontalTextPosition(SwingConstants.RIGHT);
            btn.setIconTextGap(8);

        } catch (Exception e) {
            System.out.println("Gagal load icon " + iconName + ", menggunakan tombol tanpa icon");
        }

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                // btn.setBackground(bgColor.darker()); // handled by UI
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                // btn.setBackground(bgColor); // handled by UI
            }
        });

        // Custom paint for consistent styling (prevents white "glitch") - SQUARE
        // VERSION
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

                // KOTAK (Square) - No rounding
                g2.fillRect(0, 0, c.getWidth(), c.getHeight());
                g2.dispose();

                super.paint(g, c);
            }
        });

        return btn;
    }

    // ================= METHOD UNTUK BUAT ICON DEFAULT TOMBOL =================
    private ImageIcon createDefaultButtonIcon(String iconName) {
        int width = 20;
        int height = 20;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Warna background icon
        Color iconColor = Color.WHITE;
        g2d.setColor(iconColor);

        if (iconName.contains("save")) {
            // Icon save (disket)
            g2d.fillRect(2, 5, 16, 12); // Body
            g2d.fillRect(4, 2, 12, 3); // Top

            // Metal part
            g2d.setColor(new Color(200, 200, 200));
            g2d.fillRect(2, 5, 16, 2); // Top metal
            g2d.fillRect(7, 8, 6, 2); // Label

        } else if (iconName.contains("cancel")) {
            // Icon cancel (X)
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawLine(4, 4, 16, 16);
            g2d.drawLine(16, 4, 4, 16);

        } else if (iconName.contains("back")) {
            // Icon back (panah kiri)
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawLine(12, 10, 6, 10); // Horizontal line
            g2d.drawLine(6, 10, 9, 6); // Arrow head top
            g2d.drawLine(6, 10, 9, 14); // Arrow head bottom
        }

        g2d.dispose();
        return new ImageIcon(image);
    }

    private void loadData() {
        createSettingsTable();

        try {
            // Load pengaturan offline (JSON)
            String offlineJson = getSettingValue("jam_operasional_offline");
            if (offlineJson != null && offlineJson.contains("{")) {
                txtOfflineOpen.setText(getJsonValue(offlineJson, "open"));
                txtOfflineClose.setText(getJsonValue(offlineJson, "close"));

                List<Integer> closed = getJsonArray(offlineJson, "closed_days");
                for (int i = 0; i < 7; i++) {
                    chkOfflineDays[i].setSelected(closed.contains(i));
                }
            } else {
                // Fallback attempt to read old format (pipe delimited)
                String oldOffline = getSettingValue("offline_operasional");
                if (oldOffline != null && oldOffline.contains("|")) {
                    String[] parts = oldOffline.split("\\|");
                    if (parts.length >= 3) {
                        txtOfflineOpen.setText(parts[1]);
                        txtOfflineClose.setText(parts[2]);
                        // Default Senin closed if migrating
                        chkOfflineDays[1].setSelected(true);
                    }
                } else {
                    // Default
                    txtOfflineOpen.setText("10:00");
                    txtOfflineClose.setText("20:00");
                    chkOfflineDays[1].setSelected(true); // Senin libur
                }
            }

            // Load pengaturan online (JSON)
            String onlineJson = getSettingValue("jam_operasional_online");
            if (onlineJson != null && onlineJson.contains("{")) {
                txtOnlineOpen.setText(getJsonValue(onlineJson, "open"));
                txtOnlineClose.setText(getJsonValue(onlineJson, "close"));

                List<Integer> closed = getJsonArray(onlineJson, "closed_days");
                for (int i = 0; i < 7; i++) {
                    chkOnlineDays[i].setSelected(closed.contains(i));
                }
            } else {
                // Fallback or default
                txtOnlineOpen.setText("09:00");
                txtOnlineClose.setText("17:00");
            }

            // Load hari libur
            String holidaysData = getSettingValue("hari_libur");
            if (holidaysData != null) {
                txtHolidays.setText(holidaysData);
            } else {
                txtHolidays.setText("Senin, 25 Desember, 1 Januari, 17 Agustus");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "❌ Gagal memuat data: " + e.getMessage());
            setDefaultValues();
        }
    }

    private void setDefaultValues() {
        txtOfflineOpen.setText("10:00");
        txtOfflineClose.setText("20:00");
        for (JCheckBox cb : chkOfflineDays)
            cb.setSelected(false);
        chkOfflineDays[1].setSelected(true); // Senin

        txtOnlineOpen.setText("09:00");
        txtOnlineClose.setText("17:00");
        for (JCheckBox cb : chkOnlineDays)
            cb.setSelected(false);

        txtHolidays.setText("Senin, 25 Desember, 1 Januari, 17 Agustus");
    }

    private void createSettingsTable() {
        // Cek dulu apakah tabel perlu dimigrasi dari schema lama
        try (Statement stmt = conn.createStatement()) {
            // Cek kolom yang ada
            boolean hasNamaSetting = false;
            boolean hasSettingKey = false;
            try {
                ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM settings");
                while (rs.next()) {
                    String field = rs.getString("Field");
                    if ("nama_setting".equalsIgnoreCase(field))
                        hasNamaSetting = true;
                    if ("setting_key".equalsIgnoreCase(field))
                        hasSettingKey = true;
                }
            } catch (SQLException e) {
                // Tabel mungkin belum ada, ignore
            }

            // Jika ada nama_setting tapi tidak ada setting_key, lakukan migrasi
            if (hasNamaSetting && !hasSettingKey) {
                System.out.println("⚠️ Mendeteksi schema lama, melakukan migrasi...");
                stmt.execute("ALTER TABLE settings CHANGE nama_setting setting_key VARCHAR(100) NOT NULL UNIQUE");
                try {
                    stmt.execute("ALTER TABLE settings CHANGE isi_setting setting_value TEXT");
                    System.out.println("✅ Schema berhasil dimigrasi: nm_setting -> setting_key");
                } catch (Exception ex) {
                    System.out.println("⚠️ Gagal migrasi column isi_setting: " + ex.getMessage());
                }
            }
        } catch (SQLException e) {
            System.out.println("⚠️ Info Schema Check: " + e.getMessage());
        }

        String sql = "CREATE TABLE IF NOT EXISTS settings (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "setting_key VARCHAR(100) NOT NULL UNIQUE, " +
                "setting_value TEXT, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";

        // Default values sesuai deskripsi toko (SENIN LIBUR)
        String insertDefaults = "INSERT IGNORE INTO settings (setting_key, setting_value) VALUES " +
                "('offline_operasional', 'Selasa-Jumat|10:00|20:00'), " +
                "('online_operasional', 'Setiap Hari|10:00|17:00'), " +
                "('hari_libur', 'Senin, 25 Desember, 1 Januari, 17 Agustus')";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute(insertDefaults);
            System.out.println("✅ Tabel settings siap");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getSettingValue(String settingName) throws SQLException {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, settingName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("setting_value");
            }
        }
        return null;
    }

    private void saveData() {
        // Validasi input
        if (!isValidTimeFormat(txtOfflineOpen.getText()) || !isValidTimeFormat(txtOfflineClose.getText()) ||
                !isValidTimeFormat(txtOnlineOpen.getText()) || !isValidTimeFormat(txtOnlineClose.getText())) {
            JOptionPane.showMessageDialog(this,
                    "❌ Format jam tidak valid!\nGunakan format HH:MM atau keterangan text (contoh: 10:00, Libur, 24 Jam)",
                    "Error Validasi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // -- Save Offline --
            String offlineOpen = txtOfflineOpen.getText();
            String offlineClose = txtOfflineClose.getText();

            List<Integer> offlineClosed = new ArrayList<>();
            StringBuilder offlineDaysDesc = new StringBuilder();

            for (int i = 0; i < 7; i++) {
                if (chkOfflineDays[i].isSelected()) {
                    offlineClosed.add(i);
                    offlineDaysDesc.append(chkOfflineDays[i].getText()).append(", ");
                }
            }

            String offlineClosedStr = offlineClosed.stream().map(String::valueOf).collect(Collectors.joining(","));
            String offlineJson = String.format("{\"open\":\"%s\",\"close\":\"%s\",\"closed_days\":[%s]}",
                    offlineOpen, offlineClose, offlineClosedStr);

            saveSetting("jam_operasional_offline", offlineJson);

            // -- Save Online --
            String onlineOpen = txtOnlineOpen.getText();
            String onlineClose = txtOnlineClose.getText();

            List<Integer> onlineClosed = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                if (chkOnlineDays[i].isSelected())
                    onlineClosed.add(i);
            }

            String onlineClosedStr = onlineClosed.stream().map(String::valueOf).collect(Collectors.joining(","));
            String onlineJson = String.format("{\"open\":\"%s\",\"close\":\"%s\",\"closed_days\":[%s]}",
                    onlineOpen, onlineClose, onlineClosedStr);

            saveSetting("jam_operasional_online", onlineJson);

            // -- Save Holidays --
            String holidaysValue = txtHolidays.getText().trim();
            saveSetting("hari_libur", holidaysValue);

            JOptionPane.showMessageDialog(this,
                    "✅ Pengaturan berhasil disimpan!\n\n" +
                            "📋 Ringkasan Operasional DistroZone:\n" +
                            "==================================\n" +
                            "🏪 OFFLINE (Toko Fisik):\n" +
                            "• Jam: " + offlineOpen + " - " + offlineClose + "\n" +
                            "• Tutup: "
                            + (offlineDaysDesc.length() > 0 ? offlineDaysDesc.toString() : "Buka Setiap Hari") + "\n\n"
                            +
                            "🛒 ONLINE (Penjualan Web):\n" +
                            "• Jam: " + onlineOpen + " - " + onlineClose + "\n\n" +
                            "💡 Data tersimpan sinkron dengan Website.",
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "❌ Gagal menyimpan: " + e.getMessage());
        }
    }

    // Helper to parse JSON values manually (Regex)
    private String getJsonValue(String json, String key) {
        if (json == null)
            return "";
        try {
            // Simple match for "key":"value"
            String pattern = "\"" + key + "\":\"(.*?)\"";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(json);
            if (m.find())
                return m.group(1);
        } catch (Exception e) {
        }
        return "";
    }

    // Helper to parse JSON array manually (Regex)
    private List<Integer> getJsonArray(String json, String key) {
        List<Integer> result = new ArrayList<>();
        if (json == null)
            return result;
        try {
            // Simple match for "key":[values]
            String pattern = "\"" + key + "\":\\[(.*?)\\]";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(json);
            if (m.find()) {
                String content = m.group(1);
                if (!content.trim().isEmpty()) {
                    for (String s : content.split(",")) {
                        try {
                            result.add(Integer.parseInt(s.trim()));
                        } catch (Exception e) {
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return result;
    }

    private void saveSetting(String settingName, String value) throws SQLException {
        String sql = "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE setting_value = ?, updated_at = NOW()";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, settingName);
            pstmt.setString(2, value);
            pstmt.setString(3, value);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("✅ Setting '" + settingName + "' disimpan. Rows affected: " + rowsAffected);
        }
    }

    private boolean isValidTimeFormat(String time) {
        // Allow any text format since user might want "Libur", "24 Jam", "08:00 WIB"
        // Just ensure it's not too long or empty
        return time != null && !time.trim().isEmpty() && time.length() <= 50;
    }

    // ================= METHOD UNTUK DIPANGGIL DARI KELAS LAIN =================

    /**
     * Method untuk mendapatkan jam operasional offline
     * 
     * @return array [hari, jam_buka, jam_tutup]
     */
    public static String[] getOfflineSchedule() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/distrozone_db", "root", "");

            // Check new JSON format first
            String sql = "SELECT setting_value FROM settings WHERE setting_key = 'jam_operasional_offline'";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String json = rs.getString("setting_value");
                if (json != null && json.contains("{")) {
                    return parseScheduleJson(json);
                }
            }

            // Fallback to old key
            sql = "SELECT setting_value FROM settings WHERE setting_key = 'offline_operasional'";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                String data = rs.getString("setting_value");
                if (data != null && data.contains("|")) {
                    return data.split("\\|");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
            }
        }
        return new String[] { "Selasa-Jumat", "10:00", "20:00" }; // Default
    }

    /**
     * Method untuk mendapatkan jam operasional online
     * 
     * @return array [hari, jam_buka, jam_tutup]
     */
    public static String[] getOnlineSchedule() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/distrozone_db", "root", "");

            // Check new JSON format first
            String sql = "SELECT setting_value FROM settings WHERE setting_key = 'jam_operasional_online'";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String json = rs.getString("setting_value");
                if (json != null && json.contains("{")) {
                    return parseScheduleJson(json);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
            }
        }
        return new String[] { "Setiap Hari", "10:00", "17:00" }; // Default
    }

    // Helper static for parsing JSON schedule to String array
    private static String[] parseScheduleJson(String json) {
        String open = "";
        String close = "";
        String closedDesc = "Buka Setiap Hari";

        try {
            // Open
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"open\":\"(.*?)\"").matcher(json);
            if (m.find())
                open = m.group(1);

            // Close
            m = java.util.regex.Pattern.compile("\"close\":\"(.*?)\"").matcher(json);
            if (m.find())
                close = m.group(1);

            // Closed Days
            m = java.util.regex.Pattern.compile("\"closed_days\":\\[(.*?)\\]").matcher(json);
            if (m.find()) {
                String content = m.group(1);
                if (!content.trim().isEmpty()) {
                    List<String> days = new ArrayList<>();
                    String[] map = { "Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu" };
                    for (String s : content.split(",")) {
                        try {
                            int idx = Integer.parseInt(s.trim());
                            if (idx >= 0 && idx < 7)
                                days.add(map[idx]);
                        } catch (Exception e) {
                        }
                    }
                    if (!days.isEmpty()) {
                        closedDesc = String.join(", ", days) + " Tutup";
                    }
                }
            }
        } catch (Exception e) {
        }

        return new String[] { closedDesc, open, close };
    }

    /**
     * Method untuk mendapatkan daftar hari libur
     * 
     * @return String hari libur (dipisahkan koma)
     */
    public static String getHolidays() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/distrozone_db", "root", "");
            String sql = "SELECT setting_value FROM settings WHERE setting_key = 'hari_libur'";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("setting_value");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    /**
     * Method untuk mengecek apakah hari ini libur
     * 
     * @return true jika hari ini libur
     */
    public static boolean isTodayHoliday() {
        // 1. Check fixed holidays
        String holidays = getHolidays();
        String todayDate = java.time.LocalDate.now().toString(); // Format: yyyy-MM-dd

        if (holidays != null && !holidays.isEmpty()) {
            String[] holidayList = holidays.split(",");
            for (String h : holidayList) {
                // Support exact match for yyyy-MM-dd or contains
                if (h.trim().contains(todayDate))
                    return true;
            }
        }

        // 2. Check operational closing days (Offline) - Fixed logic
        try {
            String[] schedule = getOfflineSchedule();
            // schedule[0] is description string, we need raw data from DB for accuracy
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/distrozone_db", "root", "");
            String sql = "SELECT setting_value FROM settings WHERE setting_key = 'jam_operasional_offline'";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String json = rs.getString("setting_value");
                if (json != null) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"closed_days\":\\[(.*?)\\]")
                            .matcher(json);
                    if (m.find()) {
                        String content = m.group(1);
                        // Java DayOfWeek: 1=Mon, 7=Sun. Web/JSON: 1=Mon, 0=Sun
                        int todayJava = java.time.LocalDate.now().getDayOfWeek().getValue();
                        int todayWeb = (todayJava == 7) ? 0 : todayJava;

                        for (String s : content.split(",")) {
                            if (s.trim().equals(String.valueOf(todayWeb))) {
                                conn.close();
                                return true;
                            }
                        }
                    }
                }
            }
            conn.close();
        } catch (Exception e) {
        }

        return false;
    }

    // ================= INNER CLASS FOR DATE PICKER =================
    private class SimpleDatePicker extends JDialog {
        private String selectedDate = "";
        private JLabel lblMonth;
        private JPanel pnlDays;
        private java.util.Calendar currentCal;
        private JDialog parent;
        private boolean okPressed = false;

        public SimpleDatePicker(Frame owner) {
            super(owner, "Pilih Tanggal", true);
            init();
        }

        public SimpleDatePicker(Dialog owner) {
            super(owner, "Pilih Tanggal", true);
            init();
        }

        private void init() {
            setSize(400, 450); // Increased height to ensure all rows fit
            setLocationRelativeTo(getOwner());
            setLayout(new BorderLayout());
            currentCal = java.util.Calendar.getInstance();

            // Header
            JPanel pnlHeader = new JPanel(new FlowLayout(FlowLayout.CENTER));
            pnlHeader.setBackground(GREEN_PRIMARY);

            JButton btnPrev = new JButton("<");
            styleNavButton(btnPrev);
            btnPrev.addActionListener(e -> {
                currentCal.add(java.util.Calendar.MONTH, -1);
                updateCalendar();
            });

            lblMonth = new JLabel();
            lblMonth.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lblMonth.setForeground(Color.WHITE);

            JButton btnNext = new JButton(">");
            styleNavButton(btnNext);
            btnNext.addActionListener(e -> {
                currentCal.add(java.util.Calendar.MONTH, 1);
                updateCalendar();
            });

            pnlHeader.add(btnPrev);
            pnlHeader.add(lblMonth);
            pnlHeader.add(btnNext);
            add(pnlHeader, BorderLayout.NORTH);

            // Body
            pnlDays = new JPanel(new GridLayout(0, 7, 5, 5));
            pnlDays.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            pnlDays.setBackground(Color.WHITE);
            add(pnlDays, BorderLayout.CENTER);

            updateCalendar();
        }

        private void styleNavButton(JButton btn) {
            btn.setContentAreaFilled(false);
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
            btn.setFocusPainted(false);
        }

        private void updateCalendar() {
            pnlDays.removeAll();

            // Month Label
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM yyyy");
            lblMonth.setText(sdf.format(currentCal.getTime()));

            // Headers
            String[] headers = { "Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab" };
            for (String h : headers) {
                JLabel lbl = new JLabel(h, SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setForeground(GREEN_DARK);
                pnlDays.add(lbl);
            }

            // Days
            java.util.Calendar cal = (java.util.Calendar) currentCal.clone();
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
            int startDay = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1;
            int daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);

            for (int i = 0; i < startDay; i++) {
                pnlDays.add(new JLabel(""));
            }

            for (int i = 1; i <= daysInMonth; i++) {
                final int day = i;
                JButton btn = new JButton(String.valueOf(i));
                btn.setFocusPainted(false);
                btn.setBackground(Color.WHITE);
                btn.setForeground(Color.BLACK); // Ensure text is visible
                btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));

                // Highlight today
                if (isToday(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), i)) {
                    btn.setBorder(BorderFactory.createLineBorder(GREEN_PRIMARY, 2));
                    btn.setForeground(GREEN_PRIMARY);
                }

                btn.addActionListener(e -> {
                    java.util.Calendar selected = (java.util.Calendar) currentCal.clone();
                    selected.set(java.util.Calendar.DAY_OF_MONTH, day);
                    java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
                    selectedDate = fmt.format(selected.getTime());
                    okPressed = true;
                    dispose();
                });
                pnlDays.add(btn);
            }

            pnlDays.revalidate();
            pnlDays.repaint();
        }

        private boolean isToday(int y, int m, int d) {
            java.util.Calendar now = java.util.Calendar.getInstance();
            return now.get(java.util.Calendar.YEAR) == y &&
                    now.get(java.util.Calendar.MONTH) == m &&
                    now.get(java.util.Calendar.DAY_OF_MONTH) == d;
        }

        public String getSelectedDate() {
            return okPressed ? selectedDate : null;
        }
    }

    @Override
    public void dispose() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("✅ Koneksi database ditutup");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        super.dispose();
    }

    // Untuk kompatibilitas jika ada yang memanggil main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new PengaturanOperasional(1, "Admin", "ADM001").setVisible(true);
        });
    }
}