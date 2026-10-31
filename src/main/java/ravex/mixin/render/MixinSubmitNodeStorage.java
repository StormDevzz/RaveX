package ravex.mixin.render;

import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ravex.modules.Modules;
import ravex.modules.render.Shaders;
import ravex.utility.shaders.FlatHandType;
import ravex.utility.shaders.ShaderFx;

@Mixin(SubmitNodeStorage.class)
public class MixinSubmitNodeStorage {
    // flatten to white so the tint wins
    @ModifyVariable(
        method = "submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ZZILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;I)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private net.minecraft.client.renderer.rendertype.RenderType ravexFlatHandType(
            net.minecraft.client.renderer.rendertype.RenderType renderType) {
        if (renderType == null) return renderType;
        if (!Modules.enabled(Shaders.class)) return renderType;
        if (!Shaders.RENDERING_HAND.get()) return renderType;
        Shaders shaders = Modules.get(Shaders.class);
        if (shaders == null || !shaders.hand) return renderType;
        return ravex.utility.shaders.FlatHandType.flatten(renderType);
    }

    // hand goes through submitModelPart, not renderToBuffer
    @ModifyVariable(
        method = "submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ZZILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;I)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 2
    )
    private int ravexHandTint(int tintedColor) {
        Shaders shaders = Modules.get(Shaders.class);
        if (shaders == null || !Modules.enabled(Shaders.class)) return tintedColor;
        boolean hand = Shaders.RENDERING_HAND.get();
        boolean player = Shaders.RENDERING_PLAYER.get();
        if (!hand && !player) return tintedColor;
        // hand has its own toggle
        if (hand && !shaders.hand) return tintedColor;
        // world players use the players toggle
        if (!hand && player && !shaders.players) return tintedColor;
        return ravexShadedTint(shaders);
    }

    // same mix as held items
    private static int ravexShadedTint(Shaders shaders) {
        int base = shaders.appliedColor();
        int alpha = shaders.appliedAlpha();
        int fx = shaders.fx();
        if (fx == ShaderFx.STATIC || !ShaderFx.available()) return (alpha << 24) | (base & 0x00FFFFFF);
        return ShaderFx.tint(fx, Shaders.tickTime(), base, alpha, 0f, 0f, 0f);
    }
}
