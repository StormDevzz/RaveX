package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;

import ravex.utility.shaders.EffectType;
import ravex.utility.shaders.ShaderConfig;
import ravex.manager.HandShaderManager;
import ravex.manager.PlayerShaderManager;
import ravex.utility.shaders.nativec.ShaderNative;
@Module(name = "Shaders", category = "Render")
public class Shaders {
public static final ThreadLocal<Boolean> RENDERING_PLAYER = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> RENDERING_HAND = ThreadLocal.withInitial(() -> false);
    @Parameter(name = "Players")
    public boolean players = true;
    @Parameter(name = "Items")
    public boolean items = true;
    @Parameter(name = "ThroughWalls")
    public boolean throughWalls = false;
    @Parameter(name = "Color", color = true)
    public int fillColor = 0x77FF00A4;
    @Parameter(name = "Effect", modes = {"FireAura", "EnergyGlow", "Chroma", "Ripple", "Pulse"})
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

    public void onEnable() {
        ShaderNative.init();
        HandShaderManager.init();
        PlayerShaderManager.init();
        System.out.println("[RaveX-Shaders] Enabled. Native: " + ShaderNative.isAvailable());
    }
    public void onDisable() {
        HandShaderManager.shutdown();
        PlayerShaderManager.shutdown();
    }
    public ShaderConfig createConfig() {
        ShaderConfig cfg = new ShaderConfig();
        cfg.enabled = true;
        cfg.intensity = 1f;
        cfg.throughWalls = throughWalls;
        switch (effectMode) {
            case "FireAura":   cfg.effect = EffectType.FIRE_AURA; break;
            case "EnergyGlow": cfg.effect = EffectType.ENERGY_GLOW; break;
            case "Chroma":      cfg.effect = EffectType.CHROMA; break;
            case "Ripple":      cfg.effect = EffectType.RIPPLE; break;
            case "Pulse":       cfg.effect = EffectType.PULSE; break;
        }
        return cfg;
    }
}
