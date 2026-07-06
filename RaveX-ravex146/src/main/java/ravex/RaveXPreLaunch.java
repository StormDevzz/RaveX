package ravex;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.List;

public class RaveXPreLaunch implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LogManager.getLogger("ravex-prelaunch");

    @Override
    public void onPreLaunch() {
        LOGGER.info("[RaveX-PreLaunch] Early startup detected! Initializing loader process...");

        
        try {
            java.io.File signal = new java.io.File(System.getProperty("java.io.tmpdir"), ".ravex_ready");
            if (signal.exists()) {
                signal.delete();
            }
        } catch (Exception ignored) {}

        
        if (!"true".equals(System.getenv("RAVEX_LOADER_ACTIVE"))) {
            try {
                String jarPath = RaveX.getModJarPath();
                if (jarPath != null && !jarPath.isEmpty()) {
                    String javaExe = System.getProperty("java.home") + "/bin/java";
                    java.io.File exeFile = new java.io.File(javaExe);
                    if (!exeFile.exists()) {
                        javaExe = "java"; 
                    }
                    
                    List<String> cmd = new ArrayList<>();
                    cmd.add(javaExe);
                    cmd.add("-Djava.awt.headless=false");
                    cmd.add("-cp");
                    cmd.add(jarPath);
                    cmd.add("ravex.loader.RaveXLoader");
                    cmd.add("--integrated-gui");
                    
                    LOGGER.info("[RaveX-PreLaunch] Spawning safe loader process: " + cmd);
                    ProcessBuilder pb = new ProcessBuilder(cmd);
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    RaveX.setLoaderProcess(p);
                    
                    
                    new Thread(() -> {
                        try (java.io.BufferedReader r = new java.io.BufferedReader(
                                new java.io.InputStreamReader(p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = r.readLine()) != null) {
                                LOGGER.info("[RaveX-Loader] " + line);
                            }
                        } catch (Exception ignored) {}
                    }).start();
                } else {
                    LOGGER.warn("[RaveX-PreLaunch] Could not resolve mod JAR path! Skipping loader spawn.");
                }
            } catch (Exception e) {
                LOGGER.error("[RaveX-PreLaunch] Failed to spawn safe integrated loader GUI process", e);
            }
        } else {
            LOGGER.info("[RaveX-PreLaunch] Active external loader detected. Skipping duplicate loader spawn.");
        }
    }
}
