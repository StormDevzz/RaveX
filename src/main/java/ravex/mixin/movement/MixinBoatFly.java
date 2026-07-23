package ravex.mixin.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.BoatFly;

@Mixin(LivingEntity.class)
public abstract class MixinBoatFly {

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravel(Vec3 travelVector, CallbackInfo ci) {
        if (!BoatFly.maybeEnabled()) return;
        LivingEntity entity = (LivingEntity)(Object)this;
        if (!(entity instanceof LocalPlayer player)) return;
        if (player.getVehicle() == null) return;
        if (!(player.getVehicle() instanceof Boat)) return;
        ci.cancel();
    }
}
