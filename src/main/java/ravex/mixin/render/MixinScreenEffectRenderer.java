package ravex.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.NoRender;

@Mixin(ScreenEffectRenderer.class)
public class MixinScreenEffectRenderer {

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void onRenderFire(PoseStack poseStack, MultiBufferSource multiBufferSource, TextureAtlasSprite textureAtlasSprite, CallbackInfo ci) {
<<<<<<< HEAD
        if (NoRender.maybeEnabled() && NoRender.itz().fire.getValue()) {
=======
        if (NoRender.INSTANCE.getEnabled() && NoRender.INSTANCE.fire.getValue()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            ci.cancel();
        }
    }
}
