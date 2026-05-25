package volthack.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer {
    @Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("HEAD"))
    private void onScale(LivingEntityRenderState state, PoseStack poseStack, CallbackInfo ci) {
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void onExtractRenderState(net.minecraft.world.entity.LivingEntity entity, LivingEntityRenderState state, float f, CallbackInfo ci) {
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"))
    private void onSubmitHead(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (state.getClass().getSimpleName().contains("Player")) {
            volthack.modules.render.Chams.INSTANCE.setRenderingPlayer(true);
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("TAIL"))
    private void onSubmitTail(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        volthack.modules.render.Chams.INSTANCE.setRenderingPlayer(false);
    }
}
