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
<<<<<<< HEAD
        boolean active = Shaders.maybeEnabled() && Shaders.RENDERING_HAND.get();
        System.out.println("[RaveX-DEBUG] ModelPart.render(5) called, RENDERING_HAND=" + Shaders.RENDERING_HAND.get() + " enabled=" + Shaders.maybeEnabled() + " wrapping=" + active);
=======
        boolean active = Shaders.INSTANCE.getEnabled() && Shaders.RENDERING_HAND.get();
        System.out.println("[RaveX-DEBUG] ModelPart.render(5) called, RENDERING_HAND=" + Shaders.RENDERING_HAND.get() + " enabled=" + Shaders.INSTANCE.getEnabled() + " wrapping=" + active);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (!active) {
            return consumer;
        }
        HandShaderManager.RenderInput ri = new HandShaderManager.RenderInput();
<<<<<<< HEAD
        ri.config = Shaders.itz().createConfig();
=======
        ri.config = Shaders.INSTANCE.createConfig();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
        boolean active = Shaders.maybeEnabled() && Shaders.RENDERING_HAND.get();
        System.out.println("[RaveX-DEBUG] ModelPart.render(4) called, RENDERING_HAND=" + Shaders.RENDERING_HAND.get() + " enabled=" + Shaders.maybeEnabled() + " wrapping=" + active);
=======
        boolean active = Shaders.INSTANCE.getEnabled() && Shaders.RENDERING_HAND.get();
        System.out.println("[RaveX-DEBUG] ModelPart.render(4) called, RENDERING_HAND=" + Shaders.RENDERING_HAND.get() + " enabled=" + Shaders.INSTANCE.getEnabled() + " wrapping=" + active);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (!active) {
            return consumer;
        }
        HandShaderManager.RenderInput ri = new HandShaderManager.RenderInput();
<<<<<<< HEAD
        ri.config = Shaders.itz().createConfig();
=======
        ri.config = Shaders.INSTANCE.createConfig();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        ri.time = System.nanoTime() / 1e9f;
        return new ShaderVertexConsumer(consumer, ri);
    }
}
