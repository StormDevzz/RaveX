package ravex.mixin.render;

import net.minecraft.client.model.Model;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.opengl.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.Shaders;
import ravex.manager.HandShaderManager;
import ravex.utility.render.animate.NativeAnimateVertexConsumer;
import ravex.utility.render.animate.ShaderVertexConsumer;

@Mixin(Model.class)
public class MixinModel {

    @ModifyVariable(
        method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private VertexConsumer modifyVertexConsumer(VertexConsumer consumer) {
        if (!Shaders.INSTANCE.getEnabled()) {
            return consumer;
        }

        if (Shaders.RENDERING_HAND.get()) {
            HandShaderManager.RenderInput ri = new HandShaderManager.RenderInput();
            ri.config = Shaders.INSTANCE.createConfig();
            ri.time = System.nanoTime() / 1e9f;
            return new ShaderVertexConsumer(consumer, ri);
        }

        Model self = (Model)(Object)this;
        String className = self.getClass().getSimpleName().toLowerCase();

        boolean isPlayerModel = className.contains("player") || className.contains("humanoid");

        if (isPlayerModel && Shaders.INSTANCE.players.getValue()) {
            return new NativeAnimateVertexConsumer(consumer, Shaders.INSTANCE.fillColor.getValue(), false);
        }

        return consumer;
    }

    @Inject(
        method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
        at = @At("HEAD")
    )
    private void onRenderHead(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int tint, CallbackInfo ci) {
        if (ravex.modules.render.Skeleton.INSTANCE.getEnabled()) {
            Model self = (Model)(Object)this;
            if (self instanceof net.minecraft.client.model.HumanoidModel) {
                net.minecraft.world.entity.LivingEntity entity = ravex.modules.render.Skeleton.getEntityBeingRendered(poseStack);
                if (entity != null) {
                    boolean isPlayer = entity instanceof net.minecraft.world.entity.player.Player;
                    boolean shouldRender = false;
                    if (isPlayer && ravex.modules.render.Skeleton.INSTANCE.players.getValue()) {
                        shouldRender = true;
                    } else if (!isPlayer && ravex.modules.render.Skeleton.INSTANCE.mobs.getValue()) {
                        shouldRender = true;
                    }
                    if (shouldRender) {
                        try {
                            net.minecraft.client.model.HumanoidModel<?> humanoidModel = (net.minecraft.client.model.HumanoidModel<?>) self;
                            int colorVal = ravex.modules.render.Skeleton.INSTANCE.color.getValue();
                            float lineWidth = ravex.modules.render.Skeleton.INSTANCE.lineWidth.getValue().floatValue();
                            boolean throughWalls = ravex.modules.render.Skeleton.INSTANCE.throughWalls.getValue();
                            ravex.modules.render.Skeleton.renderSkeleton(poseStack, humanoidModel, colorVal, lineWidth, throughWalls);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        if (Shaders.INSTANCE.getEnabled() && Shaders.INSTANCE.throughWalls.getValue()) {
            Model self = (Model)(Object)this;
            String className = self.getClass().getSimpleName().toLowerCase();

            boolean isPlayerModel = className.contains("player") || className.contains("humanoid");

            if (isPlayerModel && Shaders.INSTANCE.players.getValue()) {
                GlStateManager._disableDepthTest();
            }
        }
    }

    @Inject(
        method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
        at = @At("RETURN")
    )
    private void onRenderReturn(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int tint, CallbackInfo ci) {
        if (Shaders.INSTANCE.getEnabled() && Shaders.INSTANCE.throughWalls.getValue()) {
            Model self = (Model)(Object)this;
            String className = self.getClass().getSimpleName().toLowerCase();

            boolean isPlayerModel = className.contains("player") || className.contains("humanoid");

            if (isPlayerModel && Shaders.INSTANCE.players.getValue()) {
                GlStateManager._enableDepthTest();
            }
        }
    }
}
