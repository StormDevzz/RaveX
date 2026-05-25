package volthack.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
    @Inject(
        method = "computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IF)Lorg/joml/Vector4f;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onComputeFogColor(Camera camera, float partialTicks, ClientLevel level, int renderDistance, float bossColorModifier, CallbackInfoReturnable<Vector4f> cir) {
        if (volthack.modules.render.WorldTweaks.INSTANCE.getEnabled()) {
            int col = volthack.modules.render.WorldTweaks.INSTANCE.getColor();
            float r = ((col >> 16) & 0xFF) / 255.0f;
            float g = ((col >> 8) & 0xFF) / 255.0f;
            float b = (col & 0xFF) / 255.0f;
            cir.setReturnValue(new Vector4f(r, g, b, 1.0f));
        }
    }
}
