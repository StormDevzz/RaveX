package ravex.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.parameter.Parameter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    public static final ConfigManager INSTANCE = new ConfigManager();
    private final File configDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private ConfigManager() {
        File baseDir = null;
        try {
            baseDir = net.minecraft.client.Minecraft.getInstance().gameDirectory;
        } catch (Throwable ignored) {}

        if (baseDir == null) {
            baseDir = new File(".");
        }
        configDir = new File(baseDir, "RaveX/configs");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
    }

    public boolean save(String name) {
        try {
            File file = new File(configDir, name + ".json");
            JsonObject root = new JsonObject();

            for (Module m : ModuleManager.INSTANCE.getModules()) {
                JsonObject modObj = new JsonObject();
                modObj.addProperty("enabled", m.getEnabled());
                modObj.addProperty("keybind", m.getKeyBind());

                JsonObject paramsObj = new JsonObject();
                for (Parameter<?> p : m.getParameters()) {
                    if (p instanceof ravex.parameter.ActionParameter) continue;
                    paramsObj.addProperty(p.getName(), String.valueOf(p.getValue()));
                    if (p instanceof ravex.parameter.ColorParameter cp) {
                        paramsObj.addProperty(p.getName() + "_themeSync", cp.isThemeSync());
                    }
                }
                modObj.add("parameters", paramsObj);

                JsonObject extraObj = new JsonObject();
                m.saveExtra(extraObj);
                if (extraObj.size() > 0) {
                    modObj.add("extra", extraObj);
                }
                root.add(m.getName(), modObj);
            }

            JsonObject hudRoot = new JsonObject();
            for (Module hm : ModuleManager.INSTANCE.getHudModules()) {
                JsonObject hudObj = new JsonObject();
                hudObj.addProperty("enabled", hm.getEnabled());
                hudObj.addProperty("x", hm.getTargetX());
                hudObj.addProperty("y", hm.getTargetY());

                JsonObject paramsObj = new JsonObject();
                for (Parameter<?> p : hm.getParameters()) {
                    if (p instanceof ravex.parameter.ActionParameter) continue;
                    paramsObj.addProperty(p.getName(), String.valueOf(p.getValue()));
                }
                hudObj.add("parameters", paramsObj);
                hudRoot.add(hm.getName(), hudObj);
            }
            root.add("hud_modules", hudRoot);

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(root, writer);
            }
            return true;
        } catch (Exception e) {
            ravex.RaveX.LOGGER.error("[ConfigManager] Failed to save config: " + name, e);
            return false;
        }
    }

    public boolean load(String name) {
        try {
            File file = new File(configDir, name + ".json");
            if (!file.exists()) return false;

            JsonObject root;
            try (FileReader reader = new FileReader(file)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }

            for (Module m : ModuleManager.INSTANCE.getModules()) {
                if (root.has(m.getName())) {
                    JsonObject modObj = root.getAsJsonObject(m.getName());
                    if (modObj.has("enabled")) {
                        m.setEnabled(modObj.get("enabled").getAsBoolean());
                    }
                    if (modObj.has("keybind")) {
                        m.setKeyBind(modObj.get("keybind").getAsInt());
                    }

                    if (modObj.has("parameters")) {
                        JsonObject paramsObj = modObj.getAsJsonObject("parameters");
                        for (Parameter<?> p : m.getParameters()) {
                            String key = p.getName();
                            if (!paramsObj.has(key)) {
                                for (String k : paramsObj.keySet()) {
                                    if (k.replace(" ", "").equals(key)) {
                                        key = k;
                                        break;
                                    }
                                }
                            }
                            if (paramsObj.has(key)) {
                                String valStr = paramsObj.get(key).getAsString();
                                setParameterValueRaw(p, valStr);
                            }
                            if (p instanceof ravex.parameter.ColorParameter cp) {
                                String syncKey = p.getName() + "_themeSync";
                                String actualSyncKey = syncKey;
                                if (!paramsObj.has(syncKey)) {
                                    for (String k : paramsObj.keySet()) {
                                        if (k.replace(" ", "").equals(syncKey)) {
                                            actualSyncKey = k;
                                            break;
                                        }
                                    }
                                }
                                if (paramsObj.has(actualSyncKey)) {
                                    cp.setThemeSync(paramsObj.get(actualSyncKey).getAsBoolean());
                                }
                            }
                        }
                    }

                    if (modObj.has("extra")) {
                        m.loadExtra(modObj.getAsJsonObject("extra"));
                    }
                }
            }

            if (root.has("hud_modules")) {
                JsonObject hudRoot = root.getAsJsonObject("hud_modules");
                for (Module hm : ModuleManager.INSTANCE.getHudModules()) {
                    if (hudRoot.has(hm.getName())) {
                        JsonObject hudObj = hudRoot.getAsJsonObject(hm.getName());
                        if (hudObj.has("enabled")) {
                            hm.setEnabled(hudObj.get("enabled").getAsBoolean());
                        }
                        if (hudObj.has("x")) {
                            hm.setX(hudObj.get("x").getAsInt());
                        }
                        if (hudObj.has("y")) {
                            hm.setY(hudObj.get("y").getAsInt());
                        }

                        if (hudObj.has("parameters")) {
                            JsonObject paramsObj = hudObj.getAsJsonObject("parameters");
                            for (Parameter<?> p : hm.getParameters()) {
                                String key = p.getName();
                                if (!paramsObj.has(key)) {
                                    for (String k : paramsObj.keySet()) {
                                        if (k.replace(" ", "").equals(key)) {
                                            key = k;
                                            break;
                                        }
                                    }
                                }
                                if (paramsObj.has(key)) {
                                    String valStr = paramsObj.get(key).getAsString();
                                    setParameterValueRaw(p, valStr);
                                }
                            }
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            ravex.RaveX.LOGGER.error("[ConfigManager] Failed to load config: " + name, e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void setParameterValueRaw(Parameter<?> p, String valStr) {
        try {
            if (p.getValue() instanceof Boolean) {
                ((Parameter<Boolean>) p).setValue(Boolean.parseBoolean(valStr));
            } else if (p.getValue() instanceof Double) {
                ((Parameter<Double>) p).setValue(Double.parseDouble(valStr));
            } else if (p.getValue() instanceof Integer) {
                ((Parameter<Integer>) p).setValue(Integer.parseInt(valStr));
            } else if (p.getValue() instanceof String) {
                ((Parameter<String>) p).setValue(valStr);
            }
        } catch (Exception ignored) {}
    }

    public List<String> list() {
        List<String> list = new ArrayList<>();
        File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                list.add(f.getName().substring(0, f.getName().length() - 5));
            }
        }
        return list;
    }

    public boolean delete(String name) {
        File file = new File(configDir, name + ".json");
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
}
