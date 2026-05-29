package ravex.loader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class LoaderWindow extends JFrame {
    private static final Color BG_COLOR = new Color(0x0e, 0x0a, 0x1a);
    private static final Color ACCENT_LIGHT = new Color(0xda, 0x70, 0xd6);
    private static final Color ACCENT_DARK = new Color(0x8a, 0x2b, 0xe2);
    private static final Color TEXT_COLOR = new Color(0xee, 0xee, 0xee);
    private static final Color TEXT_MUTED = new Color(0x88, 0x88, 0xaa);

    private String version = "1.0";
    private String status = "Initializing...";
    private int percent = 0;
    private boolean error = false;
    private String errorMsg = "";
    private int systemScore = -1;
    private String systemInfo = "";
    private String extraInfo = "";

    public LoaderWindow() {
        setTitle("RaveX Premium Loader");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 260);
        setResizable(false);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setBackground(BG_COLOR);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFrame((Graphics2D) g);
            }
        };
        panel.setBackground(BG_COLOR);

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
    }

    public void setVersion(String v) { 
        this.version = v; 
        repaint();
    }

    public void updateStatus(String text, int pct) {
        SwingUtilities.invokeLater(() -> {
            status = text;
            percent = Math.min(pct, 100);
            repaint();
        });
    }

    public void setError(String msg) {
        SwingUtilities.invokeLater(() -> {
            error = true;
            errorMsg = msg;
            status = "Initialization Error";
            repaint();
        });
    }

    public void setSystemInfo(String info) { 
        this.systemInfo = info; 
        repaint();
    }
    
    public void setExtraInfo(String info) { 
        this.extraInfo = info; 
        repaint();
    }
    
    public void setSystemScore(int score) { 
        this.systemScore = score; 
        repaint();
    }

    private void drawFrame(Graphics2D g) {
        int w = getWidth(), h = getHeight(), cx = w / 2;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Solid Elegant Border
        g.setStroke(new BasicStroke(2f));
        g.setColor(ACCENT_DARK);
        g.drawRect(1, 1, w - 2, h - 2);

        // 2. Client Title
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        String title = "RaveX Client" + (version != null ? " " + version : "");
        int tw = g.getFontMetrics().stringWidth(title);
        GradientPaint titleGrad = new GradientPaint(cx - tw / 2f, 0, ACCENT_LIGHT, cx + tw / 2f, 0, ACCENT_DARK);
        g.setPaint(titleGrad);
        g.drawString(title, cx - tw / 2, 60);

        // 3. Status Text
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(TEXT_COLOR);
        int sw = g.getFontMetrics().stringWidth(status);
        g.drawString(status, cx - sw / 2, 105);

        // 4. Extra Info
        if (!extraInfo.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.setColor(TEXT_MUTED);
            int ew = g.getFontMetrics().stringWidth(extraInfo);
            g.drawString(extraInfo, cx - ew / 2, 122);
        }

        // 5. Progress Bar Track
        int barX = cx - 160, barY = 138, barW = 320, barH = 6;
        g.setColor(new Color(0x1a, 0x12, 0x2c));
        g.fillRect(barX, barY, barW, barH);
        g.setColor(new Color(0x35, 0x22, 0x5a));
        g.drawRect(barX, barY, barW, barH);

        // Progress Bar Fill
        if (percent > 0) {
            GradientPaint barGrad = new GradientPaint(barX, barY, ACCENT_LIGHT, barX + barW, barY, ACCENT_DARK);
            g.setPaint(barGrad);
            int fillW = barW * percent / 100;
            g.fillRect(barX, barY, fillW, barH);
        }

        // 6. Percentage
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(TEXT_COLOR);
        String pctStr = percent + "%";
        int pw = g.getFontMetrics().stringWidth(pctStr);
        g.drawString(pctStr, cx - pw / 2, barY + barH + 16);

        // 7. System Rating Badge / Info
        if (systemScore >= 0 || !systemInfo.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.setColor(TEXT_MUTED);
            String info = (systemInfo.isEmpty() ? "" : systemInfo + " | ") + 
                          (systemScore >= 0 ? "Rating: " + systemScore + "/100" : "");
            int iw = g.getFontMetrics().stringWidth(info);
            g.drawString(info, cx - iw / 2, h - 45);
        }

        // 8. Error banner
        if (error) {
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.setColor(new Color(0xff, 0x4a, 0x4a));
            String err = "ERROR: " + (errorMsg != null ? errorMsg : "Unknown Issue");
            int erw = g.getFontMetrics().stringWidth(err);
            g.drawString(err, cx - erw / 2, h - 25);
        } else {
            // Footer
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(ACCENT_LIGHT);
            String footer = "GITHUB.COM/STORMDEVZZ/RAVEX";
            int fw = g.getFontMetrics().stringWidth(footer);
            g.drawString(footer, cx - fw / 2, h - 20);
        }

        // Sync toolkit to avoid any Linux AWT lagging or desync
        Toolkit.getDefaultToolkit().sync();
    }
}
