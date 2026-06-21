package ravex.launcher;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.concurrent.*;

public class RaveXLauncher {
    private static JFrame frame;
    private static JProgressBar progressBar;
    private static JLabel statusLabel;
    private static JButton launchButton;
    private static JButton browseButton;
    private static JTextField jarPathField;
    private static JTextArea logArea;
    private static Process minecraftProcess;
    
    public static void main(String[] args) {
        // Fix Linux flickering
        System.setProperty("sun.awt.noerasebackground", "true");
        
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }
    
    private static void createAndShowGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        frame = new JFrame("RaveX Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        
        // Main panel with gradient background
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(45, 45, 55);
                Color color2 = new Color(30, 30, 40);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setOpaque(false);
        JLabel titleLabel = new JLabel("RaveX Launcher");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(100, 180, 255));
        titlePanel.add(titleLabel);
        
        // JAR selection panel
        JPanel jarPanel = new JPanel(new BorderLayout(5, 5));
        jarPanel.setOpaque(false);
        
        JLabel jarLabel = new JLabel("RaveX JAR File:");
        jarLabel.setForeground(Color.WHITE);
        jarLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        jarPathField = new JTextField();
        jarPathField.setText(getDefaultJarPath());
        jarPathField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        jarPathField.setBackground(new Color(60, 60, 70));
        jarPathField.setForeground(Color.WHITE);
        jarPathField.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 90)));
        
        browseButton = new JButton("Browse...");
        browseButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        browseButton.setBackground(new Color(70, 130, 180));
        browseButton.setForeground(Color.WHITE);
        browseButton.setFocusPainted(false);
        browseButton.addActionListener(e -> browseJarFile());
        
        jarPanel.add(jarLabel, BorderLayout.NORTH);
        jarPanel.add(jarPathField, BorderLayout.CENTER);
        jarPanel.add(browseButton, BorderLayout.EAST);
        
        // Launch button
        launchButton = new JButton("Launch RaveX");
        launchButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        launchButton.setBackground(new Color(60, 160, 100));
        launchButton.setForeground(Color.WHITE);
        launchButton.setFocusPainted(false);
        launchButton.setPreferredSize(new Dimension(200, 40));
        launchButton.addActionListener(e -> launchRaveX());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        buttonPanel.add(launchButton);
        
        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(60, 160, 100));
        progressBar.setBackground(new Color(60, 60, 70));
        progressBar.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 90)));
        
        // Status label
        statusLabel = new JLabel("Ready to launch");
        statusLabel.setForeground(new Color(150, 150, 160));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Log area
        logArea = new JTextArea(8, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBackground(new Color(25, 25, 35));
        logArea.setForeground(new Color(180, 180, 190));
        logArea.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 90)));
        
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setOpaque(false);
        logScrollPane.getViewport().setOpaque(false);
        logScrollPane.setBorder(null);
        
        // System info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(false);
        JLabel osLabel = new JLabel("OS: " + getOSInfo());
        osLabel.setForeground(new Color(120, 120, 130));
        osLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        infoPanel.add(osLabel, BorderLayout.WEST);
        
        // Layout
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(jarPanel, BorderLayout.NORTH);
        centerPanel.add(buttonPanel, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setOpaque(false);
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        bottomPanel.add(infoPanel, BorderLayout.SOUTH);
        
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        mainPanel.add(logScrollPane, BorderLayout.SOUTH);
        
        frame.add(mainPanel);
        frame.setVisible(true);
    }
    
    private static String getOSInfo() {
        String os = System.getProperty("os.name");
        String version = System.getProperty("os.version");
        String arch = System.getProperty("os.arch");
        return os + " " + version + " (" + arch + ")";
    }
    
    private static String getDefaultJarPath() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();
        
        // Try to find RaveX jar in common locations
        String[] searchPaths = {
            userHome + "/Downloads/RaveX-main3/RaveX-main/build/libs/",
            userHome + "\\Downloads\\RaveX-main3\\RaveX-main\\build\\libs\\",
            userHome + "/.minecraft/mods/",
            userHome + "\\AppData\\Roaming\\.minecraft\\mods\\"
        };
        
        for (String path : searchPaths) {
            File dir = new File(path);
            if (dir.exists()) {
                File[] jars = dir.listFiles((d, name) -> name.endsWith(".jar") && !name.contains("sources"));
                if (jars != null && jars.length > 0) {
                    return jars[0].getAbsolutePath();
                }
            }
        }
        
        return "";
    }
    
    private static void browseJarFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar");
            }
            
            @Override
            public String getDescription() {
                return "JAR Files (*.jar)";
            }
        });
        
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            jarPathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private static void launchRaveX() {
        String jarPath = jarPathField.getText().trim();
        
        if (jarPath.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please select a RaveX JAR file", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            JOptionPane.showMessageDialog(frame, "JAR file not found: " + jarPath, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        launchButton.setEnabled(false);
        browseButton.setEnabled(false);
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                updateProgress(0, "Initializing...");
                appendLog("Starting RaveX launcher...");
                appendLog("JAR: " + jarPath);
                appendLog("OS: " + getOSInfo());
                
                updateProgress(10, "Checking Java version...");
                String javaVersion = System.getProperty("java.version");
                appendLog("Java version: " + javaVersion);
                
                updateProgress(20, "Preparing launch command...");
                
                // Build launch command
                java.util.List<String> command = new ArrayList<>();
                command.add(getJavaExecutable());
                command.add("-jar");
                command.add(jarPath);
                
                // Add JVM arguments for better performance
                command.add("-Xmx2G");
                command.add("-Xms512M");
                command.add("-XX:+UseG1GC");
                command.add("-XX:MaxGCPauseMillis=20");
                command.add("-XX:InitiatingHeapOccupancyPercent=45");
                
                appendLog("Command: " + String.join(" ", command));
                
                updateProgress(40, "Launching RaveX...");
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                
                minecraftProcess = pb.start();
                
                updateProgress(50, "Reading process output...");
                
                // Read process output
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(minecraftProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        appendLog(line);
                    }
                }
                
                updateProgress(80, "Waiting for process...");
                
                int exitCode = minecraftProcess.waitFor();
                appendLog("Process exited with code: " + exitCode);
                
                if (exitCode == 0) {
                    updateProgress(100, "RaveX closed successfully");
                    appendLog("RaveX closed successfully");
                } else {
                    updateProgress(100, "RaveX closed with error");
                    appendLog("RaveX closed with error code: " + exitCode);
                }
                
            } catch (Exception e) {
                appendLog("Error: " + e.getMessage());
                e.printStackTrace();
                updateProgress(100, "Launch failed: " + e.getMessage());
                JOptionPane.showMessageDialog(frame, "Failed to launch RaveX: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    launchButton.setEnabled(true);
                    browseButton.setEnabled(true);
                });
                executor.shutdown();
            }
        });
    }
    
    private static String getJavaExecutable() {
        String javaHome = System.getProperty("java.home");
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            return javaHome + "\\bin\\java.exe";
        } else {
            return javaHome + "/bin/java";
        }
    }
    
    private static void updateProgress(int value, String status) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(value);
            statusLabel.setText(status);
        });
    }
    
    private static void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
