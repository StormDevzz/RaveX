package ravex.integrations.sodium;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import ravex.modules.render.NoRender;
import ravex.modules.Modules;

@Mixin(SodiumWorldRenderer.class)
public abstract class SodiumWorldRendererMixin {

    @ModifyArgs(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;update(Lnet/minecraft/client/Camera;Lnet/caffeinemc/mods/sodium/client/render/viewport/Viewport;Lnet/caffeinemc/mods/sodium/client/util/FogParameters;Z)V"))
    private void ravex$modifyFogParameters(Args args) {
        if (Modules.enabled(NoRender.class) && Modules.get(NoRender.class).fog) {
            args.set(2, FogParameters.NONE);
        }
    }
}