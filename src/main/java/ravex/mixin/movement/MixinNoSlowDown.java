package ravex.mixin.movement;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.movement.NoSlowDown;

@Mixin(LivingEntity.class)
public abstract class MixinNoSlowDown {

    @Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true)
    private void onGetSpeed(CallbackInfoReturnable<Float> cir) {
        if (!NoSlowDown.INSTANCE.getEnabled()) return;

        LivingEntity self = (LivingEntity)(Object)this;
        if (self != Minecraft.getInstance().player) return;
        if (!self.isUsingItem()) return;

        double baseSpeed = self.getAttributeValue(Attributes.MOVEMENT_SPEED);
        cir.setReturnValue((float) baseSpeed);
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStepTail(CallbackInfo ci) {
        if (!NoSlowDown.INSTANCE.getEnabled()) return;

        LivingEntity self = (LivingEntity)(Object)this;
        if (self != Minecraft.getInstance().player) return;

        String mode = NoSlowDown.INSTANCE.mode.getValue();
        if (!self.isUsingItem()) return;

        if (self instanceof LocalPlayer lp) {
            if ("Grim".equals(mode) || "NCP".equals(mode)) {
                lp.setSprinting(true);
            }
        }
    }
}
