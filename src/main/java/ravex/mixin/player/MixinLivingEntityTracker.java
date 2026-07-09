package ravex.mixin.player;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
<<<<<<< HEAD
import ravex.event.EventBusHolder;
import ravex.event.player.DeathEvent;
import ravex.event.combat.TotemPopEvent;
=======
import ravex.modules.misc.PopCounter;
import ravex.modules.misc.ChatUtils;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

@Mixin(LivingEntity.class)
public class MixinLivingEntityTracker {

    @Inject(method = "die", at = @At("HEAD"))
    private void onDie(DamageSource source, CallbackInfo ci) {
        if ((Object)this instanceof Player player) {
<<<<<<< HEAD
            EventBusHolder.get().post(new DeathEvent(player, source));
=======
            ChatUtils.INSTANCE.onPlayerDeath(player, source);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
    private void onCheckTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && (Object)this instanceof Player player) {
<<<<<<< HEAD
            EventBusHolder.get().post(new TotemPopEvent(player));
=======
            PopCounter.INSTANCE.onPop(player);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
    }
}
