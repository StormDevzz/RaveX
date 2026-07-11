package ravex.mixin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.NoWeb;

@Mixin(Entity.class)
public abstract class MixinNoWeb {
    @Shadow protected Vec3 stuckSpeedMultiplier;

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void onMakeStuckInBlock(net.minecraft.world.level.block.state.BlockState state, Vec3 motionMultiplier, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;

        if (!(self instanceof net.minecraft.client.player.LocalPlayer)) return;

        if (NoWeb.maybeEnabled() && state.is(net.minecraft.world.level.block.Blocks.COBWEB)) {
            String mode = NoWeb.itz().mode.getValue();
            double horizontal = 1.0;
            double vertical = 1.0;

            if (mode.equals("Custom")) {
                horizontal = NoWeb.itz().horizontalSpeed.getValue();
                vertical = NoWeb.itz().verticalSpeed.getValue();
            }

            self.fallDistance = 0.0F;

            if (mode.equals("GrimStrict")) {
                stuckSpeedMultiplier = new Vec3(0.5, 1.0, 0.5);
            } else if (horizontal >= 1.0 && vertical >= 1.0) {
                stuckSpeedMultiplier = Vec3.ZERO;
            } else {
                stuckSpeedMultiplier = new Vec3(horizontal, vertical, horizontal);
            }

            ci.cancel();
        }
    }
}
