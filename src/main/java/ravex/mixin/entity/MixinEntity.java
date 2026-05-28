package ravex.mixin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.ESP;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Inject(method = "getBlockSpeedFactor", at = @At("RETURN"), cancellable = true)
    private void onGetBlockSpeedFactor(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof net.minecraft.client.player.LocalPlayer)) return;

        if (ravex.modules.movement.NoSlowDown.INSTANCE.getEnabled() && ravex.modules.movement.NoSlowDown.INSTANCE.blocks.getValue()) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "getBlockJumpFactor", at = @At("RETURN"), cancellable = true)
    private void onGetBlockJumpFactor(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof net.minecraft.client.player.LocalPlayer)) return;

        if (ravex.modules.movement.NoSlowDown.INSTANCE.getEnabled() && ravex.modules.movement.NoSlowDown.INSTANCE.blocks.getValue()) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void onMakeStuckInBlock(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.phys.Vec3 motionMultiplier, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof net.minecraft.client.player.LocalPlayer)) return;

        if (ravex.modules.movement.NoSlowDown.INSTANCE.getEnabled() && ravex.modules.movement.NoSlowDown.INSTANCE.cobwebs.getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColor(CallbackInfoReturnable<Integer> cir) {
        if (ESP.INSTANCE.getEnabled() && ESP.INSTANCE.mode.getValue().equals("Outline")) {
            Entity self = (Entity) (Object) this;
            if (self instanceof LivingEntity) {
                if (self instanceof Player) {
                    cir.setReturnValue(ESP.INSTANCE.playerColor.getValue());
                } else if (self instanceof Monster) {
                    cir.setReturnValue(ESP.INSTANCE.mobColor.getValue());
                }
            }
        }
    }

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void onTurn(double yRot, double xRot, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof net.minecraft.client.player.LocalPlayer)) return;

        if (ravex.modules.render.FreeCam.INSTANCE.getEnabled()) {
            ravex.modules.render.FreeCam.INSTANCE.turn(yRot, xRot);
            ci.cancel();
        } else if (ravex.modules.render.FreeLook.INSTANCE.getEnabled()) {
            ravex.modules.render.FreeLook.INSTANCE.turn(yRot, xRot);
            ci.cancel();
        }
    }
}
