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
        Flight flight = Flight.itz();
        if (!Flight.maybeEnabled()) return;

        LocalPlayer player = (LocalPlayer)(Object)this;
        Minecraft mc = Minecraft.getInstance();

        String mode = flight.mode;
        double hSpeed = flight.speed;
        double vSpeed = flight.verticalSpeed;
        double glide = flight.glide;

        double[] vel = Flight.calculateVelocity(
            mode, hSpeed, vSpeed, glide,
            player.getYRot(), player.getXRot(),
            mc.options.keyJump.isDown(), mc.options.keyShift.isDown()
        );

        double currentFriction = Flight.handleAirFriction(
            mode,
            Math.sqrt(player.getDeltaMovement().x * player.getDeltaMovement().x + player.getDeltaMovement().z * player.getDeltaMovement().z),
            flight.acceleration,
            0.5
        );

        player.setDeltaMovement(vel[0] * currentFriction, vel[1], vel[2] * currentFriction);

        if ("Creative".equals(mode) || "Vanilla".equals(mode)) {
            player.getAbilities().flying = true;
        }

        if (flight.timer != 1.0) {
            float t = (float) flight.timer;
            player.setDeltaMovement(
                player.getDeltaMovement().x * t,
                player.getDeltaMovement().y,
                player.getDeltaMovement().z * t
            );
        }
    }
}
