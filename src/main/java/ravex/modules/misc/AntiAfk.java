package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.chat.Component;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "AntiAfk", category = "Misc")
public class AntiAfk {
    @Parameter(name = "Interval", min = 5.0, max = 60.0, step = 1.0)
    public double interval = 12.0;
    @Parameter(name = "MouseMove")
    public boolean mouseMove = true;
    @Parameter(name = "KeyPress")
    public boolean keyPress = true;
    @Parameter(name = "LookAround")
    public boolean lookAround = true;
    @Parameter(name = "Jump")
    public boolean jump = true;
    @Parameter(name = "Rotation", min = 10.0, max = 180.0, step = 5.0)
    public double rotationRange = 45.0;
    @Parameter(name = "DebugLog")
    public boolean debugLog = false;

    static {
        ravex.utility.nativelib.NativeLoader.load();
    }
    public void onEnable() {
        try {
            int intervalMs = (int)(interval * 1000.0);
            int jitterMs   = (int)(intervalMs * 0.3);
            int rotRange   = (int) rotationRange;
            boolean ok = nativeStart(intervalMs, jitterMs,
                mouseMove, false,
                keyPress, lookAround,
                jump, rotRange);
            var mc = MinecraftWrapper.getInstance();
            if (ok) {
                if (debugLog && mc.player != null) {
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
    public void onDisable() {
        try {
            nativeStop();
        } catch (UnsatisfiedLinkError ignored) {}
        var mc = MinecraftWrapper.getInstance();
        if (debugLog && mc.player != null) {
            mc.player.displayClientMessage(
                Component.literal("§7[§cRaveX§7] §cAntiAFK stopped"), false);
        }
    }
    private void startFallback() {
        var mc = MinecraftWrapper.getInstance();
        if (debugLog && mc.player != null) {
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