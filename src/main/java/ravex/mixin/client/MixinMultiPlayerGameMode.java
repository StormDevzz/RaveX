package ravex.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.mixin.network.AccessorMultiPlayerGameMode;
<<<<<<< HEAD
=======
import ravex.mixin.network.AccessorMultiPlayerGameMode;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.player.PacketMine;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
<<<<<<< HEAD
        PacketMine pm = PacketMine.itz();

        if (pm.getEnabled() && pm.grimMode.getValue().equals("Strict")) {
            if (pm.isTargetBlock(pos)) {
=======

        if (PacketMine.INSTANCE.getEnabled() && PacketMine.INSTANCE.grimStrict.getValue()) {
            if (PacketMine.INSTANCE.isTargetBlock(pos)) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                AccessorMultiPlayerGameMode accessor = (AccessorMultiPlayerGameMode) this;
                accessor.setDestroyBlockPos(pos);
            }
        }
    }
}
