package ravex.utility.render;

import com.mojang.blaze3d.opengl.GlStateManager;

public class GlowRenderUtility {
    public static void enableBlend() {
        GlStateManager._enableBlend();
    }

    public static void disableBlend() {
        GlStateManager._disableBlend();
    }
}
