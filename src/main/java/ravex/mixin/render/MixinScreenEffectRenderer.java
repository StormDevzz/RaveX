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
import ravex.modules.Modules;

@Mixin(ScreenEffectRenderer.class)
public class MixinScreenEffectRenderer {

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void onRenderFire(PoseStack poseStack, MultiBufferSource multiBufferSource, TextureAtlasSprite textureAtlasSprite, CallbackInfo ci) {
        if (Modules.enabled(NoRender.class) && Modules.get(NoRender.class).fire) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true)
    private void onTotemOverlay(PoseStack poseStack, float partialTick, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        ravex.modules.render.Animations m = Modules.get(ravex.modules.render.Animations.class);
        if (m == null || !Modules.enabled(ravex.modules.render.Animations.class)) return;
        if (!m.totemGroup || "Vanilla".equals(m.totemMode)) return;
        // own overlay draws in the Gui pass
        ci.cancel();
    }
}
