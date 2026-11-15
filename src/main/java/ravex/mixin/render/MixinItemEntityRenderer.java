package ravex.mixin.render;

import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.ItemPhysics;
import ravex.modules.render.NoRender;
import ravex.utility.player.InventoryUtility;
import ravex.utility.render.RaveXStateAccessorUtility;

import java.util.Random;
import ravex.modules.Modules;

@Mixin(ItemEntityRenderer.class)
public class MixinItemEntityRenderer {

    private static final java.util.Map<Integer, float[]> PHYS = new java.util.concurrent.ConcurrentHashMap<>();

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("RETURN"))
    private void onExtract(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        RaveXStateAccessorUtility accessor = (RaveXStateAccessorUtility) state;
        double motionY = entity.getDeltaMovement().y;
        boolean touching = entity.onGround() || entity.isInWater() || entity.isInLava();

        float target = 0f;
        if (entity.isInWater() || entity.isInLava()) {
            target = 1f;
        } else if (touching) {
            target = 1f;
        } else if (Math.abs(motionY) < 0.10 && entity.getAge() > 6) {
            target = (float) Math.max(0.0, Math.min(1.0, 1.0 - Math.abs(motionY) / 0.10));
        }

        int id = entity.getId();
        float airSpin = (entity.getAge() + tickDelta) * 8.0f;
        float[] p = PHYS.get(id);
        if (p == null) {
            p = new float[]{target, airSpin};
            PHYS.put(id, p);
            if (PHYS.size() > 512) {
                PHYS.clear();
                PHYS.put(id, p);
            }
        }

        float prev = p[0];
        float speed = target > prev ? 0.12f : 0.30f;
        float blend = prev + (target - prev) * speed;
        if (Math.abs(target - blend) < 0.01f) blend = target;
        if (blend < 0.001f) blend = 0f;
        if (blend > 0.999f) blend = 1f;

        if (blend < 0.05f) {
            p[1] = airSpin;
        } else if (prev < 0.05f) {
            p[1] = airSpin;
        }

        p[0] = blend;

        accessor.setRavexGroundBlend(blend);
        accessor.setRavexOnGround(blend >= 0.999f);
        accessor.setRavexMotionY(motionY);
        accessor.setRavexRestRotationX(p[1]);

        Random restRng = new Random(entity.getItem().hashCode() ^ (entity.getId() * 31L));
        accessor.setRavexRestRotation(restRng.nextFloat() * 360.0F);

        ItemStack stack = entity.getItem();
        boolean isBlock = !stack.isEmpty() && InventoryUtility.isBlockItem(stack);
        accessor.setRavexBlock(isBlock);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void onSubmit(ItemEntityRenderState state, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.CameraRenderState cameraState, CallbackInfo ci) {
        if (Modules.enabled(NoRender.class) && Modules.get(NoRender.class).items) {
            ci.cancel();
            return;
        }

        if (!Modules.enabled(ItemPhysics.class)) {
            return;
        }

        ci.cancel();

        if (state.item.isEmpty()) return;

        RaveXStateAccessorUtility accessor = (RaveXStateAccessorUtility) state;
        double motionY = accessor.getRavexMotionY();
        float groundBlend = Math.max(0f, Math.min(1f, accessor.getRavexGroundBlend()));
        boolean isBlock = accessor.isRavexBlock();
        float restRotZ = accessor.getRavexRestRotation();

        poseStack.pushPose();

        double age = state.ageInTicks;

        float airBob = (float) (Math.sin(age / 8.0) * 0.04 + 0.04);
        float groundY = isBlock ? -0.02f : -0.10f;
        float yOffset = airBob + (groundY - airBob) * groundBlend;
        poseStack.translate(0, yOffset + 0.125f, 0);

        float baseScale = 0.5F * (float) Modules.get(ItemPhysics.class).scale;
        poseStack.scale(baseScale, baseScale, baseScale);

        float t = groundBlend * groundBlend * (3.0f - 2.0f * groundBlend);
        float spinAngle = accessor.getRavexRestRotationX();
        float rest = restRotZ;

        if (isBlock) {
            float rotY = lerpAngle(spinAngle, rest, t);
            poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        } else {
            float rotZ = lerpAngle(spinAngle, restRotZ, t);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f * t));
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
        }

        int count = getRenderAmount(state.count);
        Random random = new Random(state.seed);
        for (int i = 0; i < count; ++i) {
            poseStack.pushPose();
            if (i > 0) {
                float ox = (random.nextFloat() * 2.0F - 1.0F) * 0.08F;
                float oy = (random.nextFloat() * 2.0F - 1.0F) * 0.02F;
                float oz = (random.nextFloat() * 2.0F - 1.0F) * 0.08F;
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

    private static float lerpAngle(float from, float to, float t) {
        float diff = ((to - from) % 360.0f + 540.0f) % 360.0f - 180.0f;
        return from + diff * t;
    }
}
