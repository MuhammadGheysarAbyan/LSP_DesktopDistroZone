import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoadingScreen extends JFrame {
    private JProgressBar progressBar;
    private Timer timer;
    private int progress = 0;
    private JLabel lblLoading;
    private JLabel lblPercent;

    // Warna tema hijau Distro Zone (Emerald Green - Sesuai Web)
    private static final Color GREEN_PRIMARY = new Color(16, 185, 129); // #10B981
    private static final Color GREEN_DARK = new Color(15, 118, 110); // #0F766E
    private static final Color GREEN_LIGHT = new Color(52, 211, 153); // #34D399
    private static final Color TEXT_DARK = new Color(31, 41, 55); // #1F2937
    private static final Color TEXT_GRAY = new Color(100, 116, 139); // #64748B

    public LoadingScreen() {
        setTitle("Distro Zone - Loading...");
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Gradient background dengan tema hijau
        JPanel background = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                Color color1 = GREEN_PRIMARY;
                Color color2 = Color.WHITE;
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, getHeight(), color2);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        background.setLayout(new GridBagLayout());
        add(background);

        // Card container
        JPanel card = createCard();
        background.add(card);

        startLoading();
        setVisible(true);
    }

    private JPanel createCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 240));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(450, 400));
        card.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Logo
        JLabel lblLogo = createLogo();
        card.add(lblLogo, gbc);

        // Title
        gbc.gridy++;
        JLabel lblTitle = new JLabel("DISTRO ZONE", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblTitle.setForeground(TEXT_DARK);
        card.add(lblTitle, gbc);

        // Subtitle
        gbc.gridy++;
        gbc.insets = new Insets(5, 20, 20, 20);
        JLabel lblSubtitle = new JLabel("Kasir App", JLabel.CENTER);
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSubtitle.setForeground(TEXT_GRAY);
        card.add(lblSubtitle, gbc);

        // Loading status
        gbc.gridy++;
        gbc.insets = new Insets(20, 20, 10, 20);
        lblLoading = new JLabel("Initializing application...", JLabel.CENTER);
        lblLoading.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblLoading.setForeground(TEXT_GRAY);
        card.add(lblLoading, gbc);

        // Progress bar
        gbc.gridy++;
        gbc.insets = new Insets(10, 20, 5, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        progressBar = createProgressBar();
        card.add(progressBar, gbc);

        // Percentage
        gbc.gridy++;
        gbc.insets = new Insets(5, 20, 10, 20);
        gbc.fill = GridBagConstraints.NONE;
        lblPercent = new JLabel("0%", JLabel.CENTER);
        lblPercent.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblPercent.setForeground(GREEN_PRIMARY);
        card.add(lblPercent, gbc);

        // Loading dots
        gbc.gridy++;
        gbc.insets = new Insets(10, 20, 20, 20);
        JLabel lblDots = createLoadingDots();
        card.add(lblDots, gbc);

        return card;
    }

    private JLabel createLogo() {
        JLabel lblLogo;
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/img/logodistrozone.png"));
            Image img = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            lblLogo = new JLabel(new ImageIcon(img), JLabel.CENTER);
        } catch (Exception e) {
            // Custom drawn logo dengan tema distro
            lblLogo = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int size = 60;
                    int x = (getWidth() - size) / 2;
                    int y = (getHeight() - size) / 2;

                    // Modern logo untuk distro
                    g2.setColor(GREEN_PRIMARY);
                    g2.fillRoundRect(x, y, size, size, 15, 15);

                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(3f));

                    // Bentuk baju/t-shirt
                    int[] xPoints = { x + 20, x + 40, x + 35, x + 25, x + 20 };
                    int[] yPoints = { y + 20, y + 20, y + 40, y + 40, y + 30 };
                    g2.drawPolygon(xPoints, yPoints, 5);

                    // Kerah baju
                    g2.drawLine(x + 20, y + 20, x + 22, y + 15);
                    g2.drawLine(x + 40, y + 20, x + 38, y + 15);
                    g2.drawLine(x + 22, y + 15, x + 38, y + 15);
                }
            };
            lblLogo.setPreferredSize(new Dimension(80, 80));
        }
        return lblLogo;
    }

    private JProgressBar createProgressBar() {
        JProgressBar bar = new JProgressBar(0, 100) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Background
                g2.setColor(new Color(230, 230, 230));
                g2.fillRoundRect(0, 0, w, h, h, h);

                // Progress
                if (getValue() > 0) {
                    int progressWidth = (int) ((double) getValue() / getMaximum() * w);
                    g2.setColor(GREEN_PRIMARY);
                    g2.fillRoundRect(0, 0, progressWidth, h, h, h);

                    // Shine effect
                    g2.setColor(new Color(255, 255, 255, 80));
                    g2.fillRoundRect(0, 0, progressWidth, h / 2, h / 2, h / 2);

                    // Striped effect
                    g2.setColor(new Color(255, 255, 255, 30));
                    for (int i = 0; i < progressWidth; i += 10) {
                        g2.drawLine(i, 0, i, h);
                    }
                }
            }
        };
        bar.setPreferredSize(new Dimension(300, 12));
        bar.setOpaque(false);
        bar.setBorderPainted(false);
        return bar;
    }

    private JLabel createLoadingDots() {
        JLabel lblDots = new JLabel("●●●", JLabel.CENTER);
        lblDots.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblDots.setForeground(GREEN_PRIMARY);

        // Animate dots dengan efek yang lebih smooth
        Timer dotsTimer = new Timer(300, new ActionListener() {
            private int dotState = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                String[] states = { "●○○", "●●○", "●●●", "○●●", "○○●", "○○○" };
                lblDots.setText(states[dotState]);
                // Ganti warna untuk efek animasi
                if (dotState < 3) {
                    lblDots.setForeground(GREEN_PRIMARY);
                } else {
                    lblDots.setForeground(GREEN_LIGHT);
                }
                dotState = (dotState + 1) % states.length;
            }
        });
        dotsTimer.start();

        return lblDots;
    }

    private void startLoading() {
        timer = new Timer(35, e -> {
            progress++;
            progressBar.setValue(progress);
            lblPercent.setText(progress + "%");

            // Update status dengan pesan khusus untuk distro
            if (progress < 15) {
                lblLoading.setText("Starting Distro Zone...");
            } else if (progress < 30) {
                lblLoading.setText("Loading fashion database...");
            } else if (progress < 45) {
                lblLoading.setText("Connecting to inventory...");
            } else if (progress < 60) {
                lblLoading.setText("Loading product catalog...");
            } else if (progress < 75) {
                lblLoading.setText("Initializing POS system...");
            } else if (progress < 90) {
                lblLoading.setText("Preparing sales module...");
            } else if (progress < 95) {
                lblLoading.setText("Finalizing setup...");
            } else {
                lblLoading.setText("Ready to serve customers!");
            }

            if (progress >= 100) {
                timer.stop();
                lblLoading.setText("System Ready!");
                lblPercent.setForeground(GREEN_DARK);

                Timer delayTimer = new Timer(500, evt -> {
                    ((Timer) evt.getSource()).stop();
                    openLogin();
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
            }
        });
        timer.start();
    }

    private void openLogin() {
        // Fade effect dengan warna hijau
        Timer fadeTimer = new Timer(30, new ActionListener() {
            float opacity = 1.0f;

            @Override
            public void actionPerformed(ActionEvent e) {
                opacity -= 0.1f;
                if (opacity <= 0) {
                    ((Timer) e.getSource()).stop();
                    dispose();
                    SwingUtilities.invokeLater(() -> {
                        // Pastikan LoginForm juga menggunakan warna hijau
                        new LoginForm().setVisible(true);
                    });
                } else {
                    setOpacity(Math.max(0, opacity));
                    // Efek perubahan warna background selama fade
                    float greenValue = opacity * 0.8f;
                    getContentPane().setBackground(
                            new Color(76, 175, 80, (int) (opacity * 255)));
                }
            }
        });
        fadeTimer.start();
    }

    public static void main(String[] args) {
        // Set look and feel untuk tampilan yang lebih modern
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new LoadingScreen();
        });
    }
}