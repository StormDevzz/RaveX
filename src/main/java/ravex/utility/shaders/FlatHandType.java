package ravex.utility.shaders;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.texture.AbstractTexture;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import com.mojang.blaze3d.platform.NativeImage;
import ravex.mcwrapper.MinecraftWrapper;

// the bare hand samples the player skin, so a tint only multiplies
// it. hand the model a white texel instead
public final class FlatHandType {

    private static Identifier whiteId;
    private static RenderType flatType;
    private static boolean failed;

    private FlatHandType() {}

    public static RenderType flatten(RenderType original) {
        if (original == null || failed) return original;
        if (whiteId == null) {
            whiteId = registerWhite();
            if (whiteId == null) { failed = true; return original; }
        }
        if (flatType == null) {
            // translucent keeps culling off, cutout edge stays at 0.1
            flatType = net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(whiteId);
        }
        return flatType;
    }

    private static Identifier registerWhite() {
        try {
            NativeImage img = new NativeImage(1, 1, false);
            img.setPixel(0, 0, 0xFFFFFFFF);
            DynamicTexture tex = new DynamicTexture(() -> "hand_flat_white", img);
            setLinearSampler(tex);
            Identifier id = Identifier.fromNamespaceAndPath("ravex", "hand_flat_white");
            MinecraftWrapper.getWrapper().getTextureManager().register(id, tex);
            return id;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Field samplerField;

    private static void setLinearSampler(AbstractTexture tex) {
        try {
            if (samplerField == null) {
                for (Field f : AbstractTexture.class.getDeclaredFields()) {
                    if (GpuSampler.class.isAssignableFrom(f.getType())) {
                        samplerField = f;
                        samplerField.setAccessible(true);
                        break;
                    }
                }
            }
            if (samplerField != null) {
                samplerField.set(tex, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            }
        } catch (Throwable ignored) {
        }
    }
}
