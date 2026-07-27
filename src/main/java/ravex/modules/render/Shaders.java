package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.List;

import ravex.utility.shaders.*;
import ravex.manager.HandShaderManager;
import ravex.manager.PlayerShaderManager;
import ravex.utility.shaders.nativec.ShaderNative;
@Module(name = "Shaders", category = "Render")
public class Shaders {
public static final ThreadLocal<Boolean> RENDERING_PLAYER = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> RENDERING_HAND = ThreadLocal.withInitial(() -> false);
    @Parameter(name = "Players")
    public boolean players = true;
    @Parameter(name = "ThroughWalls")
    public boolean throughWalls = false;
    @Parameter(name = "Color", color = true)
    public int fillColor = 0x77FF00A4;
    @Parameter(name = "Effect", modes = {"FireAura", "EnergyGlow", "Chroma", "Ripple", "Pulse"})
    public String effectMode = "FireAura";
    public void onEnable() {
        ShaderNative.isAvailable();
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