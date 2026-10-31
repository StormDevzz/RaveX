package ravex.utility.player;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import ravex.modules.Modules;
import ravex.modules.player.GhostHand;

public final class GhostHandRayUtility {
    private GhostHandRayUtility() {}

    // ray only sees the selected containers, walls do not block the
    // pick and the hit still looks vanilla to the server
    public static BlockHitResult findTargetBlock(net.minecraft.client.player.LocalPlayer player,
            double range, float partialTicks, boolean includeFluids) {
        if (!Modules.enabled(GhostHand.class)) return null;
        GhostHand gh = Modules.get(GhostHand.class);
        if (gh == null) return null;
        // while attack is held the pick stays vanilla
        var options = ravex.mcwrapper.MinecraftWrapper.getWrapper().getOptions();
        if (options != null && options.keyAttack.isDown()) return null;
        var level = player.level();
        Vec3 from = player.getEyePosition(partialTicks);
        Vec3 look = player.getViewVector(partialTicks);
        Vec3 to = from.add(look.x * range, look.y * range, look.z * range);
        ClipContext context = new ClipContext(from, to, ClipContext.Block.OUTLINE,
            includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, player);
        BlockHitResult hit = BlockGetter.traverseBlocks(from, to, context,
            (ClipContext ctx, BlockPos pos) -> {
                BlockState state = level.getBlockState(pos);
                if (!isTarget(gh, state.getBlock())) return null;
                VoxelShape shape = ctx.getBlockShape(state, level, pos);
                return level.clipWithInteractionOverride(from, to, pos, shape, state);
            },
            (ClipContext ctx) -> null);
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            return hit;
        }
        return null;
    }

    private static boolean isTarget(GhostHand gh, Block block) {
        if (block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock
                || block instanceof HopperBlock || block instanceof DispenserBlock || block instanceof DropperBlock) {
            return gh.chests;
        }
        if (block instanceof EnderChestBlock) return gh.enderChests;
        if (block instanceof AbstractFurnaceBlock) return gh.furnaces;
        if (block instanceof CraftingTableBlock) return gh.craftingTables;
        if (block instanceof EnchantingTableBlock) return gh.enchantTables;
        return false;
    }
}
