package ravex.modules.client;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.modules.ModuleManager;
import net.minecraft.client.Minecraft;
import java.util.List;

public class DesktopGui extends Module {
    public static final DesktopGui INSTANCE = new DesktopGui();

    private static boolean nativeAvailable = false;
    static {
        try {
            System.loadLibrary("ravex_desktopgui");
            nativeAvailable = true;
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[DesktopGui JNI] Failed to load native library: " + e.getMessage());
        }
    }

    private DesktopGui() {
        super("DesktopGui", Category.CLIENT);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (!nativeAvailable) {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§7[§5DesktopGui§7] §cNative library not found!"), false);
            }
            setEnabled(false);
            return;
        }

        List<Module> modules = ModuleManager.INSTANCE.getModules();
        String[] names = new String[modules.size()];
        boolean[] states = new boolean[modules.size()];
        for (int i = 0; i < modules.size(); i++) {
            names[i] = modules.get(i).getName();
            states[i] = modules.get(i).getEnabled();
        }

        openDesktopGui(names, states);
    }

    @Override
    protected void onDisable() {
        if (nativeAvailable) {
            closeDesktopGui();
        }
    }

    public static void onModuleToggle(String name, boolean enabled) {
        if (INSTANCE.getEnabled() && nativeAvailable) {
            updateModuleState(name, enabled);
        }
    }

    public static void toggleModuleFromNative(String name) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            Module m = ModuleManager.INSTANCE.getByName(name);
            if (m != null) {
                m.toggle();
            }
        });
    }

    public static void onNativeClose() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (INSTANCE.getEnabled()) {
                INSTANCE.setEnabled(false);
            }
        });
    }

    private static native void openDesktopGui(String[] names, boolean[] states);
    private static native void updateModuleState(String name, boolean enabled);
    private static native void closeDesktopGui();
}
