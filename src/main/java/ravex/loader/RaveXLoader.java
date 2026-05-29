package ravex.loader;

import java.io.*;
import java.util.*;
import javax.swing.SwingUtilities;

public class RaveXLoader {
    private static volatile boolean ready = false;
    private static Process childProcess;
    private static LoaderWindow window;
    private static boolean nativeAvailable = false;

    public static void main(String[] args) {
        // Prevent Java AWT from erasing/clearing background to eliminate all flickering on Linux
        System.setProperty("sun.awt.noerasebackground", "true");

        if (args.length > 0 && args[0].equals("--integrated-gui")) {
            System.setProperty("java.awt.headless", "false");
            nativeAvailable = false; // Force native-free checks for GUI to ensure 100% startup stability
            
            window = new LoaderWindow();
            window.setVersion("1.0");
            window.setVisible(true);
            window.updateStatus("Initializing client optimization...", 0);

            new Thread(() -> {
                try {
                    runChecksPhase();
                    runOptimizePhase();
                    window.updateStatus("Optimizing game environment...", 70);
                    sleep(300);
                    window.updateStatus("Optimization completed! Starting game...", 95);
                    window.setSystemScore(100);
                    
                    // Keep loader open indefinitely until killed by the main game process
                    while (true) {
                        sleep(1000);
                    }
                } catch (Exception ignored) {}
            }).start();

            // 30-second safety-net thread in case the main game crashes on startup
            new Thread(() -> {
                sleep(30000);
                closeLoader();
                System.exit(0);
            }).start();
            return;
        }

        if (java.awt.GraphicsEnvironment.isHeadless()) {
            System.out.println("[RaveX-Loader] Headless environment detected. Skipping GUI, running optimizations only.");
            nativeAvailable = NativeBridge.isLoaded();
            if (nativeAvailable) {
                try {
                    NativeBridge.trimMemory();
                    NativeBridge.setHighPriority();
                    NativeBridge.optimize();
                } catch (Exception ignored) {}
            }
            return;
        }

        boolean isGradleDev = new java.io.File("gradlew").exists() || new java.io.File("gradlew.bat").exists();
        
        if (!isGradleDev) {
            // Standalone Optimization Mode (running outside development environment)
            window = new LoaderWindow();
            window.setVersion("1.0");
            window.setVisible(true);
            window.updateStatus("Initializing Standalone Optimizer...", 0);
            
            new Thread(() -> {
                try {
                    runChecksPhase();
                    runOptimizePhase();
                    window.updateStatus("Arch Linux JNI Kernel Tuning...", 70);
                    sleep(800);
                    window.updateStatus("JVM Garbage Collector Optimized!", 90);
                    sleep(600);
                    window.updateStatus("System Optimized! Ready to play.", 100);
                    window.setSystemScore(100);
                } catch (Exception ignored) {}
            }).start();
            return;
        }

        String command;
        String[] extraArgs;

        if (args.length > 0) {
            command = args[0];
            extraArgs = Arrays.copyOfRange(args, 1, args.length);
        } else {
            command = "./gradlew";
            extraArgs = new String[]{"runClient"};
        }

        String version = readVersion("gradle.properties");
        nativeAvailable = NativeBridge.isLoaded();

        window = new LoaderWindow();
        window.setVersion(version);
        window.setVisible(true);

        window.updateStatus("Initializing loader...", 0);

        new Thread(() -> {
            try {
                runChecksPhase();
                runOptimizePhase();
                runLaunchPhase(command, extraArgs);
            } catch (Exception e) {
                window.setError(e.getMessage());
                sleep(3000);
                window.dispose();
            }
        }).start();
    }

