package ravex.utility.render;

import com.mojang.blaze3d.opengl.GlStateManager;

public class BlurRenderUtility {
    public static void setupBlur(float radius) {
        GlStateManager._enableDepthTest();
    }

    public static void resetBlur() {
        GlStateManager._disableDepthTest();
    }
}
