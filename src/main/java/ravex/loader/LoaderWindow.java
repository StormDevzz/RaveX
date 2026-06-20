package ravex.loader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class LoaderWindow extends JFrame {
    private static final Color BG_DARK = new Color(0x11, 0x11, 0x15);
    private static final Color ACCENT_BLUE = new Color(0x40, 0xA9, 0xF8);
    private static final Color TEXT_COLOR = new Color(0xe2, 0xe2, 0xe8);
    private static final Color TEXT_MUTED = new Color(0x65, 0x65, 0x75);

    private String version = "1.2 NextGen";
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
            // Ignore icon load failures
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

        // 1. Solid background
        g.setColor(BG_DARK);
        g.fillRect(0, 0, w, h);

        // 2. 1px flat border
        g.setColor(ACCENT_BLUE);
        g.setStroke(new BasicStroke(1.0f));
        g.drawRect(0, 0, w - 1, h - 1);

        // 3. Logo Image
        if (logoImage != null) {
            g.drawImage(logoImage, 25, 25, 100, 100, null);
        } else {
            // Fallback text logo if resource is missing
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            g.setColor(Color.WHITE);
            g.drawString("Rave", 25, 75);
            g.setColor(ACCENT_BLUE);
            g.drawString("X", 25 + g.getFontMetrics().stringWidth("Rave"), 75);
        }

        // 4. Logo Text (RaveX) beside the icon
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        int w1 = g.getFontMetrics().stringWidth("Rave");
        g.setColor(Color.WHITE);
        g.drawString("Rave", 150, 58);
        g.setColor(ACCENT_BLUE);
        g.drawString("X", 150 + w1, 58);

        // 5. Subtitle
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        String sub = "LOADER v" + version.toUpperCase();
        g.setColor(TEXT_MUTED);
        g.drawString(sub, 150, 78);

        // 6. Status
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        if (error) {
            g.setColor(new Color(0xff, 0x4f, 0x4f));
            String displayStatus = "FATAL: " + (errorMsg != null ? errorMsg : "Unknown Exception");
            g.drawString(displayStatus, 150, 115);
        } else {
            g.setColor(TEXT_COLOR);
            g.drawString(status, 150, 115);
        }

        // 7. Extra Info / Hardware details
        if (!error && extraInfo != null && !extraInfo.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.setColor(TEXT_MUTED);
            g.drawString(extraInfo, 150, 135);
        }

        // 8. Thin progress bar at the very bottom
        int barH = 4;
        g.setColor(new Color(0x1F, 0x1F, 0x24));
        g.fillRect(0, h - barH, w, barH);
        
        if (animatedPercent > 0.1f) {
            int fillW = (int) (w * animatedPercent / 100f);
            g.setColor(ACCENT_BLUE);
            g.fillRect(0, h - barH, fillW, barH);
        }

        // 9. Close button
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
