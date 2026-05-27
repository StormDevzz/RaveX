package ravex.mixin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.ESP;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (ESP.INSTANCE.getEnabled() && ESP.INSTANCE.mode.getValue().equals("Outline")) {
            Entity self = (Entity) (Object) this;
            // Only make living entities (players/monsters) glow to prevent level rendering crashes
            if (self instanceof LivingEntity) {
                boolean isPlayer = self instanceof Player;
                boolean isMonster = self instanceof Monster;
                if (isPlayer && ESP.INSTANCE.players.getValue()) {
                    cir.setReturnValue(true);
                } else if (isMonster && ESP.INSTANCE.monsters.getValue()) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColor(CallbackInfoReturnable<Integer> cir) {
        if (ESP.INSTANCE.getEnabled() && ESP.INSTANCE.mode.getValue().equals("Outline")) {
            Entity self = (Entity) (Object) this;
            if (self instanceof LivingEntity) {
                if (self instanceof Player) {
                    cir.setReturnValue(0xFFFF5555); // neon red for players
                } else if (self instanceof Monster) {
                    cir.setReturnValue(0xFF55FF55); // neon green for mobs
                }
            }
        }
    }
}
