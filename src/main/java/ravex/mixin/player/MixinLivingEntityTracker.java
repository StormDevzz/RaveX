package ravex.mixin.player;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.misc.PopCounter;
import ravex.modules.misc.ChatUtils;

@Mixin(LivingEntity.class)
public class MixinLivingEntityTracker {

    @Inject(method = "die", at = @At("HEAD"))
    private void onDie(DamageSource source, CallbackInfo ci) {
        if ((Object)this instanceof Player player) {
            ChatUtils.INSTANCE.onPlayerDeath(player, source);
        }
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
    private void onCheckTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && (Object)this instanceof Player player) {
            PopCounter.INSTANCE.onPop(player);
        }
    }
}
