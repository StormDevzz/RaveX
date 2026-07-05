package ravex.addon.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AddonConfig {
    private final Map<String, String> properties = new HashMap<>();
    private final File file;

    public AddonConfig(File file) {
        this.file = file;
    }

    public void set(String key, String value) { properties.put(key, value); }
    public String get(String key, String def) { return properties.getOrDefault(key, def); }
    public void load() {}
    public void save() {}
}
