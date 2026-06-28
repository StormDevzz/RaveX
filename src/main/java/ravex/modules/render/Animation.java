package ravex.modules.render;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ravex.RaveX;
import ravex.modules.Category;
import ravex.modules.Module;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Animation extends Module {
    public static final Animation INSTANCE = new Animation();

    private static final String CPA_PROJECT = "EhthJpjM";
    private static final String CLOTH_CONFIG_PROJECT = "9s6osm5g";
    private static final String PAL_PROJECT = "ha1mEyJS";
    private static final String API_BASE = "https://api.modrinth.com/v2/project/%s/version";

    private static final String CPA_MAIN = "com.github.razorplay01.cpa.ModTemplate";
    private static final String CPA_PKG = "com.github.razorplay01";

    private boolean cpaAvailable;
    private boolean cpaInitialized;
    private final List<Map.Entry<Field, Object>> toggleFields = new ArrayList<>();

    private Animation() {
        super("Animation", Category.RENDER);
    }

    private void ensureCPAInit() {
        if (cpaInitialized) return;
        cpaInitialized = true;
        try {
            Class<?> modClass = Class.forName(CPA_MAIN);
            Field configField = modClass.getField("CONFIG");
            Object config = configField.get(null);
            cpaAvailable = true;
            RaveX.LOGGER.info("[Animation] CPA found, scanning...");
            collectToggles(config);
            RaveX.LOGGER.info("[Animation] Found {} toggles", toggleFields.size());
        } catch (Exception e) {
            RaveX.LOGGER.info("[Animation] CPA not available: {}", e.toString());
            cpaAvailable = false;
        }
    }

    private void collectToggles(Object obj) {
        if (obj == null) return;
        Class<?> clazz = obj.getClass();
        String cn = clazz.getName();

        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            String fn = f.getName();

            if (cn.contains("LeanEffect") && fn.equals("enableLeanEffect")) {
                toggleFields.add(new AbstractMap.SimpleEntry<>(f, obj));
                continue;
            }

            if (fn.equals("isEnabled") &&
                (f.getType() == boolean.class || f.getType() == Boolean.class)) {
                toggleFields.add(new AbstractMap.SimpleEntry<>(f, obj));
            }
        }

        if (!cn.startsWith(CPA_PKG)) return;
        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            try {
                Object val = f.get(obj);
                if (val != null) collectToggles(val);
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        ensureCPAInit();
        if (cpaAvailable) {
            applyToggle(true);
        } else {
            downloadMods();
        }
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        ensureCPAInit();
        if (cpaAvailable) {
            applyToggle(false);
        }
    }

    private void applyToggle(boolean on) {
        int n = 0;
        for (Map.Entry<Field, Object> e : toggleFields) {
            try {
                e.getKey().setBoolean(e.getValue(), on);
                n++;
            } catch (Exception ignored) {}
        }
        RaveX.LOGGER.info("[Animation] {} ({} fields)", on ? "Enabled" : "Disabled", n);
    }

    private void downloadMods() {
        RaveX.LOGGER.info("[Animation] CPA not detected, downloading mods...");
        new Thread(() -> {
            try {
                Path modsDir = findModsDir();
                if (modsDir == null) return;
                HttpClient client = HttpClient.newHttpClient();
                downloadMod(client, modsDir, CPA_PROJECT, "cpa");
                downloadMod(client, modsDir, CLOTH_CONFIG_PROJECT, "cloth-config");
                downloadMod(client, modsDir, PAL_PROJECT, "PlayerAnimationLib");
                RaveX.LOGGER.info("[Animation] Downloads complete. Restart the game to load CPA.");
            } catch (Exception e) {
                RaveX.LOGGER.warn("[Animation] Download error: {}", e.toString());
            }
        }).start();
    }

    private Path findModsDir() {
        Path[] candidates = {Paths.get("run", "mods"), Paths.get("mods")};
        for (Path p : candidates) {
            if (Files.exists(p)) return p.toAbsolutePath().normalize();
        }
        return null;
    }

    private void downloadMod(HttpClient client, Path modsDir, String projectId, String label) throws Exception {
        String q = URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8);
        String gv = URLEncoder.encode("[\"1.21.11\"]", StandardCharsets.UTF_8);
        String urlStr = API_BASE.formatted(projectId) + "?loaders=" + q + "&game_versions=" + gv;

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(urlStr))
            .header("User-Agent", "RaveX/1.0")
            .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        JsonArray versions = new Gson().fromJson(res.body(), JsonArray.class);

        String dlUrl = null;
        String filename = null;
        for (int i = 0; i < versions.size(); i++) {
            JsonObject v = versions.get(i).getAsJsonObject();
            if ("release".equals(v.get("version_type").getAsString())) {
                JsonObject file = v.get("files").getAsJsonArray().get(0).getAsJsonObject();
                dlUrl = file.get("url").getAsString();
                filename = file.get("filename").getAsString();
                break;
            }
        }
        if (dlUrl == null) return;

        Path target = modsDir.resolve(filename);
        if (Files.exists(target)) return;

        HttpRequest dlReq = HttpRequest.newBuilder()
            .uri(URI.create(dlUrl))
            .header("User-Agent", "RaveX/1.0")
            .build();
        HttpResponse<InputStream> dlRes = client.send(dlReq, HttpResponse.BodyHandlers.ofInputStream());
        Files.copy(dlRes.body(), target, StandardCopyOption.REPLACE_EXISTING);
    }
}
