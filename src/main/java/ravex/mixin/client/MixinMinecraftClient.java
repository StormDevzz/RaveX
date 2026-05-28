package ravex.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.RaveX;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        RaveX.onClientTick();
    }

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void onShouldEntityAppearGlowing(net.minecraft.world.entity.Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ravex.modules.render.ESP.INSTANCE.getEnabled() && ravex.modules.render.ESP.INSTANCE.mode.getValue().equals("Outline")) {
            if (entity instanceof net.minecraft.world.entity.LivingEntity) {
                boolean isPlayer = entity instanceof net.minecraft.world.entity.player.Player;
                boolean isMonster = entity instanceof net.minecraft.world.entity.monster.Monster;
                if (isPlayer && ravex.modules.render.ESP.INSTANCE.players.getValue()) {
                    cir.setReturnValue(true);
                } else if (isMonster && ravex.modules.render.ESP.INSTANCE.monsters.getValue()) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
