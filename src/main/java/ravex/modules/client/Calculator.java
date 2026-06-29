package ravex.modules.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.utility.misc.NativeLoader;


public class Calculator extends Module {
    public static final Calculator INSTANCE = new Calculator();

    private static boolean nativeAvailable = false;

    static {
        nativeAvailable = NativeLoader.loadLibrary("ravex_calculator");
    }

    private Calculator() {
        super("Calculator", Category.CLIENT);
    }

    @Override
    protected void onEnable() {
        if (!nativeAvailable) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("§7[§5Calculator§7] §cNative library not found!"), false);
            }
            setEnabled(false);
            return;
        }
        openCalculator();
    }

    @Override
    protected void onDisable() {
        if (nativeAvailable) {
            closeCalculator();
        }
    }

    public static void onNativeClose() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (INSTANCE.getEnabled()) {
                INSTANCE.setEnabled(false);
            }
        });
    }

    
    private static native void openCalculator();
    private static native void closeCalculator();
    public static native String nativeEvaluate(String expr);
}
