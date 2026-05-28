package ravex.modules.misc;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.List;

public class Optimizer extends Module {
    public static final Optimizer INSTANCE = new Optimizer();

    public final ModeParameter    gcMode       = new ModeParameter("GC Mode", "Aggressive",
            List.of("Soft", "Normal", "Aggressive"));
    public final BooleanParameter notify       = new BooleanParameter("Chat Notify", true);
    public final BooleanParameter useNative    = new BooleanParameter("Native C++", true);

    private boolean nativeAvailable = ravex.utility.misc.NativeLoader.isNativeAvailable();

    private Optimizer() {
        super("Optimizer", Category.MISC);
        addParameter(gcMode);
        addParameter(notify);
        addParameter(useNative);
    }

    static {
        ravex.utility.misc.NativeLoader.load();
    }

    @Override
    protected void onEnable() {
        runOptimization();
        setEnabled(false);
    }

    private void runOptimization() {
        Minecraft mc = Minecraft.getInstance();
        String mode = gcMode.getValue();

        if (useNative.getValue()) {
            try {
                String result = nativeOptimize(mode);
                if (result != null && notify.getValue() && mc.player != null) {
                    mc.player.displayClientMessage(
                        Component.literal("§7[§cRaveX§7] §a" + result), false);
                }
                return;
            } catch (UnsatisfiedLinkError e) {
                nativeAvailable = false;
            }
        }

        switch (mode) {
            case "Aggressive" -> {
                System.gc(); System.gc(); System.gc();
            }
            case "Normal" -> System.gc();
            case "Soft" -> {
                Runtime rt = Runtime.getRuntime();
                long used = rt.totalMemory() - rt.freeMemory();
                long max  = rt.maxMemory();
                if ((double) used / max > 0.65) System.gc();
            }
        }

        if (notify.getValue() && mc.player != null) {
            Runtime rt = Runtime.getRuntime();
            long freeMb = (rt.maxMemory() - (rt.totalMemory() - rt.freeMemory())) / (1024L * 1024L);
            mc.player.displayClientMessage(
                Component.literal("§7[§cRaveX§7] §aOptimizer: ~" + freeMb + " MB free after " + mode + " GC."),
                false);
        }
    }

    public boolean isNativeAvailable() {
        return nativeAvailable;
    }

    private static native String nativeOptimize(String mode);
    private static native long   nativeFreeMemory();
    private static native String[] nativeListTechniques();
}
