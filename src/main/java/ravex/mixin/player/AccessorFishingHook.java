package ravex.mixin.player;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FishingHook.class)
public interface AccessorFishingHook {
    // bite flag is the only client synced signal
    @Accessor("biting")
    boolean getBiting();
}
