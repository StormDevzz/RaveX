package ravex.mixin.movement;

import net.minecraft.client.player.LocalPlayer;
import ravex.mcwrapper.MinecraftWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.Flight;
import ravex.modules.Modules;

@Mixin(LocalPlayer.class)
public class MixinFlight {

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStep(CallbackInfo ci) {
        Flight flight = Modules.get(Flight.class);
        if (!Modules.enabled(Flight.class)) return;

        String mode = flight.mode;
        if ("VerusFlat".equals(mode) || "VerusDamage".equals(mode)) return;

        LocalPlayer player = (LocalPlayer)(Object)this;
        var mc = MinecraftWrapper.getInstance();

        // 1.0 = vanilla creative flight speed
        double hSpeed = flight.horSpeed * 0.5;
        double vSpeed = flight.verSpeed * 0.5;
        double glide = flight.glide;

        double[] vel = Flight.calculateVelocity(
            mode, hSpeed, vSpeed, glide,
            player.getYRot(), player.getXRot(),
            mc.options.keyJump.isDown(), mc.options.keyShift.isDown()
        );

        double nx = vel[0];
        double nz = vel[2];
        if (flight.acceleration > 1.05) {
            double blend = 1.0 - 1.0 / Math.min(5.0, flight.acceleration);
            nx = player.getDeltaMovement().x + (vel[0] - player.getDeltaMovement().x) * blend;
            nz = player.getDeltaMovement().z + (vel[2] - player.getDeltaMovement().z) * blend;
        }
        player.setDeltaMovement(nx, vel[1], nz);

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
