package ravex.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import ravex.RaveX;
import ravex.gui.clickgui.CategoryPanel;
import ravex.modules.Category;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class LayoutManager {
    public static final LayoutManager INSTANCE = new LayoutManager();
    private final File layoutFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private LayoutManager() {
        File baseDir = null;
        try {
            baseDir = Minecraft.getInstance().gameDirectory;
        } catch (Throwable ignored) {}
        if (baseDir == null) baseDir = new File(".");
        layoutFile = new File(baseDir, "ravex/clickgui_layout.json");
        layoutFile.getParentFile().mkdirs();
    }

    public void save(Map<Category, CategoryPanel> panels, int width, int height, float scale) {
        try {
            double cx = width / 2.0;
            double cy = height / 2.0;
            JsonObject root = new JsonObject();
            for (Map.Entry<Category, CategoryPanel> e : panels.entrySet()) {
                JsonObject pos = new JsonObject();
                double rx = (e.getValue().getX() - cx) * scale / width + 0.5;
                double ry = (e.getValue().getY() - cy) * scale / height + 0.5;
                pos.addProperty("rx", rx);
                pos.addProperty("ry", ry);
                root.add(e.getKey().name(), pos);
            }
            try (FileWriter w = new FileWriter(layoutFile)) {
                gson.toJson(root, w);
            }
        } catch (Exception e) {
            RaveX.LOGGER.warn("[LayoutManager] Failed to save layout: {}", e.getMessage());
        }
    }

    public void save(Map<Category, CategoryPanel> panels) {
        // Fallback save using standard dimensions
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        if (sw <= 0) sw = 960;
        if (sh <= 0) sh = 540;
        save(panels, sw, sh, 1.0f);
    }

    public void reset() {
        if (layoutFile.exists()) layoutFile.delete();
    }

    public Map<Category, double[]> load() {
        Map<Category, double[]> result = new HashMap<>();
        if (!layoutFile.exists()) return result;
        try (FileReader r = new FileReader(layoutFile)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            for (Category cat : Category.values()) {
                if (root.has(cat.name())) {
                    JsonObject pos = root.getAsJsonObject(cat.name());
                    double rx = 0.0;
                    double ry = 0.0;
                    if (pos.has("rx") && pos.has("ry")) {
                        rx = pos.get("rx").getAsDouble();
                        ry = pos.get("ry").getAsDouble();
                    } else if (pos.has("x") && pos.has("y")) {
                        // Compatibility with old absolute layouts
                        rx = pos.get("x").getAsDouble();
                        ry = pos.get("y").getAsDouble();
                    }
                    result.put(cat, new double[]{rx, ry});
                }
            }
        } catch (Exception e) {
            RaveX.LOGGER.warn("[LayoutManager] Failed to load layout: {}", e.getMessage());
        }
        return result;
    }
}
