package ravex.mixin.network;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface AccessorMultiPlayerGameMode {
    @Accessor("destroyBlockPos")
    void setDestroyBlockPos(BlockPos pos);

    @Accessor("destroyProgress")
    float getDestroyProgress();

    @Accessor("destroyProgress")
    void setDestroyProgress(float progress);

    // fastBreak: vanilla waits 5 ticks
    @Accessor("destroyDelay")
    int getDestroyDelay();

    @Accessor("destroyDelay")
    void setDestroyDelay(int delay);

    // keep the held-item tracker in sync, Grim rejects resends
    @Accessor("carriedIndex")
    int getCarriedIndex();

    @Accessor("carriedIndex")
    void setCarriedIndex(int index);

    // dig packets carry the shared sequence counter
    @Invoker("startPrediction")
    void ravexStartPrediction(ClientLevel level, PredictiveAction action);
}
