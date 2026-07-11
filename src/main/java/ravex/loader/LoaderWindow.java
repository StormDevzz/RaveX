package ravex.loader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.nio.file.*;
import java.util.List;

public class LoaderWindow extends JFrame implements LoaderCallback {
    private static final Color BG_DARK = new Color(0x11, 0x11, 0x15);
    private static final Color ACCENT_BLUE = new Color(0x40, 0xA9, 0xF8);
    private static final Color TEXT_COLOR = new Color(0xe2, 0xe2, 0xe8);
    private static final Color TEXT_MUTED = new Color(0x65, 0x65, 0x75);

    private String version = "1.4.7";
    private final String osName = detectOS();
    private String status = "Initializing...";
    private int percent = 0;
    private float animatedPercent = 0f;
    private boolean error = false;
    private String errorMsg = "";
    private int systemScore = -1;
    private String systemInfo = "";
    private String extraInfo = "";

    private Timer animTimer;
    private boolean closeHovered = false;
    private Image logoImage = null;

    private static String detectOS() {
        String os = System.getProperty("os.name", "Unknown").toLowerCase();
        if (os.contains("win")) {
            return detectWindows();
        }
        if (os.contains("linux")) {
            try {
                Path osRelease = Paths.get("/etc/os-release");
                if (Files.exists(osRelease)) {
                    List<String> lines = Files.readAllLines(osRelease);
                    for (String line : lines) {
                        if (line.startsWith("PRETTY_NAME=")) {
                            String val = line.substring(12);
                            if (val.startsWith("\"") && val.endsWith("\""))
                                val = val.substring(1, val.length() - 1);
                            return val;
                        }
                    }
                }
            } catch (Exception ignored) {}
            return "Linux";
        }
        if (os.contains("mac")) return "macOS";
        if (os.contains("freebsd")) return "FreeBSD";
        return System.getProperty("os.name", "Unknown");
    }

    private static String detectWindows() {
        if (System.getProperty("os.name").contains("11"))
            return "Windows 11";
        try {
            String[] parts = System.getProperty("os.version", "0").split("\\.");
            int build = Integer.parseInt(parts[parts.length - 1]);
            if (build >= 22000) return "Windows 11";
        } catch (Exception ignored) {}
        try {
            Process p = Runtime.getRuntime().exec(
                "reg query \"HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\" /v CurrentBuild"
            );
            java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream(), "CP866")
            );
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.endsWith("CurrentBuild") || line.matches("\\d+")) {
                    String[] tokens = line.split("\\s+");
                    String buildStr = tokens[tokens.length - 1].trim();
                    int build = Integer.parseInt(buildStr);
                    if (build >= 22000) return "Windows 11";
                    break;
                }
            }
            br.close();
        } catch (Exception ignored) {}
        return System.getProperty("os.name", "Windows");
    }

    public LoaderWindow() {
        setTitle("RaveX Loader");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(480, 180);
        setResizable(false);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setBackground(BG_DARK);

        try {
            java.io.InputStream is = LoaderWindow.class.getResourceAsStream("/assets/ravex/textures/ravexclean.png");
            if (is != null) {
                logoImage = javax.imageio.ImageIO.read(is);
                setIconImage(logoImage);
            }
        } catch (Exception e) {

        }

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFrame((Graphics2D) g);
            }
        };
        panel.setBackground(BG_DARK);
        panel.setDoubleBuffered(true);

        final Point[] dragStart = new Point[1];
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getX() >= getWidth() - 28 && e.getY() <= 28) {
                    System.exit(0);
                } else {
                    dragStart[0] = e.getPoint();
                }
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

        setContentPane(panel);

        animTimer = new Timer(10, e -> {
            float diff = percent - animatedPercent;
            if (Math.abs(diff) > 0.01f) {
                animatedPercent += diff * 0.04f;
            } else {
                animatedPercent = percent;
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
        int w = getWidth(), h = getHeight();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);


        g.setColor(BG_DARK);
        g.fillRect(0, 0, w, h);


        g.setColor(ACCENT_BLUE);
        g.setStroke(new BasicStroke(1.0f));
        g.drawRect(0, 0, w - 1, h - 1);


        if (logoImage != null) {
            g.drawImage(logoImage, 25, 25, 100, 100, null);
        } else {

            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            g.setColor(Color.WHITE);
            g.drawString("Rave", 25, 75);
            g.setColor(ACCENT_BLUE);
            g.drawString("X", 25 + g.getFontMetrics().stringWidth("Rave"), 75);
        }


        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        int w1 = g.getFontMetrics().stringWidth("Rave");
        g.setColor(Color.WHITE);
        g.drawString("Rave", 150, 58);
        g.setColor(ACCENT_BLUE);
        g.drawString("X", 150 + w1, 58);


        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        String sub = "LOADER v" + version.toUpperCase() + "  \u2022  " + osName;
        g.setColor(TEXT_MUTED);
        g.drawString(sub, 150, 78);


        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        if (error) {
            g.setColor(new Color(0xff, 0x4f, 0x4f));
            String displayStatus = "FATAL: " + (errorMsg != null ? errorMsg : "Unknown Exception");
            g.drawString(displayStatus, 150, 115);
        } else {
            g.setColor(TEXT_COLOR);
            g.drawString(status, 150, 115);
        }


        if (!error && extraInfo != null && !extraInfo.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.setColor(TEXT_MUTED);
            g.drawString(extraInfo, 150, 135);
        }


        int barH = 4;
        g.setColor(new Color(0x1F, 0x1F, 0x24));
        g.fillRect(0, h - barH, w, barH);

        if (animatedPercent > 0.1f) {
            int fillW = (int) (w * animatedPercent / 100f);
            g.setColor(ACCENT_BLUE);
            g.fillRect(0, h - barH, fillW, barH);
        }


        if (closeHovered) {
            g.setColor(new Color(0xff, 0x4f, 0x4f));
        } else {
            g.setColor(TEXT_MUTED);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("✕", w - 18, 18);

        Toolkit.getDefaultToolkit().sync();
    }
}
