package ravex.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.mixin.network.AccessorMultiPlayerGameMode;
import ravex.modules.player.PacketMine;
import ravex.modules.Modules;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        PacketMine pm = Modules.get(PacketMine.class);

        if (Modules.enabled(PacketMine.class) && pm.grimMode.equals("Strict")) {
            if (pm.isTargetBlock(pos)) {
                AccessorMultiPlayerGameMode accessor = (AccessorMultiPlayerGameMode) this;
                accessor.setDestroyBlockPos(pos);
            }
        }
    }
}
