package ravex.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.player.NoSwing;
import ravex.modules.render.Shaders;
import ravex.modules.render.SwingAnimation;
import ravex.modules.render.ViewModel;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {

    @Shadow private float mainHandHeight;
    @Shadow private float oMainHandHeight;
    @Shadow private float offHandHeight;
    @Shadow private float oOffHandHeight;
    @Shadow private ItemStack mainHandItem;
    @Shadow private ItemStack offHandItem;

    @Shadow
    protected abstract void renderItem(LivingEntity livingEntity, ItemStack itemStack,
                                       ItemDisplayContext itemDisplayContext, PoseStack poseStack,
                                       SubmitNodeCollector submitNodeCollector, int light);

    private void applyViewModel(PoseStack poseStack, HumanoidArm arm) {
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
        if (rx != 0f) poseStack.mulPose(Axis.XP.rotationDegrees(rx));
        if (ry != 0f) poseStack.mulPose(Axis.YP.rotationDegrees(ry));
        if (rz != 0f) poseStack.mulPose(Axis.ZP.rotationDegrees(rz));
        if (scale != 1f) poseStack.scale(scale, scale, scale);
    }

    // ===== Shaders =====

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

    // ===== Captures =====

    private static AbstractClientPlayer capturedPlayer;
    private static InteractionHand capturedHand;
    private static float capturedSwingProgress = 0f;
    private static float peakSwingProgress = 0f;

    // --- Capture player (runs first by local var index) ---
    @ModifyVariable(
        method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private AbstractClientPlayer capturePlayerVariable(AbstractClientPlayer player) {
        capturedPlayer = player;
        return player;
    }

    // --- Capture hand (runs second by local var index, BEFORE swingProgress) ---
    @ModifyVariable(
        method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private InteractionHand ravex$captureSwingHand(InteractionHand hand) {
        capturedHand = hand;
        return hand;
    }

    // --- Modify swingProgress (runs third by local var index, AFTER hand is captured) ---
    @ModifyVariable(
        method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
        at = @At("HEAD"),
        ordinal = 2,
        argsOnly = true
    )
    private float modifySwingProgress(float swingProgress) {
        if (capturedPlayer == null) {
            capturedSwingProgress = swingProgress;
            return swingProgress;
        }

        boolean isSelf = Minecraft.getInstance().player == capturedPlayer;

        if (NoSwing.maybeEnabled()) {
            capturedSwingProgress = swingProgress;
            if (NoSwing.itz().self.getValue() && isSelf) return 1.0f;
            if (NoSwing.itz().others.getValue() && !isSelf) return 1.0f;
            return swingProgress;
        }


        peakSwingProgress = 0f;
        capturedSwingProgress = swingProgress;
        return swingProgress;
    }

    // --- Modify equipProgress for Swipe ---
    @ModifyVariable(
        method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
        at = @At("HEAD"),
        ordinal = 3,
        argsOnly = true
    )
    private float modifyEquipProgress(float equipProgress) {
        if (!SwingAnimation.maybeEnabled()) return equipProgress;
        if ("Swipe".equals(SwingAnimation.itz().mode.getValue())) return 0.0f;
        return equipProgress;
    }

    // ===== ViewModel: applyItemArmTransform RETURN =====

    @Inject(
        method = "applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
        at = @At("RETURN")
    )
    private void onApplyItemArmTransformReturn(PoseStack poseStack, HumanoidArm arm, float f, CallbackInfo ci) {
        if (SwingAnimation.maybeEnabled() && !NoSwing.maybeEnabled()) {
            String mode = SwingAnimation.itz().mode.getValue();
            if ("Default".equals(mode) || "Akrien".equals(mode) || "Swipe".equals(mode)) return;
        }
        applyViewModel(poseStack, arm);
    }

    // ===== Main: cancel renderArmWithItem for all SwingAnimation modes =====

    @Inject(
        method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRenderArmWithItem(AbstractClientPlayer player, float partialTick, float pitch,
                                      InteractionHand hand, float swingProgress, ItemStack stack,
                                      float equipProgress, PoseStack poseStack,
                                      SubmitNodeCollector collector, int light, CallbackInfo ci) {
        capturedPlayer = player;

        ViewModel vm = ViewModel.itz();
        if (vm.getEnabled()) {
            if (hand == InteractionHand.MAIN_HAND && vm.hideMainHand.getValue()) {
                ci.cancel();
                return;
            }
            if (hand == InteractionHand.OFF_HAND && vm.hideOffHand.getValue()) {
                ci.cancel();
                return;
            }
        }

        if (!SwingAnimation.maybeEnabled() || NoSwing.maybeEnabled() || stack.isEmpty()) return;
        if (hand != InteractionHand.MAIN_HAND) return;

        ci.cancel();

        String mode = SwingAnimation.itz().mode.getValue();
        HumanoidArm arm = player.getMainArm();
        boolean rightHand = arm == HumanoidArm.RIGHT;

        poseStack.pushPose();

        float ep = (capturedSwingProgress > 0f) ? 0f : equipProgress;
        switch (mode) {
            case "Default" -> SwingAnimation.itz().applyDefault(poseStack, capturedSwingProgress, ep, rightHand);
            case "Akrien"  -> SwingAnimation.itz().applyFourteen(poseStack, capturedSwingProgress, ep);
            case "Swipe"   -> SwingAnimation.itz().applySwipe(poseStack, capturedSwingProgress, ep);
        }

        applyViewModel(poseStack, arm);

        ItemDisplayContext context = rightHand
            ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;

        renderItem(player, stack, context, poseStack, collector, light);

        poseStack.popPose();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        if (SwingAnimation.maybeEnabled()) {
            String mode = SwingAnimation.itz().mode.getValue();
            if ("Default".equals(mode) || "Akrien".equals(mode)) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    if (mainHandItem != null && mainHandItem.getItem() == player.getMainHandItem().getItem()) {
                        mainHandHeight = 1.0f;
                        oMainHandHeight = 1.0f;
                        mainHandItem = player.getMainHandItem();
                    }
                    if (offHandItem != null && offHandItem.getItem() == player.getOffhandItem().getItem()) {
                        offHandHeight = 1.0f;
                        oOffHandHeight = 1.0f;
                        offHandItem = player.getOffhandItem();
                    }
                }
            }
        }
    }
}
