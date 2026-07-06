package ravex.mixin.network;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface AccessorMultiPlayerGameMode {
    @Accessor("destroyBlockPos")
    void setDestroyBlockPos(BlockPos pos);
}
