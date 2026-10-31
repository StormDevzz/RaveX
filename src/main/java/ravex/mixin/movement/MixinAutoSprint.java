package ravex.mixin.movement;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.movement.AutoSprint;
import ravex.modules.Modules;

@Mixin(LocalPlayer.class)
public class MixinAutoSprint {

    // rage: stop inside the tick so its packet says stopped
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void ravexRageStop(CallbackInfo ci) {
        if (!Modules.enabled(AutoSprint.class)) return;
        AutoSprint as = Modules.get(AutoSprint.class);
        if (as == null || !"Rage".equals(as.mode)) return;

        LocalPlayer player = (LocalPlayer) (Object) this;
        var mc = MinecraftWrapper.getInstance();
        if (mc.options == null) return;
        boolean moving = mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
            || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
        if (moving) return;
        if (player.isFallFlying() || player.isInWater() || player.isInLava() || player.getAbilities().flying) return;

        var motion = player.getDeltaMovement();
        if (Math.hypot(motion.x, motion.z) > 0.05) {
            player.setDeltaMovement(0.0, motion.y, 0.0);
            player.hurtMarked = true;
        }
    }
}
