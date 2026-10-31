package ravex.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.Modules;
import ravex.modules.render.Animations;

@Mixin(EndCrystalRenderer.class)
public class MixinEndCrystalRenderer {

    @Inject(method = "submit", at = @At("HEAD"))
    private void onCrystalSubmit(EndCrystalRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
        if (!Modules.enabled(Animations.class)) return;
        Animations an = Modules.get(Animations.class);
        if (an == null || !an.crystalGroup) return;
        if (an.crystalStatic) {
            // spin and bob read this clock
            state.ageInTicks = 0f;
        }
        float s = (float) an.crystalSize;
        if (Math.abs(s - 1.0f) > 0.001f) poseStack.scale(s, s, s);
        if (an.crystalStatic) return;
        double t = System.nanoTime() / 1_000_000_000.0;
        float yaw = (float) (t * 90.0 * an.crystalSpinH);
        float pitch = (float) (t * 90.0 * an.crystalSpinV);
        if (yaw != 0f) poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        if (pitch != 0f) poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }
}
