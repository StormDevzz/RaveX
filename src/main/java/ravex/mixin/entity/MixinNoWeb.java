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

        if (NoWeb.INSTANCE.getEnabled() && state.is(net.minecraft.world.level.block.Blocks.COBWEB)) {
            String mode = NoWeb.INSTANCE.mode.getValue();
            double horizontal = 1.0;
            double vertical = 1.0;

            if (mode.equals("Custom")) {
                horizontal = NoWeb.INSTANCE.horizontalSpeed.getValue();
                vertical = NoWeb.INSTANCE.verticalSpeed.getValue();
            } else if (mode.equals("Positive")) {
                horizontal = 1.0;
                vertical = 1.0;
            } else if (mode.equals("Positive2")) {
                horizontal = 0.6;
                vertical = 0.5;
            } else if (mode.equals("Positive3")) {
                horizontal = 0.4;
                vertical = 0.3;
            }

            self.fallDistance = 0.0F;

            
            
            if (horizontal >= 1.0 && vertical >= 1.0) {
                stuckSpeedMultiplier = Vec3.ZERO;
            } else {
                stuckSpeedMultiplier = new Vec3(horizontal, vertical, horizontal);
            }

            ci.cancel();
        }
    }
}
