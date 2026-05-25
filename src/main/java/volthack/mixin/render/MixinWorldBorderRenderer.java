package volthack.mixin.render;

import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.state.WorldBorderRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldBorderRenderer.class)
public class MixinWorldBorderRenderer {
    @Inject(
        method = "render",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRender(WorldBorderRenderState state, Vec3 vec, double d, double e, CallbackInfo ci) {
        if (volthack.modules.render.NoRender.INSTANCE.getEnabled() && volthack.modules.render.NoRender.INSTANCE.getWorldBorder()) {
            ci.cancel();
        }
    }
}
