package ravex.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.movement.SafeWalk;

@Mixin(KeyMapping.class)
public class MixinKeyMapping {

    @Inject(method = "isDown()Z", at = @At("HEAD"), cancellable = true)
    private void onIsPressed(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!((KeyMapping)(Object)this).equals(mc.options.keyShift)) return;
        if (!SafeWalk.maybeEnabled()) return;
        if (!mc.player.onGround()) return;
        if (mc.level.getBlockState(
            BlockPos.containing(mc.player.getX(), mc.player.getY() - 1, mc.player.getZ())
        ).isAir()) {
            cir.setReturnValue(true);
        }
    }
}
