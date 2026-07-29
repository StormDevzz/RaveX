package ravex.mixin.world;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import ravex.mcwrapper.MinecraftWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.movement.NoSlow;
import ravex.modules.movement.Sleepy;
import ravex.modules.Modules;

@Mixin(Block.class)
public class MixinBlock {
    @Inject(method = "getFriction", at = @At("RETURN"), cancellable = true)
    private void onGetFriction(CallbackInfoReturnable<Float> cir) {
        if (Modules.enabled(Sleepy.class)) {
            net.minecraft.client.Minecraft mc = MinecraftWrapper.getInstance();
            if (mc.player != null) {
                if (!Modules.get(Sleepy.class).onlyOnGround || mc.player.onGround()) {
                    cir.setReturnValue((float) Modules.get(Sleepy.class).friction);
                    return;
                }
            }
        }

        if (Modules.enabled(NoSlow.class) && Modules.get(NoSlow.class).blocks) {
            Block self = (Block) (Object) this;
            if (Modules.get(NoSlow.class).ice) {
                if (self == Blocks.ICE || self == Blocks.PACKED_ICE || self == Blocks.BLUE_ICE || self == Blocks.FROSTED_ICE
                    || self == Blocks.SLIME_BLOCK || self == Blocks.HONEY_BLOCK
                    || self == Blocks.SWEET_BERRY_BUSH || self == Blocks.COBWEB) {
                    cir.setReturnValue(0.6F);
                    return;
                }
            }
            if (self == Blocks.ICE || self == Blocks.PACKED_ICE || self == Blocks.BLUE_ICE || self == Blocks.FROSTED_ICE) {
                cir.setReturnValue(0.6F);
            } else {
                String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(self).toString();
                float customFriction = NoSlow.getBlockFriction(blockId, cir.getReturnValue());
                cir.setReturnValue(customFriction);
            }
        }
    }
}

