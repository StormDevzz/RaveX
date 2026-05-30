package ravex.mixin.render;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.Shaders;

@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer {

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
        at = @At("HEAD")
    )
    private void onSubmitHead(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
        boolean isPlayer = state.getClass().getSimpleName().contains("Player");
        if (isPlayer) {
            Shaders.RENDERING_PLAYER.set(true);
        }
    }

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
        at = @At("RETURN")
    )
    private void onSubmitReturn(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
        boolean isPlayer = state.getClass().getSimpleName().contains("Player");
        if (isPlayer) {
            Shaders.RENDERING_PLAYER.set(false);
        }
    }
}
