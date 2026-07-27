package ravex.modules.client;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import net.minecraft.network.chat.Component;

import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "Calculator", category = "Client")
public class Calculator implements ModuleAccess {
private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_calculator");
    static {
        NATIVE.load();
    }
    public void onEnable() {
        if (!NATIVE.isLoaded()) {
            var mc = MinecraftWrapper.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("§7[§5Calculator§7] §cNative library not found!"), false);
            }
            ravex.manager.ModuleManager.INSTANCE.getByName("Calculator").setEnabled(false);
            return;
        }
        openCalculator();
    }
    public void onDisable() {
        if (NATIVE.isLoaded()) {
            closeCalculator();
        }
    }
    public static void onNativeClose() {
        var mc = MinecraftWrapper.getInstance();
        mc.execute(() -> {
            if (ravex.manager.ModuleManager.INSTANCE.getByName("Calculator").getEnabled()) {
                ravex.manager.ModuleManager.INSTANCE.getByName("Calculator").setEnabled(false);
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


}