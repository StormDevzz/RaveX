package ravex.mixin.render;

import net.minecraft.client.renderer.texture.AbstractTexture;
import com.mojang.blaze3d.textures.GpuSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractTexture.class)
public interface AccessorAbstractTexture {
    @Accessor("sampler")
    void setSampler(GpuSampler sampler);

    @Accessor("sampler")
    GpuSampler getSampler();
}
