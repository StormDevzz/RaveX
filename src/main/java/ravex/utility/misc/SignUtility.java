package ravex.utility.misc;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;

public final class SignUtility {
    private SignUtility() {}

    public static boolean isSignScreenOpen() {
        return MinecraftWrapper.getWrapper().getCurrentScreen() instanceof AbstractSignEditScreen;
    }

    public static int findSignSlot() {
        var player = MinecraftWrapper.getWrapper().getPlayer();
        if (player == null) return -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(player, i);
            if (stack == null || stack.isEmpty()) continue;
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null && id.getPath().endsWith("_sign")) return i;
        }
        return -1;
    }

    // mode: Crosshair or Range. Returns true when a sign was placed.
    public static boolean placeSign(String mode, double range, boolean swapBack) {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null || mc.getGameMode() == null) return false;
        BlockHitResult hit = findPlaceHit(mc, player, mode, range);
        if (hit == null) return false;
        int signSlot = findSignSlot();
        if (signSlot == -1) return false;
        int prevSlot = InventoryUtility.getSelectedSlot(player);
        if (signSlot != prevSlot) InventoryUtility.selectSlot(player, signSlot);
        mc.getGameMode().useItemOn(player, InteractionHand.MAIN_HAND, hit);
        SwingUtility.swing(player, InteractionHand.MAIN_HAND);
        if (swapBack && signSlot != prevSlot) InventoryUtility.selectSlot(player, prevSlot);
        return true;
    }

    private static BlockHitResult findPlaceHit(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player,
                                               String mode, double range) {
        if ("Range".equals(mode)) {
            BlockPos target = findGroundTarget(mc, player, range);
            if (target == null) return null;
            BlockHitResult hit = BlockUtility.findPlaceTarget(mc, target);
            if (hit == null || hit.getLocation().distanceTo(player.getEyePosition()) > range) return null;
            return hit;
        }
        var hit = mc.getHitResult();
        if (!(hit instanceof BlockHitResult blockHit)) return null;
        if (hit.getLocation().distanceTo(player.getEyePosition()) > range) return null;
        BlockPos placePos = blockHit.getBlockPos().relative(blockHit.getDirection());
        if (!mc.getLevel().getBlockState(placePos).isAir()) return null;
        return new BlockHitResult(hit.getLocation(), blockHit.getDirection(), blockHit.getBlockPos(), false);
    }

    private static BlockPos findGroundTarget(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player, double range) {
        var level = mc.getLevel();
        Vec3 eye = player.getEyePosition();
        BlockPos feet = player.blockPosition();
        BlockPos base = feet.above();
        double bestDist = range * range;
        BlockPos best = null;
        int r = (int) Math.ceil(range);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = base.offset(dx, dy, dz);
                    if (pos.equals(feet) || pos.equals(feet.above())) continue;
                    if (!level.getBlockState(pos).isAir()) continue;
                    BlockPos below = pos.below();
                    if (!level.getBlockState(below).isCollisionShapeFullBlock(level, below)) continue;
                    double dist = eye.distanceToSqr(Vec3.atCenterOf(pos));
                    if (dist > bestDist) continue;
                    bestDist = dist;
                    best = pos;
                }
            }
        }
        return best;
    }
}
