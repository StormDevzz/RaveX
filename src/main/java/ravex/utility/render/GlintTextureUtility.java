package ravex.utility.render;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import ravex.mcwrapper.MinecraftWrapper;

import java.io.InputStream;

// the glint pipeline has no vertex color, so the texture gets tinted
// the tinted copies keep the original texture ids
public final class GlintTextureUtility {
    // -1 = unknown, 0 = original texture
    private static int appliedItem = -1;
    private static int appliedArmor = -1;

    private GlintTextureUtility() {}

    public static void ensure(int color, boolean items, boolean armor) {
        int wantedItem = items ? (color | 0xFF000000) : 0;
        int wantedArmor = armor ? (color | 0xFF000000) : 0;
        if (wantedItem != appliedItem) {
            applySingle(net.minecraft.client.renderer.entity.ItemRenderer.ENCHANTED_GLINT_ITEM, wantedItem);
            appliedItem = wantedItem;
        }
        if (wantedArmor != appliedArmor) {
            applySingle(net.minecraft.client.renderer.entity.ItemRenderer.ENCHANTED_GLINT_ARMOR, wantedArmor);
            appliedArmor = wantedArmor;
        }
    }

    public static void ensureOriginal() {
        ensure(0, false, false);
    }

    private static void applySingle(Identifier id, int color) {
        try {
            var resource = MinecraftWrapper.getWrapper().getRaw().getResourceManager().getResource(id);
            if (resource.isEmpty()) return;
            NativeImage image;
            try (InputStream stream = resource.get().open()) {
                image = NativeImage.read(stream);
            }
            if (color != 0) {
                int tr = (color >> 16) & 0xFF;
                int tg = (color >> 8) & 0xFF;
                int tb = color & 0xFF;
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int rgba = image.getPixel(x, y);
                        int a = (rgba >> 24) & 0xFF;
                        int r = (rgba >> 16) & 0xFF;
                        int g = (rgba >> 8) & 0xFF;
                        int b = rgba & 0xFF;
                        int lum = Math.max(r, Math.max(g, b));
                        image.setPixel(x, y, (a << 24) | ((tr * lum / 255) << 16) | ((tg * lum / 255) << 8) | (tb * lum / 255));
                    }
                }
            }
            AbstractTexture tex = new DynamicTexture(() -> "ravex_glint", image);
            MinecraftWrapper.getWrapper().getTextureManager().register(id, tex);
        } catch (Throwable ignored) {
        }
    }
}
