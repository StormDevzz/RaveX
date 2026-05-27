package ravex.mixin.render;

import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.CustomFog;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
    @Inject(method = "setupFog", at = @At("RETURN"), cancellable = true)
    private void onSetupFog(Camera camera, int renderDistance, DeltaTracker deltaTracker, float bossColorModifier, ClientLevel level, CallbackInfoReturnable<org.joml.Vector4f> cir) {
        if (CustomFog.INSTANCE.getEnabled() && cir != null) {
            float rVal = (float) (CustomFog.INSTANCE.r.getValue() / 255.0f);
            float gVal = (float) (CustomFog.INSTANCE.g.getValue() / 255.0f);
            float bVal = (float) (CustomFog.INSTANCE.b.getValue() / 255.0f);
            cir.setReturnValue(new org.joml.Vector4f(rVal, gVal, bVal, 1.0f));
        }
    }
}
