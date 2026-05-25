package volthack.mixin.entity;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void onTurn(double yRot, double xRot, CallbackInfo ci) {
        if ((Object) this == net.minecraft.client.Minecraft.getInstance().player) {
            if (volthack.modules.render.FreeLook.INSTANCE.getEnabled()) {
                volthack.modules.render.FreeLook.INSTANCE.onMouseTurn(yRot, xRot);
                ci.cancel();
            }
        }
    }

//    @Inject(method = "isCrouching()Z", at = @At("HEAD"), cancellable = true)
//    private void onIsCrouching(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
//        if (volthack.modules.render.ShiftInterp.INSTANCE.getEnabled() && (Object) this instanceof net.minecraft.world.entity.player.Player) {
//            net.minecraft.world.entity.player.Player player = (net.minecraft.world.entity.player.Player) (Object) this;
//            if (volthack.modules.render.ShiftInterp.INSTANCE.shouldCrouch(player)) {
//                cir.setReturnValue(true);
//            }
//        }
//    }
}
