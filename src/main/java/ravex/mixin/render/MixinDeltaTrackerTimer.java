package ravex.mixin.render;

import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Timer mixin — timer acceleration handled via MixinTimer movement modifier instead.
 */
@Mixin(DeltaTracker.Timer.class)
public class MixinDeltaTrackerTimer {
}
