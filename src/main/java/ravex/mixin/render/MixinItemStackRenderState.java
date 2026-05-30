package ravex.mixin.render;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.opengl.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.Shaders;

@Mixin(ItemStackRenderState.class)
public class MixinItemStackRenderState {

    @ModifyVariable(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
        at = @At("HEAD"),
        ordinal = 2,
        argsOnly = true
    )
    private int modifyTint(int tint) {
        boolean renderPlayer = Shaders.RENDERING_PLAYER.get();
        boolean renderHand = Shaders.RENDERING_HAND.get();

        if (Shaders.INSTANCE.getEnabled() && (renderPlayer || renderHand)) {
            // Apply pulsing custom color shader to player held items!
            float time = (float) ((System.currentTimeMillis() % 100000) / 1000.0);
            float pulse = (float) (Math.sin(time * 3.0f) * 0.4f + 0.6f);
            int shaderColor = Shaders.INSTANCE.fillColor.getValue();
            return Shaders.blendColors(tint, shaderColor, pulse);
        }
        return tint;
    }

    @Inject(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
        at = @At("HEAD")
    )
    private void onSubmitHead(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int tint, CallbackInfo ci) {
        boolean renderPlayer = Shaders.RENDERING_PLAYER.get();
        boolean renderHand = Shaders.RENDERING_HAND.get();

        if (Shaders.INSTANCE.getEnabled() && Shaders.INSTANCE.throughWalls.getValue() && (renderPlayer || renderHand)) {
            GlStateManager._disableDepthTest();
        }
    }

    @Inject(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
        at = @At("RETURN")
    )
    private void onSubmitReturn(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int tint, CallbackInfo ci) {
        boolean renderPlayer = Shaders.RENDERING_PLAYER.get();
        boolean renderHand = Shaders.RENDERING_HAND.get();

        if (Shaders.INSTANCE.getEnabled() && Shaders.INSTANCE.throughWalls.getValue() && (renderPlayer || renderHand)) {
            GlStateManager._enableDepthTest();
        }
    }
}
