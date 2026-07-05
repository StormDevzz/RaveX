package ravex.modules.misc;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
public class AntiAfk extends Module {
    public static final AntiAfk INSTANCE = new AntiAfk();
    public final NumberParameter interval   = new NumberParameter("Interval", 12.0, 5.0, 60.0, 1.0);
    public final BooleanParameter mouseMove = new BooleanParameter("Mouse Move", true);
    public final BooleanParameter keyPress  = new BooleanParameter("Key Press", true);
    public final BooleanParameter lookAround = new BooleanParameter("Look Around", true);
    public final BooleanParameter jump      = new BooleanParameter("Jump", true);
    public final NumberParameter rotationRange = new NumberParameter("Rotation", 45.0, 10.0, 180.0, 5.0);
    public final BooleanParameter debugLog = new BooleanParameter("Debug Log", false);

    static {
        ravex.utility.misc.NativeLoader.load();
    }
    @Override
    protected void onEnable() {
        try {
            int intervalMs = (int)(interval.getValue() * 1000.0);
            int jitterMs   = (int)(intervalMs * 0.3);
            int rotRange   = rotationRange.getValue().intValue();
            boolean ok = nativeStart(intervalMs, jitterMs,
                mouseMove.getValue(), false,
                keyPress.getValue(), lookAround.getValue(),
                jump.getValue(), rotRange);
            Minecraft mc = Minecraft.getInstance();
            if (ok) {
                if (debugLog.getValue() && mc.player != null) {
                    mc.player.displayClientMessage(
                        Component.literal("§7[§cRaveX§7] §aAntiAFK started (native)"), false);
                }
            } else {
                startFallback();
            }
        } catch (UnsatisfiedLinkError e) {
            startFallback();
        }
    }
    @Override
    protected void onDisable() {
        try {
            nativeStop();
        } catch (UnsatisfiedLinkError ignored) {}
        Minecraft mc = Minecraft.getInstance();
        if (debugLog.getValue() && mc.player != null) {
            mc.player.displayClientMessage(
                Component.literal("§7[§cRaveX§7] §cAntiAFK stopped"), false);
        }
    }
    private void startFallback() {
        Minecraft mc = Minecraft.getInstance();
        if (debugLog.getValue() && mc.player != null) {
            mc.player.displayClientMessage(
                Component.literal("§7[§cRaveX§7] §eAntiAFK fallback (Java)"), false);
        }
    }
    private native boolean nativeStart(int intervalMs, int maxJitterMs,
        boolean mouseMove, boolean mouseClick, boolean keyPress,
        boolean lookAround, boolean jumpSim, int rotationRange);
    private native void    nativeStop();
    private native boolean nativeIsRunning();
    private native boolean nativePerformAction();
}
