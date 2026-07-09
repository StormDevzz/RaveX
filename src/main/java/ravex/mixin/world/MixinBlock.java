package ravex.mixin.world;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.movement.NoSlow;
import ravex.modules.movement.Sleepy;

@Mixin(Block.class)
public class MixinBlock {
    @Inject(method = "getFriction", at = @At("RETURN"), cancellable = true)
    private void onGetFriction(CallbackInfoReturnable<Float> cir) {
<<<<<<< HEAD
        if (Sleepy.maybeEnabled()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                if (!Sleepy.itz().onlyOnGround.getValue() || mc.player.onGround()) {
                    cir.setReturnValue(Sleepy.itz().friction.getValue().floatValue());
=======
        if (ravex.modules.movement.Sleepy.INSTANCE.getEnabled()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                if (!ravex.modules.movement.Sleepy.INSTANCE.onlyOnGround.getValue() || mc.player.onGround()) {
                    cir.setReturnValue(ravex.modules.movement.Sleepy.INSTANCE.friction.getValue().floatValue());
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                    return;
                }
            }
        }

        if (NoSlow.maybeEnabled() && NoSlow.itz().blocks.getValue()) {
            Block self = (Block) (Object) this;
            if (NoSlow.itz().ice.getValue()) {
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
<<<<<<< HEAD
                float customFriction = NoSlow.getBlockFriction(blockId, cir.getReturnValue());
=======
                float customFriction = NoSlowDown.getBlockFriction(blockId, cir.getReturnValue());
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                cir.setReturnValue(customFriction);
            }
        }
    }
}

