package ravex.mixin.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.utility.player.GhostHandRayUtility;

@Mixin(Entity.class)
public class MixinGhostHand {
    // replace the pick, the click packet stays vanilla
    @Inject(method = "pick(DFZ)Lnet/minecraft/world/phys/HitResult;", at = @At("HEAD"), cancellable = true)
    private void ravexGhostHandPick(double range, float partialTicks, boolean includeFluids,
            CallbackInfoReturnable<HitResult> cir) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof net.minecraft.client.player.LocalPlayer player)) return;
        BlockHitResult hit = GhostHandRayUtility.findTargetBlock(player, range, partialTicks, includeFluids);
        if (hit != null) {
            cir.setReturnValue(hit);
        }
    }
}
