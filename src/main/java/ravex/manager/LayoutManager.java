package ravex.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ravex.RaveX;
import ravex.gui.clickgui.CategoryPanel;
import ravex.mcwrapper.MinecraftWrapper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LayoutManager {
    public static final LayoutManager INSTANCE = new LayoutManager();
    private final File layoutFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private LayoutManager() {
        File baseDir = null;
        try {
            baseDir = MinecraftWrapper.getWrapper().getRaw().gameDirectory;
        } catch (Throwable ignored) {}
        if (baseDir == null) baseDir = new File(".");
        layoutFile = new File(baseDir, "RaveX/clickgui_layout.json");
        layoutFile.getParentFile().mkdirs();
    }

    public void save(Map<String, CategoryPanel> panels, int width, int height, float scale) {
        try {
            double cx = width / 2.0;
            double cy = height / 2.0;
            JsonObject root = new JsonObject();
            for (Map.Entry<String, CategoryPanel> e : panels.entrySet()) {
                CategoryPanel p = e.getValue();
                JsonObject pos = new JsonObject();
                double rx = (p.getX() - cx) / width + 0.5;
                double ry = (p.getY() - cy) / height + 0.5;
                pos.addProperty("rx", rx);
                pos.addProperty("ry", ry);
                pos.addProperty("x", p.getX());
                pos.addProperty("y", p.getY());
                pos.addProperty("custom", p.isCustomPosition());
                root.add(e.getKey(), pos);
                root.add(e.getKey().toLowerCase(), pos);
            }
            try (FileWriter w = new FileWriter(layoutFile)) {
                gson.toJson(root, w);
            }
        } catch (Exception e) {
            RaveX.LOGGER.warn("[LayoutManager] Failed to save layout: {}", e.getMessage());
        }
    }

    public void save(Map<String, CategoryPanel> panels) {
        int sw = MinecraftWrapper.getWrapper().getWindow().getGuiScaledWidth();
        int sh = MinecraftWrapper.getWrapper().getWindow().getGuiScaledHeight();
        if (sw <= 0) sw = 960;
        if (sh <= 0) sh = 540;
        save(panels, sw, sh, 1.0f);
    }

    public void reset() {
        if (layoutFile.exists()) layoutFile.delete();
    }

    private static final List<String> ALL_CATEGORIES = Arrays.asList(
        "Combat", "Render", "Player", "Movement", "Misc", "World", "Client", "HUD", "Custom"
    );

    public Map<String, double[]> load() {
        Map<String, double[]> result = new HashMap<>();
        if (!layoutFile.exists()) return result;
        try (FileReader r = new FileReader(layoutFile)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            for (String cat : ALL_CATEGORIES) {
                String key = cat.toLowerCase();
                JsonObject pos = root.has(cat) ? root.getAsJsonObject(cat) : (root.has(key) ? root.getAsJsonObject(key) : null);
                if (pos != null) {
                    if (pos.has("custom") && !pos.get("custom").getAsBoolean()) {
                        continue;
                    }
                    if (pos.has("x") && pos.has("y")) {
                        result.put(cat, new double[]{pos.get("x").getAsDouble(), pos.get("y").getAsDouble()});
                    } else if (pos.has("rx") && pos.has("ry")) {
                        int sw = MinecraftWrapper.getWrapper().getWindow().getGuiScaledWidth();
                        int sh = MinecraftWrapper.getWrapper().getWindow().getGuiScaledHeight();
                        if (sw <= 0) sw = 960;
                        if (sh <= 0) sh = 540;
                        double rx = pos.get("rx").getAsDouble();
                        double ry = pos.get("ry").getAsDouble();
                        result.put(cat, new double[]{(rx - 0.5) * sw + sw / 2.0, (ry - 0.5) * sh + sh / 2.0});
                    }
                }
            }
        } catch (Exception e) {
            RaveX.LOGGER.warn("[LayoutManager] Failed to load layout: {}", e.getMessage());
        }
        return result;
    }
}