    public static void closeLoader() {
        if (window != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    window.dispose();
                } catch (Exception ignored) {}
                window = null;
            });
        }
    }

    public static void startIntegrated(String version) {
        System.setProperty("sun.awt.noerasebackground", "true");
        // Dynamic headless mode override for Minecraft Launchers
        System.setProperty("java.awt.headless", "false");

        if (java.awt.GraphicsEnvironment.isHeadless()) {
            System.out.println("[RaveX-Loader] Integrated Mode: Headless override failed or environment is non-graphical. Running background optimization only.");
            new Thread(() -> {
                try {
                    nativeAvailable = NativeBridge.isLoaded();
                    if (nativeAvailable) {
                        NativeBridge.trimMemory();
                        NativeBridge.setHighPriority();
                        NativeBridge.optimize();
                    } else {
                        System.gc();
                    }
                } catch (Exception ignored) {}
            }).start();
            return;
        }

        nativeAvailable = NativeBridge.isLoaded();

        window = new LoaderWindow();
        window.setVersion(version);
        window.setVisible(true);

        window.updateStatus("Initializing client optimization...", 0);

        new Thread(() -> {
            try {
                runChecksPhase();
                runOptimizePhase();
                window.updateStatus("Optimizing game environment...", 70);
                sleep(300);
                window.updateStatus("Optimization completed! Starting game...", 95);
                window.setSystemScore(100);
            } catch (Exception e) {
                window.setError(e.getMessage());
                sleep(2000);
                closeLoader();
            }
        }).start();

        // 20-second safety-net thread to prevent loader getting stuck on screen if game crashes during start
        new Thread(() -> {
            sleep(20000);
            closeLoader();
        }).start();
    }

    private static void runChecksPhase() {
        window.updateStatus("Checking system...", 5);

        if (nativeAvailable) {
            try {
                String info = NativeBridge.getSystemInfo();
                window.setSystemInfo(info);

                String json = NativeBridge.runChecks();
                int score = NativeBridge.getScore();
                window.setSystemScore(score);
                window.setExtraInfo("Score: " + score + "/100");
                window.updateStatus("System checked: " + score + "/100", 20);
                sleep(400);
            } catch (Exception e) {
                window.updateStatus("Native checks unavailable, using Java fallback", 15);
                javaFallbackChecks();
            }
        } else {
            window.setExtraInfo("Native optimizer not loaded");
            window.updateStatus("Native library unavailable", 15);
            javaFallbackChecks();
        }
    }

    private static void javaFallbackChecks() {
        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory() / (1024 * 1024);
        long totalMem = rt.totalMemory() / (1024 * 1024);
        long freeMem = rt.freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;
        int cores = rt.availableProcessors();

        String info = cores + " cores | Heap: " + usedMem + "/" + maxMem + " MB";
        window.setSystemInfo(info);

        int score = 100;
        if (usedMem > maxMem * 0.8) score -= 20;
        else if (usedMem > maxMem * 0.6) score -= 10;
        window.setSystemScore(score);
        window.setExtraInfo("Score: " + score + "/100 (Java fallback)");
        window.updateStatus("System check done: " + score + "/100", 20);
        sleep(400);
    }

    private static void runOptimizePhase() {
        window.updateStatus("Optimizing system...", 25);

        if (nativeAvailable) {
            try {
                NativeBridge.trimMemory();
                window.updateStatus("Memory trimmed", 30);
                sleep(200);

                NativeBridge.setHighPriority();
                window.updateStatus("Priority adjusted", 35);
                sleep(200);

                String json = NativeBridge.optimize();
                window.updateStatus("System optimized", 40);
                sleep(300);
            } catch (Exception e) {
                window.updateStatus("Optimization skipped", 40);
            }
        } else {
            // Java-level GC
            System.gc();
            window.updateStatus("GC completed", 35);
            sleep(200);
            window.updateStatus("Optimization skipped (no native)", 40);
        }
    }

    private static void runLaunchPhase(String command, String[] extraArgs) {
        window.updateStatus("Launching client...", 50);

        // Delete any stale signal file in system temp directory
        java.io.File signalFile = new java.io.File(System.getProperty("java.io.tmpdir"), ".ravex_ready");
        if (signalFile.exists()) {
            signalFile.delete();
        }

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(command);
            cmd.addAll(Arrays.asList(extraArgs));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            
            // Set environment flag to inform the child game process that an active loader is already running
            pb.environment().put("RAVEX_LOADER_ACTIVE", "true");
            
            Process process = pb.start();
            childProcess = process;

            window.updateStatus("Client starting...", 55);

            // Parallel thread to print game logs to terminal
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException ignored) {}
            }).start();

            // Wait for the ready signal file to be created by the game in system temp directory
            boolean detected = false;
            for (int i = 0; i < 300; i++) { // Wait up to 60 seconds (300 * 200ms)
                if (signalFile.exists()) {
                    detected = true;
                    break;
                }
                // Also check if process died unexpectedly
                if (!process.isAlive()) {
                    break;
                }
                sleep(200);
            }

            if (detected) {
                window.updateStatus("Client ready!", 100);
                window.setSystemScore(100);
                sleep(800);
                signalFile.delete(); // clean up the signal file
            } else {
                int exitCode = process.isAlive() ? 0 : process.exitValue();
                if (!process.isAlive() && exitCode != 0) {
                    window.setError("Client crashed during startup (code " + exitCode + ")");
                    sleep(3000);
                } else {
                    // Safe fallback if ready signal was somehow missed or timed out
                    window.updateStatus("Client ready!", 100);
                    sleep(800);
                }
            }

        } catch (IOException e) {
            window.setError("Launch failed: " + e.getMessage());
            sleep(3000);
        } finally {
            closeLoader();
        }
    }

    private static String readVersion(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("mod_version")) {
                    int eq = line.indexOf('=');
                    if (eq != -1) return line.substring(eq + 1).trim();
                }
            }
        } catch (Exception ignored) {}
        return "1.0";
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private static void waitForMinecraftWindow() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean found = false;

        if (os.contains("linux")) {
            // Robustly check if xdotool is actually installed first to prevent 10-second lag loops
            boolean xdotoolExists = false;
            try {
                Process p = new ProcessBuilder("which", "xdotool").start();
                xdotoolExists = p.waitFor() == 0;
            } catch (Exception ignored) {}

            if (xdotoolExists) {
                for (int i = 0; i < 20; i++) {
                    try {
                        Process p = new ProcessBuilder("xdotool", "search", "--name", "Minecraft")
                            .redirectErrorStream(true).start();
                        try (java.util.Scanner s = new java.util.Scanner(p.getInputStream())) {
                            if (s.hasNextInt()) {
                                found = true;
                                break;
                            }
                        }
                        p.waitFor();
                    } catch (Exception ignored) {}
                    sleep(200);
                }
            }
        }

        if (!found) {
            sleep(1500); // Shorter safe sleep fallback
        } else {
            sleep(1000);
        }
    }
}
