package ravex.telemetry;

import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TelemetryManager {
    public static final TelemetryManager INSTANCE = new TelemetryManager();

    private static final String API_URL = "https:
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private boolean sent = false;

    private TelemetryManager() {}

    private static String handshake() {
        int[] a = BuildConfig._p0;
        int[] b = BuildConfig._p1;
        char[] buf = new char[a.length];
        for (int i = 0; i < a.length; i++) {
            buf[i] = (char)(a[i] ^ b[i]);
        }
        return new String(buf);
    }

    public void sendTelemetry() {
        if (sent) return;

        JsonObject data = new JsonObject();
        data.addProperty("version", ravex.RaveX.version);
        data.addProperty("os_name", System.getProperty("os.name"));
        data.addProperty("os_arch", System.getProperty("os.arch"));
        data.addProperty("os_version", System.getProperty("os.version"));
        data.addProperty("java_version", System.getProperty("java.version"));
        data.addProperty("total_memory", Runtime.getRuntime().totalMemory());
        data.addProperty("available_processors", Runtime.getRuntime().availableProcessors());

        String mcVersion = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        data.addProperty("mc_version", mcVersion);
        data.addProperty("mods_count", net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods().size());

        String body = data.toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("User-Agent", "RaveX-Client")
                .header("X-Telemetry-Token", handshake())
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        ravex.RaveX.LOGGER.info("[Telemetry] Data sent successfully");
                        sent = true;
                    } else {
                        ravex.RaveX.LOGGER.warn("[Telemetry] Server returned {}", response.statusCode());
                    }
                })
                .exceptionally(ex -> {
                    ravex.RaveX.LOGGER.error("[Telemetry] Failed to send: {}", ex.getMessage());
                    return null;
                });
    }

    public void reset() {
        sent = false;
    }
}
