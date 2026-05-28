package ravex.mixin.client;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.FreeLook;
import ravex.modules.render.FreeCam;

@Mixin(MouseHandler.class)
public class MixinMouse {

    @Shadow @Final private Minecraft minecraft;
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void onTurnPlayer(CallbackInfo ci) {
        if (minecraft.player == null) return;

        boolean freeCam = FreeCam.INSTANCE.getEnabled();
        boolean freeLook = FreeLook.INSTANCE.getEnabled();

        if (freeCam || freeLook) {
            double sensitivity = minecraft.options.sensitivity().get() * 0.6 + 0.2;
            double e = sensitivity * sensitivity * sensitivity;
            double f = e * 8.0;
            double dx = accumulatedDX * f;
            double dy = accumulatedDY * f;

            if (minecraft.options.invertMouseY().get()) {
                dy = -dy;
            }

            if (freeCam) {
                FreeCam.INSTANCE.turn(dx, dy);
            } else {
                FreeLook.INSTANCE.turn(dx, dy);
            }

            // Consume/reset the accumulated mouse delta
            accumulatedDX = 0.0;
            accumulatedDY = 0.0;

            ci.cancel();
        }
    }
}
