package ravex.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class MixinPlayerRenderer {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL")
    )
    private void onExtractPlayerRenderState(net.minecraft.world.entity.Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            
            if (ravex.modules.render.ShiftInterp.INSTANCE.getEnabled()) {
                if (ravex.modules.render.ShiftInterp.INSTANCE.shouldCrouch(player)) {
                    state.isCrouching = true;
                }
            }

            
            if (ravex.modules.render.BabyDude.INSTANCE.getEnabled()) {
                if (ravex.modules.render.BabyDude.INSTANCE.shouldScale(player)) {
                    state.isBaby = true;
                    ravex.modules.render.BabyDude.INSTANCE.stateScaleMap.put(state, ravex.modules.render.BabyDude.INSTANCE.scale.getValue().floatValue());
                } else {
                    ravex.modules.render.BabyDude.INSTANCE.stateScaleMap.remove(state);
                }
            } else {
                ravex.modules.render.BabyDude.INSTANCE.stateScaleMap.remove(state);
            }
        }
    }

    @Inject(
        method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
        at = @At("TAIL")
    )
    private void onScalePlayer(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
        Float s = ravex.modules.render.BabyDude.INSTANCE.stateScaleMap.get(state);
        if (s != null) {
            poseStack.scale(s, s, s);
        }
    }

    @Inject(
        method = "render(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("TAIL")
    )
    private void onRenderPlayer(AvatarRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (ravex.modules.render.ChinaHat.INSTANCE.getEnabled()) {
            renderChinaHat(poseStack, bufferSource, packedLight);
        }
    }

    private void renderChinaHat(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        int color = ravex.modules.render.ChinaHat.INSTANCE.color.getValue();
        float alpha = ravex.modules.render.ChinaHat.INSTANCE.alpha.getValue().floatValue() / 255.0f;
        float radius = ravex.modules.render.ChinaHat.INSTANCE.radius.getValue().floatValue();
        float height = ravex.modules.render.ChinaHat.INSTANCE.height.getValue().floatValue();

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        poseStack.pushPose();
        poseStack.translate(0.0, -1.8, 0.0);
        poseStack.scale(radius, height, radius);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(r, g, b, (int)(alpha * 255)));

        float segments = 32;
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (2 * Math.PI * i / segments);
            float angle2 = (float) (2 * Math.PI * (i + 1) / segments);

            float x1 = (float) Math.cos(angle1);
            float z1 = (float) Math.sin(angle1);
            float x2 = (float) Math.cos(angle2);
            float z2 = (float) Math.sin(angle2);

            // Side triangles
            consumer.vertex(poseStack.last().pose(), x1, 1.0f, z1).color(r, g, b, (int)(alpha * 255)).uv(0, 0).uv2(packedLight).endVertex();
            consumer.vertex(poseStack.last().pose(), x2, 1.0f, z2).color(r, g, b, (int)(alpha * 255)).uv(0, 0).uv2(packedLight).endVertex();
            consumer.vertex(poseStack.last().pose(), 0.0f, 0.0f, 0.0f).color(r, g, b, (int)(alpha * 255)).uv(0, 0).uv2(packedLight).endVertex();

            // Bottom circle
            consumer.vertex(poseStack.last().pose(), x1, 0.0f, z1).color(r, g, b, (int)(alpha * 255)).uv(0, 0).uv2(packedLight).endVertex();
            consumer.vertex(poseStack.last().pose(), x2, 0.0f, z2).color(r, g, b, (int)(alpha * 255)).uv(0, 0).uv2(packedLight).endVertex();
            consumer.vertex(poseStack.last().pose(), 0.0f, 0.0f, 0.0f).color(r, g, b, (int)(alpha * 255)).uv(0, 0).uv2(packedLight).endVertex();
        }

        poseStack.popPose();
    }
}
