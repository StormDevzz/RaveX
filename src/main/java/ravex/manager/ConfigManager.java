package ravex.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ravex.modules.Module;
import ravex.modules.ModuleManager;
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
        configDir = new File(baseDir, "ravex/configs");
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
                }
                modObj.add("parameters", paramsObj);

                JsonObject extraObj = new JsonObject();
                m.saveExtra(extraObj);
                if (extraObj.size() > 0) {
                    modObj.add("extra", extraObj);
                }

                root.add(m.getName(), modObj);
            }

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
                            if (paramsObj.has(p.getName())) {
                                String valStr = paramsObj.get(p.getName()).getAsString();
                                setParameterValueRaw(p, valStr);
                            }
                        }
                    }

                    if (modObj.has("extra")) {
                        m.loadExtra(modObj.getAsJsonObject("extra"));
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
