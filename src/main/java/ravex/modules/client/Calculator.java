package ravex.modules.client;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ravex.modules.Module;
import ravex.utility.nativelib.NativeLibrary;
public class Calculator extends Module {
=======
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.utility.nativelib.NativeLibrary;
public class Calculator extends Module {
    public static final Calculator INSTANCE = new Calculator();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
            if (ModuleManager.get(Calculator.class).getEnabled()) {
                ModuleManager.get(Calculator.class).setEnabled(false);
=======
            if (INSTANCE.getEnabled()) {
                INSTANCE.setEnabled(false);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            }
        });
    }
    private static native void openCalculator();
    private static native void closeCalculator();
    public static native String nativeEvaluate(String expr);
<<<<<<< HEAD

    public static boolean maybeEnabled() {
        return maybeEnabled(Calculator.class);
    }

    public static Calculator itz() {
        return ModuleManager.get(Calculator.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
