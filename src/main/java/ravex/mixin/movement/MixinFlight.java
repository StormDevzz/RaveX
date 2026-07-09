package ravex.mixin.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.Flight;

@Mixin(LocalPlayer.class)
public class MixinFlight {

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStep(CallbackInfo ci) {
<<<<<<< HEAD
        Flight flight = Flight.itz();
=======
        Flight flight = Flight.INSTANCE;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (!flight.getEnabled()) return;

        LocalPlayer player = (LocalPlayer)(Object)this;
        Minecraft mc = Minecraft.getInstance();

        String mode = flight.mode.getValue();
        double hSpeed = flight.speed.getValue();
        double vSpeed = flight.verticalSpeed.getValue();
        double glide = flight.glide.getValue();

        double[] vel = Flight.calculateVelocity(
            mode, hSpeed, vSpeed, glide,
            player.getYRot(), player.getXRot(),
            mc.options.keyJump.isDown(), mc.options.keyShift.isDown()
        );

        double currentFriction = Flight.handleAirFriction(
            mode,
            Math.sqrt(player.getDeltaMovement().x * player.getDeltaMovement().x + player.getDeltaMovement().z * player.getDeltaMovement().z),
            flight.acceleration.getValue().doubleValue(),
            0.5
        );

        player.setDeltaMovement(vel[0] * currentFriction, vel[1], vel[2] * currentFriction);

        if ("Creative".equals(mode) || "Vanilla".equals(mode)) {
            player.getAbilities().flying = true;
        }

        if (flight.timer.getValue() != 1.0) {
            float t = flight.timer.getValue().floatValue();
            player.setDeltaMovement(
                player.getDeltaMovement().x * t,
                player.getDeltaMovement().y,
                player.getDeltaMovement().z * t
            );
        }
    }
}
