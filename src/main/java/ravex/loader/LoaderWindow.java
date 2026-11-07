package ravex.loader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LoaderWindow extends JFrame implements LoaderCallback {

    private static final Color ACCENT_BLUE = new Color(0x40, 0xA9, 0xF8);
    private static final Color TEXT_MUTED = new Color(0xFF, 0xFF, 0xFF);
    private static final Color BG_TINT = new Color(0x0A, 0x0C, 0x14, 0xC0);

    private JPanel panel;
    private Timer animTimer;
    private float animPhase = 0.0f;
    private Image logoImage = null;
    private Image osIcon = null;
    private String osName = getWindowsDisplayName();

    private static String getWindowsDisplayName() {
        String osNameProp = System.getProperty("os.name", "").toLowerCase();
        if (!osNameProp.contains("win")) {
            return System.getProperty("os.name", "Unknown OS");
        }
        String productName = null;
        String displayVersion = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-Command",
                    "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; " +
                    "$os = Get-CimInstance -ClassName Win32_OperatingSystem; " +
                    "Write-Output $os.Caption; " +
                    "$v = (Get-ItemProperty 'HKLM:\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion'); " +
                    "Write-Output $v.DisplayVersion");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            String line;
            List<String> lines = new ArrayList<>();
            while ((line = r.readLine()) != null) {
                lines.add(line.trim());
            }
            p.waitFor();
            if (lines.size() >= 1 && !lines.get(0).isEmpty()) {
                productName = lines.get(0);
            }
            if (lines.size() >= 2 && !lines.get(1).isEmpty()) {
                displayVersion = lines.get(1);
            }
        } catch (Exception ignored) {}
        // Fallback to reg query if PowerShell failed
        if (productName == null) {
            try {
                Process p = new ProcessBuilder("reg", "query",
                        "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
                        "/v", "ProductName").start();
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), java.nio.charset.Charset.defaultCharset()));
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("ProductName")) {
                        String[] parts = line.trim().split("\\s{2,}");
                        if (parts.length >= 3) {
                            productName = parts[parts.length - 1].trim();
                        }
                    }
                }
                p.waitFor();
            } catch (Exception ignored) {}
        }
        if (displayVersion == null) {
            try {
                Process p = new ProcessBuilder("reg", "query",
                        "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
                        "/v", "DisplayVersion").start();
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), java.nio.charset.Charset.defaultCharset()));
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("DisplayVersion")) {
                        String[] parts = line.trim().split("\\s{2,}");
                        if (parts.length >= 3) {
                            displayVersion = parts[parts.length - 1].trim();
                        }
                    }
                }
                p.waitFor();
            } catch (Exception ignored) {}
        }
        // Fallback to ReleaseId for older Windows 10
        if (displayVersion == null) {
            try {
                Process p = new ProcessBuilder("reg", "query",
                        "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
                        "/v", "ReleaseId").start();
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), java.nio.charset.Charset.defaultCharset()));
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("ReleaseId")) {
                        String[] parts = line.trim().split("\\s{2,}");
                        if (parts.length >= 3) {
                            displayVersion = parts[parts.length - 1].trim();
                        }
                    }
                }
                p.waitFor();
            } catch (Exception ignored) {}
        }
        if (productName != null && displayVersion != null && !displayVersion.isEmpty()) {
            return productName + " " + displayVersion;
        } else if (productName != null) {
            return productName;
        }
        return System.getProperty("os.name", "Windows") + " " + System.getProperty("os.version", "");
    }
    private String systemInfo = "";
    private String extraInfo = "";
    private String statusText = "Initializing...";
    private int systemScore = 0;
    private int progress = 0;
    private boolean closeHovered = false;

    public LoaderWindow() {
        setSize(480, 180);
        setLocationRelativeTo(null);
        setUndecorated(true);
        getRootPane().setOpaque(false);
        getLayeredPane().setOpaque(false);

        try {
            java.io.InputStream is = LoaderWindow.class.getResourceAsStream("/assets/ravex/textures/ravexv2.png");
            if (is != null) {
                logoImage = javax.imageio.ImageIO.read(is);
                setIconImage(logoImage);
            }
        } catch (Exception ignored) {}

        try {
            String iconRes = null;
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                iconRes = os.contains("11") ? "/ravex/loader/icons/windows11.png"
                    : "/ravex/loader/icons/windows10.png";
            } else if (os.contains("linux")) {
                iconRes = "/ravex/loader/icons/linux.png";
            }
            if (iconRes != null) {
                java.io.InputStream is2 = LoaderWindow.class.getResourceAsStream(iconRes);
                if (is2 != null) {
                    osIcon = javax.imageio.ImageIO.read(is2);
                }
            }
        } catch (Exception ignored) {}

        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                drawFrame((Graphics2D) g);
            }
        };
        panel.setOpaque(false);
        panel.setBackground(new Color(0, 0, 0, 0));
        panel.setDoubleBuffered(true);
        panel.setPreferredSize(new Dimension(480, 180));

        setContentPane(panel);

        final Point[] dragStart = new Point[1];

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getX() >= getWidth() - 28 && e.getY() <= 28) {
                    if (ProcessLauncher.launchedProcess != null) {
                        try { ProcessLauncher.launchedProcess.destroyForcibly(); } catch (Exception ignored) {}
                        ProcessLauncher.launchedProcess = null;
                    }
                    System.exit(0);
                } else {
                    dragStart[0] = new Point(e.getXOnScreen() - getX(), e.getYOnScreen() - getY());
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                dragStart[0] = null;
                setCursor(Cursor.getDefaultCursor());
            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart[0] != null) {
                    setLocation(e.getXOnScreen() - dragStart[0].x, e.getYOnScreen() - dragStart[0].y);
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    repaint();
                }
            }
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean hovered = (e.getX() >= getWidth() - 28 && e.getY() <= 28);
                if (hovered != closeHovered) {
                    closeHovered = hovered;
                    repaint();
                }
                if (hovered) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                applyBlurDelayed();
            }
            @Override
            public void componentResized(ComponentEvent e) {
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (ProcessLauncher.launchedProcess != null) {
                    try { ProcessLauncher.launchedProcess.destroyForcibly(); } catch (Exception ignored) {}
                    ProcessLauncher.launchedProcess = null;
                }
                System.exit(0);
            }
        });

        animTimer = new Timer(16, e -> {
            animPhase += 0.008f;
            if (animPhase >= 1.0f) animPhase -= 1.0f;
            repaint();
        });
        animTimer.start();
    }

    private void applyBlurDelayed() {
        javax.swing.Timer t = new javax.swing.Timer(300, ev -> {
            try {
                AcrylicBlur.applyGlass(LoaderWindow.this);
                System.err.println("[LoaderWindow] AcrylicBlur.applyGlass invoked");
            } catch (Throwable th) {
                System.err.println("[LoaderWindow] AcrylicBlur failed: " + th);
            }
            ((javax.swing.Timer) ev.getSource()).stop();
        });
        t.setRepeats(false);
        t.start();
    }

    public void updateStatus(String text, int progress) {
        this.statusText = text;
        this.progress = progress;
        SwingUtilities.invokeLater(this::repaint);
    }

    public void setSystemScore(int score) {
        this.systemScore = score;
        SwingUtilities.invokeLater(this::repaint);
    }

    public void setError(String msg) {
        this.extraInfo = msg;
        SwingUtilities.invokeLater(this::repaint);
    }

    public void setSystemInfo(String info) {
        this.systemInfo = info;
        SwingUtilities.invokeLater(this::repaint);
    }

    public void setExtraInfo(String info) {
        this.extraInfo = info;
        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void paint(Graphics g) {
        paintComponents(g);
    }

    @Override
    public void dispose() {
        if (animTimer != null) animTimer.stop();
        super.dispose();
    }

    private void drawFrame(Graphics2D g) {
        int w = getWidth(), h = getHeight();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g.setColor(BG_TINT);
        g.fillRect(0, 0, w, h);

        g.setColor(ACCENT_BLUE);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

        if (logoImage != null) {
            g.drawImage(logoImage, 25, 25, 100, 100, null);
        } else {
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            g.setColor(Color.WHITE);
            g.drawString("Rave", 25, 75);
            g.setColor(ACCENT_BLUE);
            Object oldAA2 = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g.drawString("X", 25 + g.getFontMetrics().stringWidth("Rave"), 75);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAA2);
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        int w1 = g.getFontMetrics().stringWidth("Rave");
        g.setColor(Color.WHITE);
        g.drawString("Rave", 150, 58);
        g.setColor(ACCENT_BLUE);
        Object oldAA = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.drawString("X", 150 + w1, 58);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAA);

        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        String sub = "LOADER  \u2022  " + osName;
        g.setColor(Color.WHITE);
        if (osIcon != null) {
            g.drawImage(osIcon, 120, 55, 32, 32, null);
        }
        int maxSubWidth = w - 160 - 32; // leave room for close button
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(sub) > maxSubWidth) {
            while (sub.length() > 3 && fm.stringWidth(sub + "...") > maxSubWidth) {
                sub = sub.substring(0, sub.length() - 1);
            }
            sub = sub + "...";
        }
        g.drawString(sub, 160, 78);

        if (!systemInfo.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.setColor(TEXT_MUTED);
            String info = systemInfo;
            int maxInfoWidth = w - 160;
            FontMetrics fmInfo = g.getFontMetrics();
            if (fmInfo.stringWidth(info) > maxInfoWidth) {
                while (info.length() > 3 && fmInfo.stringWidth(info + "...") > maxInfoWidth) {
                    info = info.substring(0, info.length() - 1);
                }
                info = info + "...";
            }
            g.drawString(info, 150, 98);
        }

        if (!extraInfo.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 9));
            g.setColor(TEXT_MUTED);
            String info = extraInfo;
            int maxInfoWidth = w - 160;
            FontMetrics fmInfo = g.getFontMetrics();
            if (fmInfo.stringWidth(info) > maxInfoWidth) {
                while (info.length() > 3 && fmInfo.stringWidth(info + "...") > maxInfoWidth) {
                    info = info.substring(0, info.length() - 1);
                }
                info = info + "...";
            }
            g.drawString(info, 150, 115);
        }

        if (statusText != null && !statusText.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(Color.WHITE);
            String st = statusText;
            int maxStWidth = w - 160;
            FontMetrics fmSt = g.getFontMetrics();
            if (fmSt.stringWidth(st) > maxStWidth) {
                while (st.length() > 3 && fmSt.stringWidth(st + "...") > maxStWidth) {
                    st = st.substring(0, st.length() - 1);
                }
                st = st + "...";
            }
            g.drawString(st, 150, 125);
        }

        if (closeHovered) {
            g.setColor(new Color(0xFF, 0x40, 0x40, 0x80));
            g.fillRoundRect(w - 28, 0, 28, 28, 6, 6);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(closeHovered ? Color.WHITE : TEXT_MUTED);
        g.drawString("\u2715", w - 20, 18);

        int barX = 150, barY = 135, barW = w - 170, barH = 4;
        g.setColor(new Color(0x30, 0x30, 0x38));
        g.fillRoundRect(barX, barY, barW, barH, 2, 2);
        g.setColor(ACCENT_BLUE);
        int progress = (int) (systemScore / 100.0f * barW);
        g.fillRoundRect(barX, barY, Math.max(2, progress), barH, 2, 2);

        int cx = 75, cy = 140;
        int radius = 14;
        int dotSize = 4;
        float[] delays = {0.0f, 0.05f, 0.10f, 0.15f, 0.20f};
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i < 5; i++) {
            float localT = (animPhase + delays[i]) % 1.0f;
            if (localT < 0) localT += 1.0f;

            float rot = 225f;  // degrees
            float opacity = 1f;
            if (localT < 0.07f) {
                float u = localT / 0.07f;
                rot = 225f + (345f - 225f) * u;
                opacity = 1f;
            } else if (localT < 0.30f) {
                float u = (localT - 0.07f) / (0.30f - 0.07f);
                rot = 345f + (455f - 345f) * u;
                opacity = 1f;
            } else if (localT < 0.39f) {
                float u = (localT - 0.30f) / (0.39f - 0.30f);
                rot = 455f + (690f - 455f) * u;
                opacity = 1f;
            } else if (localT < 0.70f) {
                float u = (localT - 0.39f) / (0.70f - 0.39f);
                rot = 690f + (815f - 690f) * u;
                opacity = 1f;
            } else if (localT < 0.75f) {
                float u = (localT - 0.70f) / (0.75f - 0.70f);
                rot = 815f + (945f - 815f) * u;
                opacity = 1f - u;
            } else {
                rot = 945f;
                opacity = 0f;
            }

            if (opacity <= 0f) continue;

            double rad = Math.toRadians(rot);
            int dx = cx + (int) (Math.cos(rad) * radius);
            int dy = cy + (int) (Math.sin(rad) * radius);

            int alpha = (int) (opacity * 255);
            g.setColor(new Color(255, 255, 255, alpha));
            g.fillOval(dx - dotSize / 2, dy - dotSize / 2, dotSize, dotSize);
        }
    }
}
