package ravex.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.Shaders;
import ravex.modules.render.ViewModel;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {

    // ── Existing Shaders hooks ───────────────────────────────────────────────

    @Inject(
        method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
        at = @At("HEAD")
    )
    private void onRenderHead(float f, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player, int i, CallbackInfo ci) {
        Shaders.RENDERING_HAND.set(true);
    }

    @Inject(
        method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
        at = @At("RETURN")
    )
    private void onRenderReturn(float f, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player, int i, CallbackInfo ci) {
        Shaders.RENDERING_HAND.set(false);
    }

    // ── ViewModel: applyItemArmTransform ─────────────────────────────────────
    // This method sets up the base PoseStack transform for the arm/item at rest.
    // We inject at TAIL to apply our extra translation+rotation+scale on top.

    @Inject(
        method = "applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
        at = @At("RETURN")
    )
    private void onApplyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float f, CallbackInfo ci) {
        ViewModel vm = ViewModel.INSTANCE;
        if (!vm.getEnabled()) return;

        boolean isRight = arm == HumanoidArm.RIGHT;

        // Determine if right arm = main hand (default for right-handed player)
        // We apply per-hand transforms
        float tx, ty, tz, rx, ry, rz, scale;

        if (isRight) {
            tx    = vm.mainX.getValue().floatValue();
            ty    = vm.mainY.getValue().floatValue();
            tz    = vm.mainZ.getValue().floatValue();
            rx    = vm.mainRotX.getValue().floatValue();
            ry    = vm.mainRotY.getValue().floatValue();
            rz    = vm.mainRotZ.getValue().floatValue();
            scale = vm.mainScale.getValue().floatValue();
        } else {
            tx    = vm.offX.getValue().floatValue();
            ty    = vm.offY.getValue().floatValue();
            tz    = vm.offZ.getValue().floatValue();
            rx    = vm.offRotX.getValue().floatValue();
            ry    = vm.offRotY.getValue().floatValue();
            rz    = vm.offRotZ.getValue().floatValue();
            scale = vm.offScale.getValue().floatValue();
        }

        // Apply translation
        poseStack.translate(tx, ty, tz);

        // Apply rotations (in degrees)
        if (rx != 0f) poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rx));
        if (ry != 0f) poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(ry));
        if (rz != 0f) poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rz));

        // Apply scale
        if (scale != 1f) poseStack.scale(scale, scale, scale);
    }

    // ── ViewModel: renderArmWithItem — Hide hand / swing speed ───────────────

    @Inject(
        method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRenderArmWithItem(AbstractClientPlayer player, float partialTick, float pitch,
                                      InteractionHand hand, float swingProgress, ItemStack stack,
                                      float equipProgress, PoseStack poseStack,
                                      SubmitNodeCollector collector, int light, CallbackInfo ci) {
        ViewModel vm = ViewModel.INSTANCE;
        if (!vm.getEnabled()) return;

        boolean isMainHand = hand == InteractionHand.MAIN_HAND;

        if (isMainHand && vm.hideMainHand.getValue()) {
            ci.cancel();
            return;
        }
        if (!isMainHand && vm.hideOffHand.getValue()) {
            ci.cancel();
            return;
        }
    }
}
