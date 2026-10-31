package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.shaders.ShaderFx;

@Module(name = "Shaders", category = "Render")
public class Shaders {
    // effect engine dll, NativeLibraryUtility loads it from the resources on enable
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_shaderfx");

    public static final ThreadLocal<Boolean> RENDERING_PLAYER = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> RENDERING_HAND = ThreadLocal.withInitial(() -> false);
    @Parameter(name = "Players")
    public boolean players = true;
    @Parameter(name = "Items")
    public boolean items = true;
    @Parameter(name = "Hand")
    public boolean hand = true;
    @Parameter(name = "Color", color = true)
    public int fillColor = 0x7740A9F8;
    // shared opacity for hands, items, players
    @Parameter(name = "Opacity", min = 0.1, max = 1.0, step = 0.05)
    public double opacity = 1.0;
    @Parameter(name = "Effect", modes = {"Static", "FireAura", "EnergyGlow", "Chroma"})
    public String effectMode = "FireAura";

    private static long lastTimeNanos;
    private static float timeSeconds;

    public static float tickTime() {
        long now = System.nanoTime();
        if (lastTimeNanos == 0L) lastTimeNanos = now;
        float dt = (now - lastTimeNanos) / 1_000_000_000.0f;
        lastTimeNanos = now;
        if (dt > 0.1f) dt = 0.1f;
        timeSeconds += dt;
        return timeSeconds;
    }

    public int appliedAlpha() {
        int a = (int) Math.round(opacity * 255.0);
        if (a < 1) a = 1;
        if (a > 255) a = 255;
        return a;
    }

    // colour the render paths use; the picker stays in charge in every mode.
    public int appliedColor() {
        return fillColor;
    }

    // native effect id, the whole formula lives in the dll
    public int fx() {
        return ShaderFx.idOf(effectMode);
    }

    // native effect engine availability, the render paths ask this before shading
    public static boolean nativeReady() {
        return NATIVE.isLoaded();
    }

    public void onEnable() {
        // native engine only
        NATIVE.load();
        // unknown old mode falls back to Static
        if (ShaderFx.idOf(effectMode) == ShaderFx.STATIC && !"Static".equals(effectMode)) effectMode = "Static";
        System.out.println("[RaveX-Shaders] Enabled. Native: " + NATIVE.isLoaded());
    }
}
