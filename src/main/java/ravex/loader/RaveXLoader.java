package ravex.loader;

import java.io.*;
import java.util.*;

public class RaveXLoader {
    private static volatile boolean ready = false;
    private static Process childProcess;
    private static LoaderWindow window;
    private static boolean nativeAvailable = false;

    public static void main(String[] args) {
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

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(command);
            cmd.addAll(Arrays.asList(extraArgs));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            childProcess = process;

            window.updateStatus("Client starting...", 55);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("[RaveX-Java] Loading complete") ||
                        line.contains("[RaveX-Java] Successful initialization!") ||
                        line.contains("RaveX successfully initialized")) {
                        ready = true;
                        window.updateStatus("Client ready!", 100);
                        window.setSystemScore(100);
                        waitForMinecraftWindow();
                        window.dispose();
                        return;
                    }

                    if (line.contains("[Loading]")) {
                        int pctPos = line.lastIndexOf('(');
                        int endPos = line.lastIndexOf(')');
                        if (pctPos != -1 && endPos != -1 && endPos > pctPos) {
                            try {
                                int pct = Integer.parseInt(
                                    line.substring(pctPos + 1, endPos).trim());
                                String stage;
                                int colon = line.indexOf("] ");
                                if (colon != -1) {
                                    stage = line.substring(colon + 2, pctPos).trim();
                                } else {
                                    stage = line.substring(pctPos + 1, endPos);
                                }
                                window.updateStatus(stage.isEmpty() ? "Loading..." : stage,
                                    Math.min(pct, 95));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }

            int exitCode = process.waitFor();
            if (!ready) {
                window.setError("Process exited with code " + exitCode);
                sleep(3000);
            }

        } catch (IOException e) {
            window.setError("Launch failed: " + e.getMessage());
            sleep(3000);
        } catch (InterruptedException e) {
            window.setError("Interrupted");
            sleep(1000);
        } finally {
            window.dispose();
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
            // Use xdotool to detect the Minecraft window
            for (int i = 0; i < 40; i++) {
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
                sleep(250);
            }
        }

        if (!found) {
            sleep(2500);
        } else {
            sleep(2000);
        }
    }
}
