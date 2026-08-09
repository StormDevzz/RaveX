package ravex.mixin.world;

import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.NoSlow;
import ravex.modules.Modules;

@Mixin(HoneyBlock.class)
public class MixinHoneyBlock {

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity, net.minecraft.world.entity.InsideBlockEffectApplier insideBlockEffectApplier, boolean isInside, CallbackInfo ci) {
        if (entity instanceof LocalPlayer) {
            if (Modules.enabled(NoSlow.class) && Modules.get(NoSlow.class).blocks) {
                ci.cancel();
            }
        }
    }
}
