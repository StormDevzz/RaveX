package ravex.modules.client;
import ravex.modules.annotations.Module;
import net.minecraft.network.chat.Component;

import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "Calculator", category = "Client")
public class Calculator {
private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_calculator");
    public void onEnable() {
        if (!NATIVE.isLoaded()) {
            var mc = MinecraftWrapper.getWrapper();
            if (mc.getPlayer() != null) {
                mc.getPlayer().displayClientMessage(
                    Component.literal("§7[§5Calculator§7] §cNative library not found!"), false);
            }
            Modules.setEnabled(Calculator.class, false);
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
        var mc = MinecraftWrapper.getWrapper();
        mc.execute(() -> {
            if (Modules.enabled(Calculator.class)) {
                Modules.setEnabled(Calculator.class, false);
            }
        });
    }
    private static native void openCalculator();
    private static native void closeCalculator();
    public static native String nativeEvaluate(String expr);






}