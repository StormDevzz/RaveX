package ravex.modules.client;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import ravex.utility.nativelib.NativeLibraryUtility;
@ModuleInfo(name = "Calculator", category = "Client")
public class Calculator extends ravex.modules.Module {
private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_calculator");
    static {
        NATIVE.load();
    }
    protected void onEnable() {
        if (!NATIVE.isLoaded()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("§7[§5Calculator§7] §cNative library not found!"), false);
            }
            enabled = false;
            return;
        }
        openCalculator();
    }
    protected void onDisable() {
        if (NATIVE.isLoaded()) {
            closeCalculator();
        }
    }
    public static void onNativeClose() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (ravex.manager.ModuleManager.delegate(Calculator.class).getEnabled()) {
                ravex.manager.ModuleManager.delegate(Calculator.class).setEnabled(false);
            }
        });
    }
    private static native void openCalculator();
    private static native void closeCalculator();
    public static native String nativeEvaluate(String expr);

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Calculator").getEnabled();
    }

    public static Calculator itz() {
        return ravex.manager.ModuleManager.delegate(Calculator.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}