package ravex.loader;

import java.io.*;
import java.util.*;

public class ProcessLauncher {

    public void launch(String command, String[] extraArgs, LoaderCallback callback) {
        File signalFile = new File(System.getProperty("java.io.tmpdir"), ".ravex_ready");
        if (signalFile.exists()) signalFile.delete();

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(command);
            cmd.addAll(Arrays.asList(extraArgs));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.environment().put("RAVEX_LOADER_ACTIVE", "true");

            Process process = pb.start();

            callback.updateStatus("Client starting...", 55);

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException ignored) {}
            }).start();

            boolean detected = false;
            for (int i = 0; i < 300; i++) {
                if (signalFile.exists()) {
                    detected = true;
                    break;
                }
                if (!process.isAlive()) break;
                sleep(200);
            }

            if (detected) {
                callback.updateStatus("Client ready!", 100);
                callback.setSystemScore(100);
                sleep(800);
                signalFile.delete();
            } else {
                int exitCode = process.isAlive() ? 0 : process.exitValue();
                if (!process.isAlive() && exitCode != 0) {
                    callback.setError("Client crashed during startup (code " + exitCode + ")");
                    sleep(3000);
                } else {
                    callback.updateStatus("Client ready!", 100);
                    sleep(800);
                }
            }
        } catch (IOException e) {
            callback.setError("Launch failed: " + e.getMessage());
            sleep(3000);
        } finally {
            callback.dispose();
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
