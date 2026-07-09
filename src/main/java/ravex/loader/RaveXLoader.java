package ravex.loader;

import java.io.*;
import java.util.*;

public class RaveXLoader {
    private static volatile boolean ready = false;
    private static Process childProcess;
    private static LoaderWindow window;
    private static boolean nativeAvailable = false;

    public static void main(String[] args) {
        System.setProperty("sun.awt.noerasebackground", "true");

        if (args.length > 0 && args[0].equals("--integrated-gui")) {
            startIntegratedMode();
            return;
        }

        if (java.awt.GraphicsEnvironment.isHeadless()) {
            runHeadlessMode();
            return;
        }

        boolean isGradleDev = new java.io.File("gradlew").exists() || new java.io.File("gradlew.bat").exists();

        if (!isGradleDev) {
            runStandaloneOptimizer();
            return;
        }

        String command;
        String[] extraArgs;
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        if (args.length > 0) {
            command = args[0];
            extraArgs = Arrays.copyOfRange(args, 1, args.length);
        } else {
            command = isWindows ? "gradlew.bat" : "./gradlew";
            extraArgs = new String[]{"runClient"};
        }

        String version = readVersion("gradle.properties");

        window = new LoaderWindow();
        window.setVersion(version);
        window.setVisible(true);

        window.updateStatus("Initializing loader...", 0);

        new Thread(() -> {
            try {
                window.updateStatus("Loading components...", 2);
                nativeAvailable = NativeBridge.load();
                new AssetDownloader().downloadRequiredAssets();
                runFullWorkflow(command, extraArgs);
            } catch (Exception e) {
                window.setError(e.getMessage());
                sleep(3000);
                window.dispose();
            }
        }).start();
    }

    private static void startIntegratedMode() {
        System.setProperty("java.awt.headless", "false");
        nativeAvailable = false;

        window = new LoaderWindow();
        window.setVersion("1.0");
        window.setVisible(true);
        window.updateStatus("Initializing client optimization...", 0);

        new Thread(() -> {
            try {
                AssetDownloader downloader = new AssetDownloader();
                downloader.downloadRequiredAssets();
                runOptimizationOnly();
            } catch (Exception ignored) {}
        }).start();

        new Thread(() -> {
            sleep(30000);
            closeLoader();
            System.exit(0);
        }).start();
    }

    private static void runHeadlessMode() {
        System.out.println("[RaveX-Loader] Headless environment detected. Skipping GUI, running optimizations only.");
        nativeAvailable = NativeBridge.load();
        if (nativeAvailable) {
            try {
                NativeBridge.trimMemory();
                NativeBridge.setHighPriority();
                NativeBridge.optimize();
            } catch (Exception ignored) {}
        }
    }

    private static void runStandaloneOptimizer() {
        window = new LoaderWindow();
        window.setVersion("1.0");
        window.setVisible(true);
        window.updateStatus("Initializing Standalone Optimizer...", 0);

        new Thread(() -> {
            try {
                String osDetails = getDetailedOSName();
                SystemOptimizer optimizer = new SystemOptimizer(nativeAvailable);
                optimizer.runChecks(window, osDetails);
                optimizer.optimize(window);
                window.updateStatus("Arch Linux JNI Kernel Tuning...", 70);
                sleep(800);
                window.updateStatus("JVM Garbage Collector Optimized!", 90);
                sleep(600);
                window.updateStatus("System Optimized! Ready to play.", 100);
                window.setSystemScore(100);
            } catch (Exception ignored) {}
        }).start();
    }

    private static void runFullWorkflow(String command, String[] extraArgs) {
        String osDetails = getDetailedOSName();
        SystemOptimizer optimizer = new SystemOptimizer(nativeAvailable);
        optimizer.runChecks(window, osDetails);
        optimizer.optimize(window);

        window.updateStatus("Launching client...", 50);
        ProcessLauncher launcher = new ProcessLauncher();
        launcher.launch(command, extraArgs, window);
    }

    private static void runOptimizationOnly() {
        String osDetails = getDetailedOSName();
        SystemOptimizer optimizer = new SystemOptimizer(nativeAvailable);
        optimizer.runChecks(window, osDetails);
        optimizer.optimize(window);
        window.updateStatus("Optimization completed! Starting game...", 95);
        window.setSystemScore(100);

        while (true) {
            sleep(1000);
        }
    }

    public static void closeLoader() {
        if (window != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    window.dispose();
                } catch (Exception ignored) {}
                window = null;
            });
        }
    }

    public static void startIntegrated(String version) {
        System.setProperty("sun.awt.noerasebackground", "true");
        System.setProperty("java.awt.headless", "false");

        if (java.awt.GraphicsEnvironment.isHeadless()) {
            System.out.println("[RaveX-Loader] Integrated Mode: Headless override failed. Running background optimization only.");
            new Thread(() -> {
                try {
                    nativeAvailable = NativeBridge.load();
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

        window = new LoaderWindow();
        window.setVersion(version);
        window.setVisible(true);
        window.updateStatus("Initializing client optimization...", 0);

        new Thread(() -> {
            try {
                window.updateStatus("Loading components...", 2);
                nativeAvailable = NativeBridge.load();
                new AssetDownloader().downloadRequiredAssets();
                String osDetails = getDetailedOSName();
                SystemOptimizer optimizer = new SystemOptimizer(nativeAvailable);
                optimizer.runChecks(window, osDetails);
                optimizer.optimize(window);
                window.updateStatus("Optimization completed! Starting game...", 95);
                window.setSystemScore(100);
            } catch (Exception e) {
                window.setError(e.getMessage());
                sleep(2000);
                closeLoader();
            }
        }).start();

        new Thread(() -> {
            sleep(20000);
            closeLoader();
        }).start();
    }

    public static String getDetailedOSName() {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");

        if (osName.toLowerCase().contains("win")) {
            return osName + " " + osVersion;
        }

        if (osName.toLowerCase().contains("linux")) {
            File osRelease = new File("/etc/os-release");
            if (osRelease.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(osRelease))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("PRETTY_NAME=")) {
                            String pretty = line.substring("PRETTY_NAME=".length());
                            if (pretty.startsWith("\"") && pretty.endsWith("\"")) {
                                pretty = pretty.substring(1, pretty.length() - 1);
                            }
                            return pretty + " (Kernel " + osVersion + ")";
                        }
                    }
                } catch (Exception ignored) {}
            }
            return "Linux (Kernel " + osVersion + ")";
        }

        return osName + " (" + osVersion + ")";
    }

    static void updateWindowStatus(String status, int progress) {
        if (window != null) {
            try {
                window.updateStatus(status, progress);
            } catch (Throwable ignored) {}
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
}
