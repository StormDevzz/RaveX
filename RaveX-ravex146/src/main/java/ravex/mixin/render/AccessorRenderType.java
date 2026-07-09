package ravex.mixin.render;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderType.class)
public interface AccessorRenderType {
    @Invoker("create")
    static RenderType invokeCreate(String name, RenderSetup setup) {
        throw new AssertionError();
    }
}
