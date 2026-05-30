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
        if (!ItemPhysics.INSTANCE.getEnabled()) {
            return;
        }

        // Intercept vanilla submitting to apply realistic item physics
        ci.cancel();

        if (state.item.isEmpty()) return;

        RaveXStateAccessor accessor = (RaveXStateAccessor) state;
        boolean onGround = accessor.isRavexOnGround();
        double motionY = accessor.getRavexMotionY();
        boolean isBlock = accessor.isRavexBlock();

        int seed = state.seed;
        Random random = new Random(seed);

        poseStack.pushPose();

        // Calculate dynamic physical rotation based on age and vertical motion
        double age = state.ageInTicks;
        
        float rotation = 0;
        if (!onGround) {
            // Falls down spinning, spinning speed scales with vertical velocity
            rotation = (float) (age * (12.0 + Math.abs(motionY) * 24.0));
        } else {
            // Stationary flat on ground, rotated statically based on stack seed for variety
            random.setSeed(seed);
            rotation = random.nextFloat() * 360.0F;
        }

        // Wave offset and height alignment
        float floatOffset = 0.0F;
        if (!onGround) {
            floatOffset = (float) (Math.sin(age / 8.0) * 0.04 + 0.04);
        } else {
            // Rest flat on the block surface (slightly above to prevent Z-fighting)
            floatOffset = -0.06F;
        }

        poseStack.translate(0, floatOffset + 0.1875F, 0);

        // Apply custom physical size/scale multiplier configured via the slide bar!
        float baseScale = 0.5F * ItemPhysics.INSTANCE.scale.getValue().floatValue();
        poseStack.scale(baseScale, baseScale, baseScale);

        // Realistic 3D Block vs 2D flat Item physics alignment
        if (isBlock) {
            // 3D Blocks sit flat on their base and spin cleanly
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            if (!onGround) {
                // Tumble slightly along other axes while falling
                poseStack.mulPose(Axis.XP.rotationDegrees(rotation * 0.4F));
            }
        } else {
            // 2D flat items (swords, ingots, food) lay completely flat on blocks
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F)); // tilt flat on face
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation)); // spin around its perpendicular normal axis
        }

        // Stacking item entities rendering
        int count = getRenderAmount(state.count);
        for (int i = 0; i < count; ++i) {
            poseStack.pushPose();
            if (i > 0) {
                // Stack offset
                float ox = (random.nextFloat() * 2.0F - 1.0F) * 0.1F;
                float oy = (random.nextFloat() * 2.0F - 1.0F) * 0.1F;
                float oz = (random.nextFloat() * 2.0F - 1.0F) * 0.1F;
                poseStack.translate(ox, oy, oz);
            }

            int light = state.lightCoords;
            int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
            
            // Pass the exact outlineColor state from vanilla rendering.
            // This contains the original item coloration/light tints, resolving the white item rendering bug!
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
