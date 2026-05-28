package ravex.mixin.render;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.Shaders;
import ravex.manager.ShaderManager;

@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer {

    @Inject(method = "submit", at = @At("HEAD"))
    private void onSubmitHead(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
        if (Shaders.INSTANCE.getEnabled() && Shaders.INSTANCE.players.getValue()) {
            if (state instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState) {
                ShaderManager.INSTANCE.renderingPlayer = true;
            }
        }
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void onSubmitReturn(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
        ShaderManager.INSTANCE.renderingPlayer = false;
    }
}
