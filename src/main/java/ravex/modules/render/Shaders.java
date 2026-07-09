package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import java.util.List;
=======
import java.util.List;
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.shaders.*;
import ravex.manager.HandShaderManager;
import ravex.manager.PlayerShaderManager;
import ravex.utility.shaders.nativec.ShaderNative;
public class Shaders extends Module {
<<<<<<< HEAD
=======
    public static final Shaders INSTANCE = new Shaders();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public static final ThreadLocal<Boolean> RENDERING_PLAYER = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> RENDERING_HAND = ThreadLocal.withInitial(() -> false);
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter throughWalls = new BooleanParameter("ThroughWalls", false);
    public final ColorParameter fillColor = new ColorParameter("Color", 0x77FF00A4);
    public final ModeParameter effectMode = new ModeParameter("Effect", "FireAura",
        List.of("FireAura", "EnergyGlow", "Chroma", "Ripple", "Pulse"));
    public Shaders() {
        super("Shaders");
    }
    @Override
    protected void onEnable() {
        ShaderNative.isAvailable(); // trigger native load attempt
        HandShaderManager.init();
        PlayerShaderManager.init();
        System.out.println("[RaveX-Shaders] Enabled. Native: " + ShaderNative.isAvailable());
    }
    @Override
    protected void onDisable() {
        HandShaderManager.shutdown();
        PlayerShaderManager.shutdown();
<<<<<<< HEAD
    }
    public ShaderConfig createConfig() {
        ShaderConfig cfg = new ShaderConfig();
        cfg.enabled = true;
        cfg.intensity = 1f;
        cfg.throughWalls = throughWalls.getValue();
        switch (effectMode.getValue()) {
            case "FireAura":   cfg.effect = EffectType.FIRE_AURA; break;
            case "EnergyGlow": cfg.effect = EffectType.ENERGY_GLOW; break;
            case "Chroma":      cfg.effect = EffectType.CHROMA; break;
            case "Ripple":      cfg.effect = EffectType.RIPPLE; break;
            case "Pulse":       cfg.effect = EffectType.PULSE; break;
        }
        return cfg;
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(Shaders.class);
    }

    public static Shaders itz() {
        return ModuleManager.get(Shaders.class);
=======
    }
    public ShaderConfig createConfig() {
        ShaderConfig cfg = new ShaderConfig();
        cfg.enabled = true;
        cfg.intensity = 1f;
        cfg.throughWalls = throughWalls.getValue();
        switch (effectMode.getValue()) {
            case "FireAura":   cfg.effect = EffectType.FIRE_AURA; break;
            case "EnergyGlow": cfg.effect = EffectType.ENERGY_GLOW; break;
            case "Chroma":      cfg.effect = EffectType.CHROMA; break;
            case "Ripple":      cfg.effect = EffectType.RIPPLE; break;
            case "Pulse":       cfg.effect = EffectType.PULSE; break;
        }
        return cfg;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }
}
