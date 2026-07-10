package ravex.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ravex.modules.render.Shaders;
import ravex.manager.HandShaderManager;
import ravex.utility.render.animate.ShaderVertexConsumer;

@Mixin(ModelPart.class)
public class MixinModelPart {

    @ModifyVariable(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 1
    )
    private VertexConsumer wrapHandConsumer(VertexConsumer consumer) {
        boolean active = Shaders.maybeEnabled() && Shaders.RENDERING_HAND.get();
        if (!active) {
            return consumer;
        }
        HandShaderManager.RenderInput ri = new HandShaderManager.RenderInput();
        ri.config = Shaders.itz().createConfig();
        ri.time = System.nanoTime() / 1e9f;
        return new ShaderVertexConsumer(consumer, ri);
    }

    @ModifyVariable(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 1
    )
    private VertexConsumer wrapHandConsumer4(VertexConsumer consumer) {
        boolean active = Shaders.maybeEnabled() && Shaders.RENDERING_HAND.get();
        if (!active) {
            return consumer;
        }
        HandShaderManager.RenderInput ri = new HandShaderManager.RenderInput();
        ri.config = Shaders.itz().createConfig();
        ri.time = System.nanoTime() / 1e9f;
        return new ShaderVertexConsumer(consumer, ri);
    }
}
