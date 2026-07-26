package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import java.util.List;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.shaders.*;
import ravex.manager.HandShaderManager;
import ravex.manager.PlayerShaderManager;
import ravex.utility.shaders.nativec.ShaderNative;
@ModuleInfo(name = "Shaders", category = "Render")
public class Shaders extends ravex.modules.Module {
public static final ThreadLocal<Boolean> RENDERING_PLAYER = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> RENDERING_HAND = ThreadLocal.withInitial(() -> false);
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter throughWalls = new BooleanParameter("ThroughWalls", false);
    public final ColorParameter fillColor = new ColorParameter("Color", 0x77FF00A4);
    public final ModeParameter effectMode = new ModeParameter("Effect", "FireAura",
        List.of("FireAura", "EnergyGlow", "Chroma", "Ripple", "Pulse"));
    public Shaders() {
        
    }
    protected void onEnable() {
        ShaderNative.isAvailable();
        HandShaderManager.init();
        PlayerShaderManager.init();
        System.out.println("[RaveX-Shaders] Enabled. Native: " + ShaderNative.isAvailable());
    }
    protected void onDisable() {
        HandShaderManager.shutdown();
        PlayerShaderManager.shutdown();
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
        return ravex.manager.ModuleManager.INSTANCE.getByName("Shaders").getEnabled();
    }

    public static Shaders itz() {
        return ravex.manager.ModuleManager.delegate(Shaders.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}