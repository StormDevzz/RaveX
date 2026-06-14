package ravex.loader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.*;

public class LoaderWindow extends JFrame {
    private static final Color BG_DARK = new Color(0x09, 0x06, 0x11);
    private static final Color BG_LIGHT = new Color(0x1a, 0x0f, 0x30);
    private static final Color ACCENT_PINK = new Color(0xff, 0x2a, 0x85);
    private static final Color ACCENT_PURPLE = new Color(0x9d, 0x4e, 0xdd);
    private static final Color ACCENT_BLUE = new Color(0x00, 0xb4, 0xd8);
    private static final Color TEXT_COLOR = new Color(0xf8, 0xf9, 0xfa);
    private static final Color TEXT_MUTED = new Color(0x9a, 0x8c, 0xb9);

    private String version = "1.2 NextGen";
    private String status = "Initializing...";
    private int percent = 0;
    private float animatedPercent = 0f;
    private boolean error = false;
    private String errorMsg = "";
    private int systemScore = -1;
    private String systemInfo = "";
    private String extraInfo = "";
    
    private float pulseAngle = 0f;
    private Timer animTimer;

    public LoaderWindow() {
        setTitle("RaveX NextGen Loader");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(540, 290);
        setResizable(false);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setBackground(BG_DARK);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFrame((Graphics2D) g);
            }
        };
        panel.setBackground(BG_DARK);
        panel.setDoubleBuffered(true);

        // Make window draggable
        final Point[] dragStart = new Point[1];
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart[0] = e.getPoint();
            }
        });
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart[0] != null) {
                    Point curr = e.getLocationOnScreen();
                    setLocation(curr.x - dragStart[0].x, curr.y - dragStart[0].y);
                }
            }
        });

        setContentPane(panel);

        // Animation loop running at 60 FPS
        animTimer = new Timer(16, e -> {
            // Smooth progress bar interpolation
            float diff = percent - animatedPercent;
            if (Math.abs(diff) > 0.05f) {
                animatedPercent += diff * 0.08f;
            } else {
                animatedPercent = percent;
            }

            // Pulse animation for neon glows
            pulseAngle += 0.05f;
            if (pulseAngle > Math.PI * 2) {
                pulseAngle = 0f;
            }

            repaint();
        });
        animTimer.start();
    }

    public void setVersion(String v) { 
        this.version = v; 
        repaint();
    }

    public void updateStatus(String text, int pct) {
        SwingUtilities.invokeLater(() -> {
            status = text;
            percent = Math.min(pct, 100);
        });
    }

    public void setError(String msg) {
        SwingUtilities.invokeLater(() -> {
            error = true;
            errorMsg = msg;
            status = "Initialization Error";
        });
    }

    public void setSystemInfo(String info) { 
        this.systemInfo = info; 
    }
    
    public void setExtraInfo(String info) { 
        this.extraInfo = info; 
    }
    
    public void setSystemScore(int score) { 
        this.systemScore = score; 
    }

    @Override
    public void dispose() {
        if (animTimer != null) {
            animTimer.stop();
        }
        super.dispose();
    }

    private void drawFrame(Graphics2D g) {
        int w = getWidth(), h = getHeight(), cx = w / 2;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Beautiful Space/Radial Background
        RadialGradientPaint bgGrad = new RadialGradientPaint(
            new Point2D.Float(cx, h / 2f), w * 0.8f,
            new float[]{0f, 0.7f, 1f},
            new Color[]{BG_LIGHT, BG_DARK, new Color(4, 2, 8)}
        );
        g.setPaint(bgGrad);
        g.fillRect(0, 0, w, h);

        // 2. Cyber Grid/Decorative Lines
        g.setColor(new Color(0x3f, 0x2d, 0x6e, 25));
        g.setStroke(new BasicStroke(1f));
        int gridSpacing = 20;
        for (int x = 0; x < w; x += gridSpacing) {
            g.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += gridSpacing) {
            g.drawLine(0, y, w, y);
        }

        // Pulse intensity calculated from sin wave
        float pulse = (float) (Math.sin(pulseAngle) + 1f) / 2f; // [0, 1]
        int glowAlpha = 50 + (int)(pulse * 60); // [50, 110]

        // 3. Gorgeous Neon Border
        GradientPaint borderGrad = new GradientPaint(
            0, 0, ACCENT_PINK,
            w, h, ACCENT_PURPLE
        );
        g.setPaint(borderGrad);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(1, 1, w - 2, h - 2);

        // Outer Glow for border
        g.setColor(new Color(ACCENT_PURPLE.getRed(), ACCENT_PURPLE.getGreen(), ACCENT_PURPLE.getBlue(), glowAlpha / 3));
        g.setStroke(new BasicStroke(4f));
        g.drawRect(2, 2, w - 4, h - 4);

        // 4. Logo / Client Title
        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        String title = "RaveX";
        int tw = g.getFontMetrics().stringWidth(title);
        
        // Glowing drop shadow for title
        g.setColor(new Color(ACCENT_PINK.getRed(), ACCENT_PINK.getGreen(), ACCENT_PINK.getBlue(), glowAlpha));
        g.drawString(title, cx - tw / 2 - 1, 65 + 1);
        g.drawString(title, cx - tw / 2 + 1, 65 - 1);

        GradientPaint titleGrad = new GradientPaint(
            cx - tw / 2f, 0, ACCENT_PINK,
            cx + tw / 2f, 0, ACCENT_BLUE
        );
        g.setPaint(titleGrad);
        g.drawString(title, cx - tw / 2, 65);

        // Version Badge
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        String badge = version.toUpperCase();
        int bw = g.getFontMetrics().stringWidth(badge);
        int bx = cx + tw / 2 + 10;
        int by = 44;
        
        // Draw badge background
        g.setColor(new Color(0xff, 0x2a, 0x85, 30));
        g.fillRoundRect(bx - 6, by - 12, bw + 12, 18, 6, 6);
        g.setColor(ACCENT_PINK);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(bx - 6, by - 12, bw + 12, 18, 6, 6);
        g.drawString(badge, bx, by);

        // 5. Status text
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(TEXT_COLOR);
        int sw = g.getFontMetrics().stringWidth(status);
        g.drawString(status, cx - sw / 2, 115);

        // Extra info
        if (extraInfo != null && !extraInfo.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.setColor(TEXT_MUTED);
            int ew = g.getFontMetrics().stringWidth(extraInfo);
            g.drawString(extraInfo, cx - ew / 2, 132);
        }

        // 6. Smooth Progress Bar
        int barX = cx - 180, barY = 150, barW = 360, barH = 8;
        
        // Track
        g.setColor(new Color(0x13, 0x0a, 0x22));
        g.fillRoundRect(barX, barY, barW, barH, 8, 8);
        g.setColor(new Color(0x3a, 0x22, 0x5c, 100));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(barX, barY, barW, barH, 8, 8);

        // Fill
        if (animatedPercent > 0.1f) {
            int fillW = (int) (barW * animatedPercent / 100f);
            
            // Draw glow under progress fill
            g.setColor(new Color(ACCENT_PINK.getRed(), ACCENT_PINK.getGreen(), ACCENT_PINK.getBlue(), glowAlpha));
            g.setStroke(new BasicStroke(4f));
            g.drawRoundRect(barX, barY, fillW, barH, 8, 8);

            GradientPaint barGrad = new GradientPaint(
                barX, barY, ACCENT_PINK,
                barX + barW, barY, ACCENT_BLUE
            );
            g.setPaint(barGrad);
            g.fillRoundRect(barX, barY, fillW, barH, 8, 8);
        }

        // 7. Percentage label
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(TEXT_COLOR);
        String pctStr = Math.round(animatedPercent) + "%";
        int pw = g.getFontMetrics().stringWidth(pctStr);
        g.drawString(pctStr, cx - pw / 2, barY + barH + 20);

        // 8. System score rating badge (glowing circle / panel)
        if (systemScore >= 0) {
            int badgeW = 100, badgeH = 26;
            int rx = cx - badgeW / 2;
            int ry = h - 75;

            // Select color based on rating
            Color scoreColor = ACCENT_BLUE;
            if (systemScore >= 80) scoreColor = new Color(0x00, 0xe6, 0x76); // Lime Green
            else if (systemScore >= 50) scoreColor = new Color(0xff, 0xd6, 0x00); // Yellow
            else scoreColor = new Color(0xff, 0x17, 0x44); // Red

            // Box
            g.setColor(new Color(scoreColor.getRed(), scoreColor.getGreen(), scoreColor.getBlue(), 15));
            g.fillRoundRect(rx, ry, badgeW, badgeH, 6, 6);
            g.setColor(new Color(scoreColor.getRed(), scoreColor.getGreen(), scoreColor.getBlue(), 80));
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(rx, ry, badgeW, badgeH, 6, 6);

            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(TEXT_COLOR);
            String scoreText = "RATING: " + systemScore + "/100";
            int sctw = g.getFontMetrics().stringWidth(scoreText);
            g.drawString(scoreText, cx - sctw / 2, ry + 16);
        }

        // 9. System Info Footer
        if (systemInfo != null && !systemInfo.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.setColor(TEXT_MUTED);
            int siw = g.getFontMetrics().stringWidth(systemInfo);
            g.drawString(systemInfo, cx - siw / 2, h - 38);
        }

        // 10. Footer / Error
        if (error) {
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.setColor(new Color(0xff, 0x33, 0x33));
            String err = "FATAL: " + (errorMsg != null ? errorMsg : "Unknown Exception");
            int erw = g.getFontMetrics().stringWidth(err);
            g.drawString(err, cx - erw / 2, h - 18);
        } else {
            g.setFont(new Font("SansSerif", Font.BOLD, 9));
            g.setColor(TEXT_MUTED);
            String footer = "STORMDEVZZ  •  GITHUB.COM/STORMDEVZZ/RAVEX";
            int fw = g.getFontMetrics().stringWidth(footer);
            g.drawString(footer, cx - fw / 2, h - 18);
        }

        // Sync toolkit to avoid any Linux AWT lagging or desync
        Toolkit.getDefaultToolkit().sync();
    }
}
