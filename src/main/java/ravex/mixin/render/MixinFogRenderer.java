package ravex.mixin.render;

import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.CustomFog;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
    /**
     * Inject into the PRIVATE computeFogColor method which is called BEFORE updateBuffer writes
     * the color to the GPU UBO. Changing the return value here means the correct color goes
     * into the UBO and is actually visible.
     * 
     * Signature: private Vector4f computeFogColor(Camera, float, ClientLevel, int, float)
     */
    @Inject(method = "computeFogColor", at = @At("RETURN"), cancellable = true)
    private void onComputeFogColor(Camera camera, float partialTick, ClientLevel level,
                                   int renderDistance, float bossColorModifier,
                                   CallbackInfoReturnable<Vector4f> cir) {
        if (ravex.modules.player.Xray.INSTANCE.getEnabled()) {
            cir.setReturnValue(new Vector4f(0.0f, 0.0f, 0.0f, 0.0f));
            return;
        }

        if (!CustomFog.INSTANCE.getEnabled()) return;

        int argb = CustomFog.INSTANCE.color.getValue();
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float g = ((argb >>  8) & 0xFF) / 255.0f;
        float b = ( argb        & 0xFF) / 255.0f;

        cir.setReturnValue(new Vector4f(r, g, b, 1.0f));
    }
}
