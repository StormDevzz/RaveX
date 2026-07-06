package ravex.modules.render;

import java.util.List;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.shaders.*;
import ravex.shaders.hand.HandShaderManager;
import ravex.shaders.player.PlayerShaderManager;
import ravex.shaders.nativec.ShaderNative;

public class Shaders extends Module {
    public static final Shaders INSTANCE = new Shaders();

    public static final ThreadLocal<Boolean> RENDERING_PLAYER = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> RENDERING_HAND = ThreadLocal.withInitial(() -> false);

    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter throughWalls = new BooleanParameter("Through Walls", false);
    public final ColorParameter fillColor = new ColorParameter("Color", 0x77FF00A4);
    public final ModeParameter effectMode = new ModeParameter("Effect", "Fire Aura",
        List.of("Fire Aura", "Energy Glow", "Chroma", "Ripple", "Pulse"));

    public Shaders() {
        super("Shaders", Category.RENDER);
        addParameter(players);
        addParameter(throughWalls);
        addParameter(fillColor);
        addParameter(effectMode);
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
    }

    public ShaderConfig createConfig() {
        ShaderConfig cfg = new ShaderConfig();
        cfg.enabled = true;
        cfg.intensity = 1f;
        cfg.throughWalls = throughWalls.getValue();
        switch (effectMode.getValue()) {
            case "Fire Aura":   cfg.effect = EffectType.FIRE_AURA; break;
            case "Energy Glow": cfg.effect = EffectType.ENERGY_GLOW; break;
            case "Chroma":      cfg.effect = EffectType.CHROMA; break;
            case "Ripple":      cfg.effect = EffectType.RIPPLE; break;
            case "Pulse":       cfg.effect = EffectType.PULSE; break;
        }
        return cfg;
    }
}
