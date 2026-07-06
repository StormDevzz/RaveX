package ravex.utility.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import ravex.RaveX;

import java.lang.reflect.Method;

public class BlurUtility {
    private static final Identifier BLUR_ID = Identifier.withDefaultNamespace("blur");
    private static boolean active = false;
    private static Method setPostEffect;
    private static Method clearPostEffect;

    static {
        try {
            setPostEffect = GameRenderer.class.getDeclaredMethod("setPostEffect", Identifier.class);
            setPostEffect.setAccessible(true);
            clearPostEffect = GameRenderer.class.getDeclaredMethod("clearPostEffect");
            clearPostEffect.setAccessible(true);
        } catch (Exception e) {
            RaveX.LOGGER.warn("[BlurUtility] Failed to find methods: {}", e.getMessage());
        }
    }

    public static void enable() {
        if (active || setPostEffect == null) return;
        try {
            setPostEffect.invoke(Minecraft.getInstance().gameRenderer, BLUR_ID);
            active = true;
        } catch (Exception e) {
            RaveX.LOGGER.warn("[BlurUtility] Failed to enable blur: {}", e.getMessage());
        }
    }

    public static void disable() {
        if (!active || clearPostEffect == null) return;
        try {
            clearPostEffect.invoke(Minecraft.getInstance().gameRenderer);
            active = false;
        } catch (Exception e) {
            RaveX.LOGGER.warn("[BlurUtility] Failed to disable blur: {}", e.getMessage());
        }
    }

    public static boolean isActive() {
        return active;
    }
}
