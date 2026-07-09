package ravex.mixin.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.player.NoInteract;

@Mixin(Minecraft.class)
public class MixinNoInteract {

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void onStartUseItem(CallbackInfo ci) {
        NoInteract ni = NoInteract.itz();
        if (!ni.getEnabled()) return;

        if (ni.allBlocks.getValue()) {
            ci.cancel();
            return;
        }

        Minecraft mc = (Minecraft)(Object)this;
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            Block block = state.getBlock();

            if (ni.chests.getValue() && isChestLike(block)) {
                ci.cancel();
            }
            if (ni.enderChests.getValue() && block instanceof EnderChestBlock) {
                ci.cancel();
            }
            if (ni.furnaces.getValue() && block instanceof AbstractFurnaceBlock) {
                ci.cancel();
            }
            if (ni.crafting.getValue() && block instanceof CraftingTableBlock) {
                ci.cancel();
            }
            if (ni.enchanting.getValue() && block instanceof EnchantingTableBlock) {
                ci.cancel();
            }
        }
    }

    private static boolean isChestLike(Block block) {
        return block instanceof ChestBlock
            || block instanceof BarrelBlock
            || block instanceof ShulkerBoxBlock;
    }
}
