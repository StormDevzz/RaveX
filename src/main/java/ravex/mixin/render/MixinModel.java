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
import ravex.utility.render.animate.NativeAnimateVertexConsumer;

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

        boolean renderingHand = Shaders.RENDERING_HAND.get();
        if (renderingHand && Shaders.INSTANCE.hands.getValue()) {
            return new NativeAnimateVertexConsumer(consumer, Shaders.INSTANCE.fillColor.getValue(), true);
        }

        Model self = (Model)(Object)this;
        String className = self.getClass().getSimpleName().toLowerCase();

        boolean isPlayerModel = className.contains("player") || className.contains("humanoid");
        boolean isHandModel = className.contains("arm") || className.contains("hand") || className.contains("skin");

        if (isPlayerModel && Shaders.INSTANCE.players.getValue()) {
            return new NativeAnimateVertexConsumer(consumer, Shaders.INSTANCE.fillColor.getValue(), false);
        }
        if (isHandModel && Shaders.INSTANCE.hands.getValue()) {
            return new NativeAnimateVertexConsumer(consumer, Shaders.INSTANCE.fillColor.getValue(), true);
        }

        return consumer;
    }

    @Inject(
        method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
        at = @At("HEAD")
    )
    private void onRenderHead(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int tint, CallbackInfo ci) {
        if (Shaders.INSTANCE.getEnabled() && Shaders.INSTANCE.throughWalls.getValue()) {
            boolean renderingHand = Shaders.RENDERING_HAND.get();
            Model self = (Model)(Object)this;
            String className = self.getClass().getSimpleName().toLowerCase();

            boolean isPlayerModel = className.contains("player") || className.contains("humanoid");
            boolean isHandModel = className.contains("arm") || className.contains("hand") || className.contains("skin");

            if ((renderingHand && Shaders.INSTANCE.hands.getValue()) ||
                (isPlayerModel && Shaders.INSTANCE.players.getValue()) ||
                (isHandModel && Shaders.INSTANCE.hands.getValue())) {
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
            boolean renderingHand = Shaders.RENDERING_HAND.get();
            Model self = (Model)(Object)this;
            String className = self.getClass().getSimpleName().toLowerCase();

            boolean isPlayerModel = className.contains("player") || className.contains("humanoid");
            boolean isHandModel = className.contains("arm") || className.contains("hand") || className.contains("skin");

            if ((renderingHand && Shaders.INSTANCE.hands.getValue()) ||
                (isPlayerModel && Shaders.INSTANCE.players.getValue()) ||
                (isHandModel && Shaders.INSTANCE.hands.getValue())) {
                GlStateManager._enableDepthTest();
            }
        }
    }
}
