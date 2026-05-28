package ravex.mixin.render;

import net.minecraft.client.model.Model;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ravex.modules.render.Shaders;
import ravex.utility.render.animate.NativeAnimateVertexConsumer;

@Mixin(Model.class)
public class MixinModel {

    @ModifyVariable(
        method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private VertexConsumer onRenderToBuffer(VertexConsumer original) {
        if (Shaders.INSTANCE.getEnabled()) {
            String className = this.getClass().getName();
            boolean isPlayerOrHandModel = className.contains("Player") 
                    || className.contains("Humanoid") 
                    || className.contains("Armor") 
                    || className.contains("Cape") 
                    || className.contains("Elytra");

            boolean shouldApply = (Shaders.INSTANCE.players.getValue() && isPlayerOrHandModel)
                    || (Shaders.INSTANCE.hands.getValue() && className.contains("Player"));

            if (shouldApply) {
                return new NativeAnimateVertexConsumer(original, Shaders.INSTANCE.fillColor.getValue());
            }
        }
        return original;
    }

    @ModifyVariable(
        method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private VertexConsumer onRenderToBuffer2(VertexConsumer original) {
        if (Shaders.INSTANCE.getEnabled()) {
            String className = this.getClass().getName();
            boolean isPlayerOrHandModel = className.contains("Player") 
                    || className.contains("Humanoid") 
                    || className.contains("Armor") 
                    || className.contains("Cape") 
                    || className.contains("Elytra");

            boolean shouldApply = (Shaders.INSTANCE.players.getValue() && isPlayerOrHandModel)
                    || (Shaders.INSTANCE.hands.getValue() && className.contains("Player"));

            if (shouldApply) {
                return new NativeAnimateVertexConsumer(original, Shaders.INSTANCE.fillColor.getValue());
            }
        }
        return original;
    }
}
