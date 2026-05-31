package ravex.mixin.world;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.movement.NoSlowDown;

@Mixin(Block.class)
public class MixinBlock {
    @Inject(method = "getFriction", at = @At("RETURN"), cancellable = true)
    private void onGetFriction(CallbackInfoReturnable<Float> cir) {
        if (ravex.modules.movement.Sleepy.INSTANCE.getEnabled()) {
            cir.setReturnValue(0.98F);
            return;
        }

        if (NoSlowDown.INSTANCE.getEnabled() && NoSlowDown.INSTANCE.blocks.getValue()) {
            Block self = (Block) (Object) this;
            if (self == Blocks.ICE || self == Blocks.PACKED_ICE || self == Blocks.BLUE_ICE || self == Blocks.FROSTED_ICE) {
                cir.setReturnValue(0.6F); // Default block friction
            }
        }
    }
}
