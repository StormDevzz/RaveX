package ravex.mixin.render;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.utility.render.CorpseRenderStateUtility;

@Mixin(PlayerModel.class)
public class MixinPlayerModel {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void ravexCorpsePose(AvatarRenderState state, CallbackInfo ci) {
        ravex$poseCorpse(state);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void ravexCorpsePoseHumanoid(HumanoidRenderState state, CallbackInfo ci) {
        ravex$poseCorpse(state);
    }

    // corpse limbs rest straight, only physics impulses move them
    @Unique
    private static long ravexLastPoseLog = 0L;

    @Unique
    private void ravex$poseCorpse(HumanoidRenderState state) {
        CorpseRenderStateUtility cs = (CorpseRenderStateUtility) (Object) state;
        if (!cs.isRavexCorpse()) return;
        net.minecraft.client.model.HumanoidModel<?> model =
            (net.minecraft.client.model.HumanoidModel<?>) (Object) this;
        float swing = Math.min(1.0f, cs.ravexLimbSwing() * 0.55f);
        float s = (float) Math.sin(cs.ravexLimbPhase() * 0.6f) * swing;
        model.leftArm.xRot = s * 0.55f;
        model.rightArm.xRot = -s * 0.55f;
        model.leftArm.zRot = 0.06f;
        model.rightArm.zRot = -0.06f;
        model.leftLeg.xRot = -s * 0.4f;
        model.rightLeg.xRot = s * 0.4f;
        model.head.xRot = -s * 0.3f;
        long nowMs = System.currentTimeMillis();
        if (nowMs - ravexLastPoseLog > 1000L) {
            ravexLastPoseLog = nowMs;
            ravex.RaveX.LOGGER.info("[DeathPhysics] corpse pose applied, swing {} phase {}",
                cs.ravexLimbSwing(), cs.ravexLimbPhase());
        }
    }
}
