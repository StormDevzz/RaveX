package ravex.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ravex.gui.profile.Profile;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ActionParameter;
import ravex.parameter.Parameter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileManager {
    public static final ProfileManager INSTANCE = new ProfileManager();

    private final List<Profile> profiles = new ArrayList<>();
    private final File profilesDir;
    private final Gson gson;

    private ProfileManager() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        profilesDir = FabricLoader.getInstance().getConfigDir().resolve("ravex").resolve("profiles").toFile();
    }

    public List<Profile> getProfiles() { return profiles; }

    public void load() {
        if (!profilesDir.exists()) return;
        File[] files = profilesDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        profiles.clear();
        for (File f : files) {
            try (FileReader reader = new FileReader(f)) {
                Profile profile = gson.fromJson(reader, Profile.class);
                if (profile != null) profiles.add(profile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void saveProfile(Profile profile) {
        profilesDir.mkdirs();
        File file = new File(profilesDir, profile.getName().replaceAll("[^a-zA-Z0-9_-]", "") + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(profile, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!profiles.contains(profile)) profiles.add(profile);
    }

    public void deleteProfile(Profile profile) {
        File file = new File(profilesDir, profile.getName().replaceAll("[^a-zA-Z0-9_-]", "") + ".json");
        if (file.exists()) file.delete();
        profiles.remove(profile);
    }

    public Profile captureCurrent(String name) {
        Map<String, Boolean> states = new HashMap<>();
        Map<String, Map<String, Object>> params = new HashMap<>();
        Map<String, Integer> keybinds = new HashMap<>();

        for (Module m : ModuleManager.INSTANCE.getModules()) {
            states.put(m.getName(), m.getEnabled());
            keybinds.put(m.getName(), m.getKeyBind());

            Map<String, Object> modParams = new HashMap<>();
            for (Parameter<?> p : m.getParameters()) {
                if (p instanceof ActionParameter) continue;
                modParams.put(p.getName(), p.getValue());
            }
            params.put(m.getName(), modParams);
        }

        return new Profile(name, states, params, keybinds);
    }

    public void applyProfile(Profile profile) {
        for (Module m : ModuleManager.INSTANCE.getModules()) {
            String name = m.getName();
            if (profile.getModuleStates().containsKey(name)) {
                m.setEnabled(profile.getModuleStates().get(name));
            }
            if (profile.getModuleKeyBinds().containsKey(name)) {
                m.setKeyBind(profile.getModuleKeyBinds().get(name));
            }
            if (profile.getModuleParameters().containsKey(name)) {
                Map<String, Object> savedParams = profile.getModuleParameters().get(name);
                for (Parameter<?> p : m.getParameters()) {
                    String pName = p.getName();
                    if (!savedParams.containsKey(pName)) {
                        for (String k : savedParams.keySet()) {
                            if (k.replace(" ", "").equals(pName)) {
                                pName = k;
                                break;
                            }
                        }
                    }
                    if (!savedParams.containsKey(pName)) continue;
                    if (p instanceof ActionParameter) continue;
                    p.setValueFromObject(savedParams.get(pName));
                }
            }
        }
    }
}
