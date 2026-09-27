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
import ravex.utility.render.animate.NativeAnimateVertexConsumerUtility;
import ravex.utility.render.animate.ShaderVertexConsumerUtility;
import ravex.modules.Modules;
import ravex.modules.render.Skeleton;

@Mixin(Model.class)
public class MixinModel {

    @ModifyVariable(
        method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private VertexConsumer modifyVertexConsumer(VertexConsumer consumer) {
        if (!Modules.enabled(Shaders.class)) {
            return consumer;
        }

        if (Shaders.RENDERING_HAND.get()) {
            return consumer;
        }

        Model self = (Model)(Object)this;
        String className = self.getClass().getSimpleName().toLowerCase();

        boolean isPlayerModel = className.contains("player") || className.contains("humanoid");

        if (isPlayerModel && Modules.get(Shaders.class).players) {
            return new NativeAnimateVertexConsumerUtility(consumer, Modules.get(Shaders.class).fillColor, false);
        }

        return consumer;
    }

    @Inject(
        method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
        at = @At("HEAD")
    )
    private void onRenderHead(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int tint, CallbackInfo ci) {
        if (Modules.enabled(Shaders.class) && Modules.get(Shaders.class).throughWalls) {
            Model self = (Model)(Object)this;
            String className = self.getClass().getSimpleName().toLowerCase();

            boolean isPlayerModel = className.contains("player") || className.contains("humanoid");

            if (isPlayerModel && Modules.get(Shaders.class).players) {
                GlStateManager._disableDepthTest();
            }
        }
    }

    @Inject(
        method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
        at = @At("RETURN")
    )
    private void onRenderReturn(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int tint, CallbackInfo ci) {
        if (Modules.enabled(Skeleton.class)) {
            Model self = (Model)(Object)this;
            try {
                int colorVal = Modules.get(Skeleton.class).color;
                float lineWidth = (float) Modules.get(Skeleton.class).lineWidth;
                boolean throughWalls = Modules.get(Skeleton.class).throughWalls;
                if (self instanceof net.minecraft.client.model.HumanoidModel) {
                    net.minecraft.world.entity.LivingEntity entity = ravex.modules.render.Skeleton.getEntityBeingRendered(poseStack);
                    if (entity != null && ravex.modules.render.Skeleton.shouldRender(entity)) {
                        net.minecraft.client.model.HumanoidModel<?> humanoidModel = (net.minecraft.client.model.HumanoidModel<?>) self;
                        boolean dick = Modules.get(Skeleton.class).dick;
                        boolean glow = Modules.get(Skeleton.class).glow;
                        ravex.modules.render.Skeleton.drawHumanoid(poseStack, humanoidModel, entity.getId(), colorVal, lineWidth, throughWalls, dick, glow);
                    }
                }
            } catch (Exception ignored) {}
        }

        if (Modules.enabled(Shaders.class) && Modules.get(Shaders.class).throughWalls) {
            Model self = (Model)(Object)this;
            String className = self.getClass().getSimpleName().toLowerCase();

            boolean isPlayerModel = className.contains("player") || className.contains("humanoid");

            if (isPlayerModel && Modules.get(Shaders.class).players) {
                GlStateManager._enableDepthTest();
            }
        }
    }
}
