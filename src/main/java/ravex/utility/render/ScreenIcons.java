package ravex.utility.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ravex.mcwrapper.MinecraftWrapper;
import java.io.InputStream;

// header icons of the toolbar screens, loaded from plain png files in the jar.
public final class ScreenIcons {
    private ScreenIcons() {}

    public static final Identifier CONFIGS = Identifier.fromNamespaceAndPath("ravex", "textures/screen_configs");
    public static final Identifier MACROS = Identifier.fromNamespaceAndPath("ravex", "textures/screen_macros");
    public static final Identifier PROFILES = Identifier.fromNamespaceAndPath("ravex", "textures/screen_profiles");

    private static boolean loaded = false;

    public static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        load(CONFIGS, "/assets/ravex/textures/screen_configs.png", "screen_configs");
        load(MACROS, "/assets/ravex/textures/screen_macros.png", "screen_macros");
        load(PROFILES, "/assets/ravex/textures/screen_profiles.png", "screen_profiles");
    }

    private static void load(Identifier id, String path, String name) {
        try (InputStream stream = ScreenIcons.class.getResourceAsStream(path)) {
            if (stream == null) return;
            NativeImage image = NativeImage.read(stream);
            // pngs are white, no tint needed
            DynamicTexture tex = new DynamicTexture(() -> name, image);
            Render2DUtility.setLinearSampler(tex);
            MinecraftWrapper.getWrapper().getTextureManager().register(id, tex);
        } catch (Exception ignored) {
        }
    }
}
