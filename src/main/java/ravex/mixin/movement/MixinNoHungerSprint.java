package ravex.mixin.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.NoHungerSprint;

@Mixin(Player.class)
public abstract class MixinNoHungerSprint {

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStepTail(CallbackInfo ci) {
        if (!NoHungerSprint.maybeEnabled()) return;

        Player self = (Player)(Object)this;
        Minecraft mc = Minecraft.getInstance();

        if (self.getFoodData().getFoodLevel() <= 6.0F
            && self.input != null
            && self.input.hasForwardImpulse()
            && !self.isUsingItem()
            && !self.isShiftKeyDown()) {
            self.setSprinting(true);
        }
    }
}
