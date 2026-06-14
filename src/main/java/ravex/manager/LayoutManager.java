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

    public void save(Map<Category, CategoryPanel> panels) {
        try {
            JsonObject root = new JsonObject();
            for (Map.Entry<Category, CategoryPanel> e : panels.entrySet()) {
                JsonObject pos = new JsonObject();
                pos.addProperty("x", e.getValue().getX());
                pos.addProperty("y", e.getValue().getY());
                root.add(e.getKey().name(), pos);
            }
            try (FileWriter w = new FileWriter(layoutFile)) {
                gson.toJson(root, w);
            }
        } catch (Exception e) {
            RaveX.LOGGER.warn("[LayoutManager] Failed to save layout: {}", e.getMessage());
        }
    }

    public Map<Category, int[]> load() {
        Map<Category, int[]> result = new HashMap<>();
        if (!layoutFile.exists()) return result;
        try (FileReader r = new FileReader(layoutFile)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            for (Category cat : Category.values()) {
                if (root.has(cat.name())) {
                    JsonObject pos = root.getAsJsonObject(cat.name());
                    int px = pos.get("x").getAsInt();
                    int py = pos.get("y").getAsInt();
                    result.put(cat, new int[]{px, py});
                }
            }
        } catch (Exception e) {
            RaveX.LOGGER.warn("[LayoutManager] Failed to load layout: {}", e.getMessage());
        }
        return result;
    }
}
