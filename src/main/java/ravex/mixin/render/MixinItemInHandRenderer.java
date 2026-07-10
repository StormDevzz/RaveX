package ravex.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.player.NoSwing;
import ravex.modules.player.Swing;
import ravex.modules.render.Shaders;
import ravex.modules.render.ViewModel;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {



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



    @Inject(
        method = "applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
        at = @At("RETURN")
    )
    private void onApplyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float f, CallbackInfo ci) {
        ViewModel vm = ViewModel.itz();
        if (!vm.getEnabled()) return;

        boolean isRight = arm == HumanoidArm.RIGHT;

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

        poseStack.translate(tx, ty, tz);

        if (rx != 0f) poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rx));
        if (ry != 0f) poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(ry));
        if (rz != 0f) poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rz));

        if (scale != 1f) poseStack.scale(scale, scale, scale);
    }



    @Inject(
        method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRenderArmWithItem(AbstractClientPlayer player, float partialTick, float pitch,
                                      InteractionHand hand, float swingProgress, ItemStack stack,
                                      float equipProgress, PoseStack poseStack,
                                      SubmitNodeCollector collector, int light, CallbackInfo ci) {
        ViewModel vm = ViewModel.itz();
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



    private static AbstractClientPlayer capturedPlayer;

    @Inject(
        method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
        at = @At("HEAD")
    )
    private void capturePlayer(AbstractClientPlayer player, float partialTick, float pitch,
                                InteractionHand hand, float swingProgress, ItemStack stack,
                                float equipProgress, PoseStack poseStack,
                                SubmitNodeCollector collector, int light, CallbackInfo ci) {
        capturedPlayer = player;
    }

    @ModifyVariable(
        method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
        at = @At("HEAD"),
        ordinal = 2,
        argsOnly = true
    )
    private float modifySwingProgress(float swingProgress) {
        if (capturedPlayer == null) return swingProgress;

        boolean isSelf = Minecraft.getInstance().player == capturedPlayer;

<<<<<<< HEAD
        if (NoSwing.maybeEnabled()) {
            if (NoSwing.itz().self.getValue() && isSelf) return 1.0f;
            if (NoSwing.itz().others.getValue() && !isSelf) return 1.0f;
        }

        if (Swing.maybeEnabled() && "Custom".equals(Swing.itz().mode.getValue())) {
            float p = swingProgress;
            String path = Swing.itz().swingPath.getValue();
=======
        if (NoSwing.INSTANCE.getEnabled()) {
            if (NoSwing.INSTANCE.self.getValue() && isSelf) return 1.0f;
            if (NoSwing.INSTANCE.others.getValue() && !isSelf) return 1.0f;
        }

        if (Swing.INSTANCE.getEnabled() && "Custom".equals(Swing.INSTANCE.mode.getValue())) {
            float p = swingProgress;
            String path = Swing.INSTANCE.swingPath.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if ("Smooth".equals(path)) {
                float t = 1 - p;
                t = t * t * (3 - 2 * t);
                p = 1 - t;
            } else if ("Bounce".equals(path)) {
                float t = 1 - p;
                t = t * t * (3 - 2 * t) + 0.08f * (float) Math.sin(t * Math.PI * 4);
                t = Math.max(0, Math.min(1, t));
                p = 1 - t;
            } else if ("Reverse".equals(path)) {
                p = 1 - p;
            }
<<<<<<< HEAD
            p = (float) Math.pow(p, Swing.itz().swingCurve.getValue().floatValue());
            p = Math.min(p, Swing.itz().progressCap.getValue().floatValue());
            p = Math.max(p, Swing.itz().progressFloor.getValue().floatValue());
=======
            p = (float) Math.pow(p, Swing.INSTANCE.swingCurve.getValue().floatValue());
            p = Math.min(p, Swing.INSTANCE.progressCap.getValue().floatValue());
            p = Math.max(p, Swing.INSTANCE.progressFloor.getValue().floatValue());
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            return p;
        }

        return swingProgress;
    }

    @ModifyVariable(
        method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
        at = @At("HEAD"),
        ordinal = 3,
        argsOnly = true
    )
    private float modifyEquipProgress(float equipProgress) {
<<<<<<< HEAD
        if (Swing.maybeEnabled()
            && ("1.8".equals(Swing.itz().mode.getValue())
                || ("Custom".equals(Swing.itz().mode.getValue()) && Swing.itz().noEquip.getValue()))) {
=======
        if (Swing.INSTANCE.getEnabled()
            && ("1.8".equals(Swing.INSTANCE.mode.getValue())
                || ("Custom".equals(Swing.INSTANCE.mode.getValue()) && Swing.INSTANCE.noEquip.getValue()))) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            return 0.0f;
        }
        return equipProgress;
    }
}
