package volthack.mixin.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientLevel {
    @Inject(
        method = "addDestroyBlockEffect",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onAddDestroyBlockEffect(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (volthack.modules.render.NoRender.INSTANCE.getEnabled() && volthack.modules.render.NoRender.INSTANCE.getBreakParticles()) {
            ci.cancel();
        }
    }

    @Inject(
        method = "addBreakingBlockEffect",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onAddBreakingBlockEffect(BlockPos pos, Direction side, CallbackInfo ci) {
        if (volthack.modules.render.NoRender.INSTANCE.getEnabled() && volthack.modules.render.NoRender.INSTANCE.getBreakParticles()) {
            ci.cancel();
        }
    }
}
