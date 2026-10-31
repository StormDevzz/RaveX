package ravex.mixin.render;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.Shaders;
import ravex.modules.render.NameTags;
import ravex.modules.render.ShiftInterp;
import ravex.modules.render.Skeleton;
import ravex.modules.Modules;

@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer {

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void onShouldShowName(net.minecraft.world.entity.LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {

        if (Modules.enabled(NameTags.class)) {
            cir.setReturnValue(false);
        }
    }

    // logout statues: tinted skin, plain model
    @Inject(method = "getModelTint(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)I", at = @At("RETURN"), cancellable = true)
    private void ravex$logoutSpotTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        if (((ravex.utility.render.CorpseRenderStateUtility) (Object) state).isRavexLogoutSpot()) {
            cir.setReturnValue(ravex.modules.render.LogoutSpots.statueTint());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("RETURN"), cancellable = true)
    private void ravex$logoutSpotRenderType(LivingEntityRenderState state, boolean flag1, boolean flag2, boolean flag3, CallbackInfoReturnable<net.minecraft.client.renderer.rendertype.RenderType> cir) {
        if (cir.getReturnValue() == null) return;
        if (!((ravex.utility.render.CorpseRenderStateUtility) (Object) state).isRavexLogoutSpot()) return;
        net.minecraft.resources.Identifier texture =
            ((net.minecraft.client.renderer.entity.LivingEntityRenderer) (Object) this).getTextureLocation(state);
        cir.setReturnValue(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture));
    }

    @Inject(method = "setupRotations", at = @At("HEAD"), cancellable = true)
    private void onSetupRotations(LivingEntityRenderState state, PoseStack poseStack, float bob, float yBodyRot, CallbackInfo ci) {
        if (!((ravex.utility.render.CorpseRenderStateUtility) (Object) state).isRavexCorpse()) return;
        // corpse lies on its back: rotate X, not vanilla Z
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - yBodyRot));
        if (state.deathTime > 0) {
            float f = (state.deathTime - 1.0F) / 20.0F * 1.6F;
            f = net.minecraft.util.Mth.sqrt(Math.max(0.0F, f));
            if (f > 1.0F) f = 1.0F;
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F * f));
        }
        ci.cancel();
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
        at = @At("TAIL")
    )
    private void onExtractRenderState(net.minecraft.world.entity.LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        // corpse copies: no red overlay, limbs read from the state
        // corpses -10999..-10000, logout statues -20000
        boolean logoutSpot = entity.getId() <= -20000;
        boolean corpse = entity.getId() <= -10000 && entity.getId() > -20000;
        ravex.utility.render.CorpseRenderStateUtility cs =
            (ravex.utility.render.CorpseRenderStateUtility) (Object) state;
        cs.setRavexCorpse(corpse);
        cs.setRavexLogoutSpot(logoutSpot);
        float[] limbs = corpse ? ravex.modules.render.Animations.corpseLimbs(entity.getId()) : null;
        cs.setRavexLimbSwing(limbs != null ? limbs[1] : 0f);
        cs.setRavexLimbPhase(limbs != null ? limbs[0] : 0f);
        if (corpse) {
            state.hasRedOverlay = false;
            // kill vanilla walk pose, corpse limbs use physics
            state.walkAnimationPos = 0f;
            state.walkAnimationSpeed = 0f;
        }
        if (logoutSpot) {
            // statue stands still, no red flash, own label
            state.hasRedOverlay = false;
            state.walkAnimationPos = 0f;
            state.walkAnimationSpeed = 0f;
            state.nameTag = null;
        }
        // player scale from the Animations module
        if (entity instanceof net.minecraft.world.entity.player.Player player
                && Modules.enabled(ravex.modules.render.Animations.class)) {
            ravex.modules.render.Animations an = Modules.get(ravex.modules.render.Animations.class);
            if (an != null && an.playerScale && Math.abs(an.playerScaleValue - 1.0) > 0.001) {
                boolean self = ravex.utility.player.PlayerUtility.getPlayer() == player;
                if (!an.scaleSelfOnly || self) {
                    state.scale *= (float) an.playerScaleValue;
                }
            }
        }
        // autoMend: throw visible on own body
        if (entity == ravex.utility.player.PlayerUtility.getPlayer()) {
            float mendPitch = ravex.modules.player.AutoMend.visiblePitch;
            if (!Float.isNaN(mendPitch)) {
                state.xRot = mendPitch;
            }
        }
        if (!Modules.enabled(ravex.modules.player.AntiAim.class)) return;
        if (entity != ravex.utility.player.PlayerUtility.getPlayer()) return;
        ravex.modules.player.AntiAim aa = Modules.get(ravex.modules.player.AntiAim.class);
        if (aa == null || !aa.silent) return;
        state.xRot = ravex.modules.player.AntiAim.getSilentPitch();
    }

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
        at = @At("HEAD")
    )
    private void onSubmitHead(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo ci) {
        boolean isPlayer = state.getClass().getSimpleName().contains("Player");
        if (isPlayer) {
            Shaders.RENDERING_PLAYER.set(true);
        }
        if (!(state instanceof HumanoidRenderState humanoid)) return;
        if (!Modules.enabled(ShiftInterp.class)) return;
        net.minecraft.world.entity.LivingEntity entity = Skeleton.getEntityBeingRendered(poseStack);
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) return;
        ShiftInterp si = Modules.get(ShiftInterp.class);
        if (si != null && si.shouldCrouch(player)) humanoid.isCrouching = true;
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
