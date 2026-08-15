package ravex.utility.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ravex.mcwrapper.MinecraftWrapper;

import java.awt.Color;

public final class PaletteTextureUtility {
    public static final int SIZE = 256;

    private static Identifier hueId;
    private static NativeImage hueImage;
    private static DynamicTexture hueTexture;

    private static Identifier svId;
    private static NativeImage svImage;
    private static DynamicTexture svTexture;
    private static int svKey = -1;

    private static Identifier alphaId;
    private static NativeImage alphaImage;
    private static DynamicTexture alphaTexture;
    private static int alphaKey = -1;

    private PaletteTextureUtility() {}

    public static Identifier getHueGradient() {
        if (hueTexture == null) {
            hueImage = new NativeImage(SIZE, 1, false);
            for (int x = 0; x < SIZE; x++) {
                hueImage.setPixel(x, 0, Color.HSBtoRGB(x / (float) (SIZE - 1), 1.0f, 1.0f));
            }
            hueTexture = new DynamicTexture(() -> "palette_hue", hueImage);
            Render2DUtility.setLinearSampler(hueTexture);
            hueId = Identifier.fromNamespaceAndPath("ravex", "palette_hue");
            MinecraftWrapper.getWrapper().getTextureManager().register(hueId, hueTexture);
        }
        return hueId;
    }

    public static Identifier getSvGradient(float hue) {
        if (svTexture == null) {
            svImage = new NativeImage(SIZE, SIZE, false);
            svTexture = new DynamicTexture(() -> "palette_sv", svImage);
            Render2DUtility.setLinearSampler(svTexture);
            svId = Identifier.fromNamespaceAndPath("ravex", "palette_sv");
            MinecraftWrapper.getWrapper().getTextureManager().register(svId, svTexture);
            svKey = -1;
        }
        int key = Math.round(hue * 255);
        if (svKey != key) {
            for (int y = 0; y < SIZE; y++) {
                float v = 1.0f - y / (float) (SIZE - 1);
                for (int x = 0; x < SIZE; x++) {
                    svImage.setPixel(x, y, Color.HSBtoRGB(hue, x / (float) (SIZE - 1), v));
                }
            }
            svTexture.upload();
            svKey = key;
        }
        return svId;
    }

    public static Identifier getAlphaGradient(int rgb) {
        if (alphaTexture == null) {
            alphaImage = new NativeImage(SIZE, 1, false);
            alphaTexture = new DynamicTexture(() -> "palette_alpha", alphaImage);
            Render2DUtility.setLinearSampler(alphaTexture);
            alphaId = Identifier.fromNamespaceAndPath("ravex", "palette_alpha");
            MinecraftWrapper.getWrapper().getTextureManager().register(alphaId, alphaTexture);
            alphaKey = -1;
        }
        int key = rgb & 0x00FFFFFF;
        if (alphaKey != key) {
            for (int x = 0; x < SIZE; x++) {
                int a = Math.round(x / (float) (SIZE - 1) * 255.0f);
                alphaImage.setPixel(x, 0, (a << 24) | (rgb & 0x00FFFFFF));
            }
            alphaTexture.upload();
            alphaKey = key;
        }
        return alphaId;
    }
}
