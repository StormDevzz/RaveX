package ravex.mixin.render;

import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import ravex.modules.render.NoRender;
import ravex.modules.render.CustomFog;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {

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

    @ModifyArgs(
        method = "setupFog",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V")
    )
    private void onUpdateBufferArgs(Args args) {
        if (NoRender.INSTANCE.getEnabled() && NoRender.INSTANCE.fog.getValue()) {
            float envStart = args.get(3);
            float envEnd = args.get(4);
            float rdStart = args.get(5);
            float rdEnd = args.get(6);
            float skyEnd = args.get(7);
            float cloudEnd = args.get(8);

            float[] optimized = NoRender.optimizeFog(envStart, envEnd, rdStart, rdEnd, skyEnd, cloudEnd);
            args.set(3, optimized[0]);
            args.set(4, optimized[1]);
            args.set(5, optimized[2]);
            args.set(6, optimized[3]);
            args.set(7, optimized[4]);
            args.set(8, optimized[5]);
        }
    }
}
