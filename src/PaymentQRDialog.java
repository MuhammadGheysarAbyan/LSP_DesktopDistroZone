import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Random;
import java.util.Locale;

/**
 * Dialog untuk menampilkan QR Code (QRIS) atau Virtual Account (Transfer)
 * saat pembayaran non-tunai
 */
public class PaymentQRDialog extends JDialog {

    private static final Color GREEN_PRIMARY = new Color(16, 185, 129);
    private static final Color GREEN_DARK = new Color(4, 120, 87);
    private static final Color GREEN_LIGHT = new Color(236, 253, 245);
    private static final Color BLUE_PRIMARY = new Color(59, 130, 246);

    private static final DecimalFormat RUPIAH;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        RUPIAH = new DecimalFormat("#,##0", symbols);
    }

    private boolean paymentConfirmed = false;
    private Timer countdownTimer;
    private int countdown = 30; // 30 detik countdown

    /**
     * Constructor untuk PaymentQRDialog
     * 
     * @param parent        Parent frame
     * @param paymentMethod Metode pembayaran ("QRIS" atau "Transfer BCA")
     * @param amount        Jumlah pembayaran
     * @param kodeTransaksi Kode transaksi untuk referensi
     */
    public PaymentQRDialog(JFrame parent, String paymentMethod, double amount, String kodeTransaksi) {
        super(parent, "Pembayaran " + paymentMethod, true);
        setSize(450, 550);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        // Main panel with gradient
        JPanel mainPanel = new JPanel(new BorderLayout(0, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, GREEN_LIGHT, 0, getHeight(), Color.WHITE);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        mainPanel.setOpaque(false);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel(
                paymentMethod.equals("QRIS") ? "📱 Scan QR untuk Bayar" : "🏦 Transfer ke Virtual Account");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(GREEN_DARK);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblAmount = new JLabel("Rp " + RUPIAH.format(amount));
        lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblAmount.setForeground(GREEN_PRIMARY);
        lblAmount.setHorizontalAlignment(SwingConstants.CENTER);

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblAmount, BorderLayout.CENTER);

        // Content Panel (QR atau VA)
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREEN_PRIMARY, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        if (paymentMethod.equals("QRIS")) {
            // Generate QR-like image
            JLabel lblQR = new JLabel();
            lblQR.setIcon(generateQRCodeSimulation(200, kodeTransaksi, amount));
            lblQR.setHorizontalAlignment(SwingConstants.CENTER);
            contentPanel.add(lblQR, BorderLayout.CENTER);

            JLabel lblQRInfo = new JLabel(
                    "<html><center>Scan QR diatas menggunakan<br>aplikasi e-wallet atau mobile banking</center></html>");
            lblQRInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblQRInfo.setForeground(Color.GRAY);
            lblQRInfo.setHorizontalAlignment(SwingConstants.CENTER);
            contentPanel.add(lblQRInfo, BorderLayout.SOUTH);

        } else {
            // Virtual Account Display
            String vaNumber = generateVirtualAccount();

            JPanel vaPanel = new JPanel(new GridLayout(3, 1, 5, 10));
            vaPanel.setOpaque(false);

            JLabel lblBank = new JLabel("Bank BCA");
            lblBank.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lblBank.setForeground(BLUE_PRIMARY);
            lblBank.setHorizontalAlignment(SwingConstants.CENTER);

            JLabel lblVA = new JLabel(vaNumber);
            lblVA.setFont(new Font("Consolas", Font.BOLD, 24));
            lblVA.setForeground(GREEN_DARK);
            lblVA.setHorizontalAlignment(SwingConstants.CENTER);
            lblVA.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GREEN_PRIMARY, 1),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)));
            lblVA.setOpaque(true);
            lblVA.setBackground(Color.WHITE);

            JButton btnCopy = new JButton("📋 Salin Nomor VA");
            btnCopy.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            btnCopy.setForeground(Color.WHITE);
            btnCopy.setBackground(BLUE_PRIMARY);
            btnCopy.setFocusPainted(false);
            btnCopy.setBorderPainted(false);
            btnCopy.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnCopy.addActionListener(e -> {
                java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(vaNumber);
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                btnCopy.setText("✅ Tersalin!");
                Timer resetTimer = new Timer(2000, evt -> btnCopy.setText("📋 Salin Nomor VA"));
                resetTimer.setRepeats(false);
                resetTimer.start();
            });

            vaPanel.add(lblBank);
            vaPanel.add(lblVA);
            vaPanel.add(btnCopy);

            contentPanel.add(vaPanel, BorderLayout.CENTER);

            JLabel lblVAInfo = new JLabel(
                    "<html><center>Transfer ke nomor VA diatas melalui<br>ATM, Mobile Banking, atau Internet Banking</center></html>");
            lblVAInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblVAInfo.setForeground(Color.GRAY);
            lblVAInfo.setHorizontalAlignment(SwingConstants.CENTER);
            contentPanel.add(lblVAInfo, BorderLayout.SOUTH);
        }

        // Footer dengan countdown dan tombol
        JPanel footerPanel = new JPanel(new BorderLayout(0, 10));
        footerPanel.setOpaque(false);

        JLabel lblCountdown = new JLabel("⏳ Menunggu pembayaran... (" + countdown + " detik)");
        lblCountdown.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblCountdown.setForeground(Color.GRAY);
        lblCountdown.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        btnPanel.setOpaque(false);

        JButton btnConfirm = new JButton("✅ Sudah Bayar");
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setBackground(GREEN_PRIMARY);
        btnConfirm.setFocusPainted(false);
        btnConfirm.setBorderPainted(false);
        btnConfirm.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConfirm.setPreferredSize(new Dimension(140, 40));

        JButton btnCancel = new JButton("❌ Batal");
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setBackground(new Color(239, 68, 68));
        btnCancel.setFocusPainted(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.setPreferredSize(new Dimension(120, 40));

        btnPanel.add(btnConfirm);
        btnPanel.add(btnCancel);

        footerPanel.add(lblCountdown, BorderLayout.NORTH);
        footerPanel.add(btnPanel, BorderLayout.CENTER);

        // Event handlers
        btnConfirm.addActionListener(e -> {
            // Simulasi verifikasi pembayaran
            btnConfirm.setEnabled(false);
            btnConfirm.setText("Memverifikasi...");

            Timer verifyTimer = new Timer(1500, evt -> {
                paymentConfirmed = true;
                if (countdownTimer != null) {
                    countdownTimer.stop();
                }
                dispose();
            });
            verifyTimer.setRepeats(false);
            verifyTimer.start();
        });

        btnCancel.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Yakin ingin membatalkan pembayaran?",
                    "Konfirmasi Batal",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                paymentConfirmed = false;
                if (countdownTimer != null) {
                    countdownTimer.stop();
                }
                dispose();
            }
        });

        // Countdown timer
        countdownTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                countdown--;
                lblCountdown.setText("⏳ Menunggu pembayaran... (" + countdown + " detik)");

                if (countdown <= 0) {
                    countdownTimer.stop();
                    lblCountdown.setText("⚠️ Waktu habis! Klik 'Sudah Bayar' jika sudah transfer.");
                    lblCountdown.setForeground(new Color(234, 179, 8));
                }
            }
        });
        countdownTimer.start();

        // Handle window close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                btnCancel.doClick();
            }
        });

        // Layout
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    /**
     * Generate simulasi QR code (pattern sederhana)
     */
    private ImageIcon generateQRCodeSimulation(int size, String data, double amount) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background putih
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, size, size);

        // Generate pseudo-random pattern based on data hash
        Random random = new Random((data + amount).hashCode());
        int moduleSize = size / 25;

        g2.setColor(Color.BLACK);

        // Position detection patterns (corners)
        drawFinderPattern(g2, 0, 0, moduleSize);
        drawFinderPattern(g2, size - 7 * moduleSize, 0, moduleSize);
        drawFinderPattern(g2, 0, size - 7 * moduleSize, moduleSize);

        // Timing patterns
        for (int i = 8; i < 17; i++) {
            if (i % 2 == 0) {
                g2.fillRect(i * moduleSize, 6 * moduleSize, moduleSize, moduleSize);
                g2.fillRect(6 * moduleSize, i * moduleSize, moduleSize, moduleSize);
            }
        }

        // Random data modules
        for (int y = 9; y < 23; y++) {
            for (int x = 9; x < 23; x++) {
                if (random.nextBoolean()) {
                    g2.fillRect(x * moduleSize, y * moduleSize, moduleSize, moduleSize);
                }
            }
        }

        // Center logo area
        g2.setColor(Color.WHITE);
        g2.fillOval(size / 2 - 25, size / 2 - 25, 50, 50);
        g2.setColor(GREEN_PRIMARY);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        g2.drawString("QRIS", size / 2 - 18, size / 2 + 6);

        g2.dispose();
        return new ImageIcon(image);
    }

    private void drawFinderPattern(Graphics2D g2, int x, int y, int moduleSize) {
        // Outer black
        g2.setColor(Color.BLACK);
        g2.fillRect(x, y, 7 * moduleSize, 7 * moduleSize);

        // Inner white
        g2.setColor(Color.WHITE);
        g2.fillRect(x + moduleSize, y + moduleSize, 5 * moduleSize, 5 * moduleSize);

        // Center black
        g2.setColor(Color.BLACK);
        g2.fillRect(x + 2 * moduleSize, y + 2 * moduleSize, 3 * moduleSize, 3 * moduleSize);
    }

    /**
     * Generate Virtual Account number
     */
    private String generateVirtualAccount() {
        Random random = new Random();
        StringBuilder va = new StringBuilder("8277"); // Prefix BCA VA
        for (int i = 0; i < 12; i++) {
            va.append(random.nextInt(10));
        }
        // Format: XXXX XXXX XXXX XXXX
        return va.substring(0, 4) + " " + va.substring(4, 8) + " " + va.substring(8, 12) + " " + va.substring(12);
    }

    /**
     * Check if payment was confirmed
     */
    public boolean isPaymentConfirmed() {
        return paymentConfirmed;
    }

    /**
     * Static method to show payment dialog and return confirmation result
     */
    public static boolean showPaymentDialog(JFrame parent, String paymentMethod, double amount, String kodeTransaksi) {
        PaymentQRDialog dialog = new PaymentQRDialog(parent, paymentMethod, amount, kodeTransaksi);
        dialog.setVisible(true);
        return dialog.isPaymentConfirmed();
    }
}
