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
        return consumer;
    }

    @ModifyVariable(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 1
    )
    private VertexConsumer wrapHandConsumer4(VertexConsumer consumer) {
        return consumer;
    }
}
