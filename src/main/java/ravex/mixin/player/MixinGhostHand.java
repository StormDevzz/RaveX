package ravex.mixin.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.player.GhostHand;

@Mixin(Minecraft.class)
public class MixinGhostHand {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void onStartUseItem(CallbackInfo ci) {
        if (!GhostHand.maybeEnabled()) return;

        Minecraft mc = (Minecraft)(Object)this;
        if (mc.player == null || mc.level == null) return;

        HitResult hit = mc.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) return;

        net.minecraft.client.player.LocalPlayer player = mc.player;
        double range = GhostHand.itz().range.getValue();
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.x * range, look.y * range, look.z * range);

        ClipContext ctx = new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        BlockHitResult blockHit = mc.level.clip(ctx);

        if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            Block block = state.getBlock();

            boolean canInteract = GhostHand.itz().allBlocks.getValue();
            if (!canInteract) {
                if (GhostHand.itz().chests.getValue() && isChest(block)) canInteract = true;
                else if (GhostHand.itz().enderChests.getValue() && block instanceof EnderChestBlock) canInteract = true;
                else if (GhostHand.itz().furnaces.getValue() && (block instanceof AbstractFurnaceBlock || block instanceof FurnaceBlock)) canInteract = true;
                else if (GhostHand.itz().craftingTables.getValue() && block instanceof CraftingTableBlock) canInteract = true;
                else if (GhostHand.itz().enchantTables.getValue() && block instanceof EnchantingTableBlock) canInteract = true;
            }

            if (canInteract) {
                mc.gameMode.useItemOn(player,
                    player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty()
                        ? net.minecraft.world.InteractionHand.OFF_HAND
                        : net.minecraft.world.InteractionHand.MAIN_HAND,
                    blockHit);
                ci.cancel();
            }
        }
    }

    private static boolean isChest(Block block) {
        return block instanceof ChestBlock || block instanceof BarrelBlock
            || block instanceof ShulkerBoxBlock || block instanceof HopperBlock
            || block instanceof DispenserBlock || block instanceof DropperBlock;
    }
}
