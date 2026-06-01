package ravex.mixin.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {
    @Accessor("pose")
    Matrix3x2fStack getPose();

    @Invoker("innerBlit")
    void invokeInnerBlit(RenderPipeline pipeline, Identifier texture, int x1, int y1, int x2, int y2, float u1, float v1, float u2, float v2, int color);

    @Invoker("submitBlit")
    void invokeSubmitBlit(RenderPipeline pipeline, GpuTextureView textureView, GpuSampler sampler, int x, int y, int width, int height, float uMin, float vMin, float uMax, float vMax, int color);
}
