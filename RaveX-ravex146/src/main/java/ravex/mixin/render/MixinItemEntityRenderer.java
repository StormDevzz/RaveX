package ravex.mixin.render;

import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.ItemPhysics;
import ravex.utility.render.RaveXStateAccessor;

import java.util.Random;

@Mixin(ItemEntityRenderer.class)
public class MixinItemEntityRenderer {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("RETURN"))
    private void onExtract(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        RaveXStateAccessor accessor = (RaveXStateAccessor) state;
        accessor.setRavexOnGround(entity.onGround() || entity.isInWater() || entity.isInLava());
        accessor.setRavexMotionY(entity.getDeltaMovement().y);

        ItemStack stack = entity.getItem();
        boolean isBlock = !stack.isEmpty() && stack.getItem() instanceof BlockItem;
        accessor.setRavexBlock(isBlock);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void onSubmit(ItemEntityRenderState state, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.CameraRenderState cameraState, CallbackInfo ci) {
        if (ravex.modules.render.NoRender.INSTANCE.getEnabled() && ravex.modules.render.NoRender.INSTANCE.items.getValue()) {
            ci.cancel();
            return;
        }

        if (!ItemPhysics.INSTANCE.getEnabled()) {
            return;
        }


        ci.cancel();

        if (state.item.isEmpty()) return;

        RaveXStateAccessor accessor = (RaveXStateAccessor) state;
        boolean onGround = accessor.isRavexOnGround();
        double motionY = accessor.getRavexMotionY();
        boolean isBlock = accessor.isRavexBlock();

        int seed = state.seed;
        Random random = new Random(seed);

        poseStack.pushPose();


        double age = state.ageInTicks;

        float rotation = 0;
        if (!onGround) {

            rotation = (float) (age * (12.0 + Math.abs(motionY) * 24.0));
        } else {

            random.setSeed(seed);
            rotation = random.nextFloat() * 360.0F;
        }


        float floatOffset = 0.0F;
        if (!onGround) {
            floatOffset = (float) (Math.sin(age / 8.0) * 0.04 + 0.04);
        } else {

            floatOffset = -0.06F;
        }

        poseStack.translate(0, floatOffset + 0.1875F, 0);


        float baseScale = 0.5F * ItemPhysics.INSTANCE.scale.getValue().floatValue();
        poseStack.scale(baseScale, baseScale, baseScale);


        if (isBlock) {

            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            if (!onGround) {

                poseStack.mulPose(Axis.XP.rotationDegrees(rotation * 0.4F));
            }
        } else {

            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        }


        int count = getRenderAmount(state.count);
        for (int i = 0; i < count; ++i) {
            poseStack.pushPose();
            if (i > 0) {

                float ox = (random.nextFloat() * 2.0F - 1.0F) * 0.1F;
                float oy = (random.nextFloat() * 2.0F - 1.0F) * 0.1F;
                float oz = (random.nextFloat() * 2.0F - 1.0F) * 0.1F;
                poseStack.translate(ox, oy, oz);
            }

            int light = state.lightCoords;
            int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;



            int tint = state.outlineColor;

            state.item.submit(poseStack, collector, light, overlay, tint);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private int getRenderAmount(int count) {
        if (count > 48) return 5;
        if (count > 32) return 4;
        if (count > 16) return 3;
        if (count > 1) return 2;
        return 1;
    }
}
