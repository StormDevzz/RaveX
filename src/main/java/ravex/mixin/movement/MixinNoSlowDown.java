package ravex.mixin.movement;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class MixinNoSlowDown {
    // Moved to MixinLocalPlayer to correctly bypass LocalPlayer.aiStep item-use slowdown
}
