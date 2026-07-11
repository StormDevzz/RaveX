package ravex.modules.client;
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ravex.modules.Module;
import ravex.utility.nativelib.NativeLibrary;
public class Calculator extends Module {
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_calculator");
    static {
        NATIVE.load();
    }

    @Override
    protected void onEnable() {
        if (!NATIVE.isLoaded()) {
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
        if (NATIVE.isLoaded()) {
            closeCalculator();
        }
    }
    public static void onNativeClose() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (ModuleManager.get(Calculator.class).getEnabled()) {
                ModuleManager.get(Calculator.class).setEnabled(false);
            }
        });
    }
    private static native void openCalculator();
    private static native void closeCalculator();
    public static native String nativeEvaluate(String expr);

    public static boolean maybeEnabled() {
        return maybeEnabled(Calculator.class);
    }

    public static Calculator itz() {
        return ModuleManager.get(Calculator.class);
    }
}
