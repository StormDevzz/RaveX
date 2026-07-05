package ravex.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import ravex.macro.Macro;
import ravex.macro.MacroAction;
import ravex.manager.ModuleManager;
import ravex.modules.Module;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MacroManager {
    public static final MacroManager INSTANCE = new MacroManager();

    private final List<Macro> macros = new ArrayList<>();
    private final File configFile;
    private final Gson gson;
    private boolean[] wasKeyDown = new boolean[GLFW.GLFW_KEY_LAST + 1];

    private MacroManager() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        configFile = FabricLoader.getInstance().getConfigDir().resolve("ravex").resolve("macros.json").toFile();
    }

    public List<Macro> getMacros() { return macros; }

    public void addMacro(Macro macro) { macros.add(macro); save(); }

    public void removeMacro(Macro macro) { macros.remove(macro); save(); }

    public void load() {
        if (!configFile.exists()) return;
        try (FileReader reader = new FileReader(configFile)) {
            Type type = new TypeToken<List<Macro>>(){}.getType();
            List<Macro> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                macros.clear();
                macros.addAll(loaded);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        configFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(macros, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getWindow() == null) return;

        long handle = mc.getWindow().handle();
        for (Macro macro : macros) {
            int key = macro.getKeyBind();
            if (key < 0 || key > GLFW.GLFW_KEY_LAST) continue;

            boolean isDown = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
            if (isDown && !wasKeyDown[key]) {
                executeMacro(macro);
            }
            wasKeyDown[key] = isDown;
        }
    }

    private void executeMacro(Macro macro) {
        Minecraft mc = Minecraft.getInstance();
        for (MacroAction action : macro.getActions()) {
            switch (action.getType()) {
                case TOGGLE_MODULE:
                    Module mod = ModuleManager.INSTANCE.getByName(action.getData());
                    if (mod != null) mod.toggle();
                    break;
                case SEND_CHAT:
                    if (mc.player != null) mc.player.connection.sendChat(action.getData());
                    break;
                case EXECUTE_COMMAND:
                    if (mc.player != null) mc.player.connection.sendCommand(action.getData());
                    break;
                case DELAY:
                    try {
                        Thread.sleep(Long.parseLong(action.getData()));
                    } catch (Exception ignored) {}
                    break;
            }
        }
    }
}
