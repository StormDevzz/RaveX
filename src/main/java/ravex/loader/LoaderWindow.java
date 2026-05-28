package ravex.loader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class LoaderWindow extends JFrame {
    private static final Color BG = new Color(0x1a, 0x1a, 0x2e);
    private static final Color ACCENT = new Color(0xbb, 0x86, 0xfc);
    private static final Color BAR_BG = new Color(0x33, 0x33, 0x33);
    private static final Color TEXT = new Color(0xaa, 0xaa, 0xaa);
    private static final Color DIM = new Color(0x66, 0x66, 0x66);

    private final List<float[]> particles = new ArrayList<>();
    private String version;
    private String status = "Initializing...";
    private int percent;
    private boolean error;
    private String errorMsg;
    private int systemScore = -1;
    private String systemInfo = "";
    private String extraInfo = "";
    private float animTime;

    public LoaderWindow() {
        setTitle("RaveX Loader");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(540, 350);
        setResizable(false);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setBackground(BG);

        for (int i = 0; i < 12; i++) {
            particles.add(new float[]{
                (i * 137f + 40f * i) % 540,
                (i * 97f + 25f * i) % 350,
                i * 0.7f
            });
        }

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFrame((Graphics2D) g);
            }
        };
        panel.setBackground(BG);
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int cx = getWidth() / 2;
                int fy = getHeight() - 25;
                if (e.getY() >= fy - 12 && e.getY() <= fy + 12
                        && e.getX() >= cx - 140 && e.getX() <= cx + 140) {
                    openRepo();
                }
            }
        });
        setContentPane(panel);

        // Animation timer
        new Timer(50, e -> repaint()).start();
    }

    private void openRepo() {
        try { Desktop.getDesktop().browse(new URI("https://github.com/StormDevzz/RaveX")); }
        catch (Exception ignored) {}
    }

    public void setVersion(String v) { this.version = v; }

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
            status = "Error";
        });
    }

    public void setSystemInfo(String info) { this.systemInfo = info; }
    public void setExtraInfo(String info) { this.extraInfo = info; }
    public void setSystemScore(int score) { this.systemScore = score; }

    private void drawFrame(Graphics2D g) {
        animTime += 0.05f;
        float t = animTime;
        int w = getWidth(), h = getHeight(), cx = w / 2;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(BG);
        g.fillRect(0, 0, w, h);

        // Particles
        g.setColor(new Color(0x9b, 0x59, 0xb6, 80));
        for (float[] p : particles) {
            float px = (p[0] + t * 40) % w;
            float py = (p[1] + t * 25) % h;
            float alpha = 0.3f + 0.3f * (float) Math.sin(t * 2 + p[2]);
            g.setColor(new Color(0x9b, 0x59, 0xb6, (int) (alpha * 255)));
            g.fillRect((int) px, (int) py, 2, 2);
        }

        // System info top-left
        if (!systemInfo.isEmpty()) {
            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(DIM);
            g.drawString(systemInfo, 12, 16);
        }

        // Title
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.setColor(ACCENT);
        String title = "RaveX" + (version != null ? " " + version : "");
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, cx - tw / 2, 65);

        // Status
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(TEXT);
        int sw = g.getFontMetrics().stringWidth(status);
        g.drawString(status, cx - sw / 2, 108);

        // Extra info
        if (!extraInfo.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(DIM);
            int ew = g.getFontMetrics().stringWidth(extraInfo);
            g.drawString(extraInfo, cx - ew / 2, 125);
        }

        // Progress bar
        int barX = cx - 150, barY = 140, barW = 300, barH = 6;
        g.setColor(BAR_BG);
        g.fillRect(barX, barY, barW, barH);

        if (percent > 0) {
            g.setColor(error ? new Color(0xE7, 0x4C, 0x3C) : ACCENT);
            int fillW = barW * percent / 100;
            g.fillRect(barX, barY, fillW, barH);
        }

        // Percentage
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(TEXT);
        String pct = percent + "%";
        int pw = g.getFontMetrics().stringWidth(pct);
        g.drawString(pct, cx - pw / 2, barY + barH + 16);

        // Loading dots
        int dots = (int) ((t * 3) % 4);
        String ld = "Loading" + ".".repeat(dots) + "   ".substring(0, 3 - dots);
        int ldw = g.getFontMetrics().stringWidth(ld);
        g.drawString(ld, cx - ldw / 2, barY + barH + 36);

        // Spinner
        g.setColor(ACCENT);
        int spinCy = barY + barH + 60;
        for (int i = 0; i < 8; i++) {
            double a = t * 2 + i * Math.PI / 4;
            int sx = cx + (int) (12 * Math.cos(a));
            int sy = spinCy + (int) (12 * Math.sin(a));
            g.fillRect(sx - (i < 4 ? 2 : 1), sy - (i < 4 ? 2 : 1), i < 4 ? 4 : 2, i < 4 ? 4 : 2);
        }

        // Score
        if (systemScore >= 0) {
            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(DIM);
            String sc = "System score: " + systemScore + "/100";
            int scw = g.getFontMetrics().stringWidth(sc);
            g.drawString(sc, cx - scw / 2, spinCy + 30);
        }

        // Error
        if (error) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(0xFF, 0x44, 0x44));
            String err = "Error: " + (errorMsg != null ? errorMsg : "");
            int erw = g.getFontMetrics().stringWidth(err);
            g.drawString(err, cx - erw / 2, spinCy + 55);
        }

        // Footer / repo
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(ACCENT);
        String footer = "github.com/StormDevzz/RaveX (click)";
        int fw = g.getFontMetrics().stringWidth(footer);
        g.drawString(footer, cx - fw / 2, h - 20);
    }
}
