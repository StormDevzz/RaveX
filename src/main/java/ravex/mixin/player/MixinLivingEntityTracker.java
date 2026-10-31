package ravex.mixin.player;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.event.EventBusHolder;
import ravex.event.player.DeathEvent;
import ravex.event.combat.TotemPopEvent;

@Mixin(LivingEntity.class)
public class MixinLivingEntityTracker {

    @Inject(method = "die", at = @At("HEAD"))
    private void onDie(DamageSource source, CallbackInfo ci) {
        // on LivingEntity, so mobs are covered too
        EventBusHolder.get().post(new DeathEvent((LivingEntity) (Object) this, source));
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
    private void onCheckTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && (Object)this instanceof Player player) {
            EventBusHolder.get().post(new TotemPopEvent(player));
            if (player == ravex.utility.player.PlayerUtility.getPlayer()) {
                ravex.modules.render.Animations.totemPopNs = System.nanoTime();
            }
        }
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void onHandleEntityEvent(byte id, CallbackInfo ci) {
        // remote players pop through the entity event
        if (id != net.minecraft.world.entity.EntityEvent.PROTECTED_FROM_DEATH) return;
        if (!((Object) this instanceof Player player)) return;
        if (player == ravex.utility.player.PlayerUtility.getPlayer()) return;
        EventBusHolder.get().post(new TotemPopEvent(player));
    }
}
