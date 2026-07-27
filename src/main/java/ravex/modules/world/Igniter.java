package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;

import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "Igniter", category = "World")
public class Igniter {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.1)
    public double range = 4.0;
    @Parameter(name = "SwapMode", modes = {"Silent", "Normal", "None"})
    public String swapMode = "Silent";
    @Parameter(name = "AutoDisable")
    public boolean autoDisable = false;
    @Parameter(name = "Rotate")
    public boolean rotate = true;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        int[] tntPos = findNearestTNT(mc);
        if (tntPos == null) return;
        int itemSlot = findIgnitionItem(mc);
        if (itemSlot == -1) return;
        var hitVec = net.minecraft.world.phys.Vec3.atCenterOf(BlockUtility.pos(tntPos[0], tntPos[1], tntPos[2]));
        if (rotate) {
            float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), hitVec);
            mc.getPlayer().setYRot(angles[0]);
            mc.getPlayer().setXRot(angles[1]);
        }
        int originalSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        String swap = swapMode;
        if (swap.equals("Normal")) {
            InventoryUtility.selectSlot(mc.getPlayer(), itemSlot);
        } else if (swap.equals("Silent")) {
            InventoryUtility.silentSelectSlot(mc.getPlayer(), itemSlot);
        } else if (swap.equals("None")) {
            if (InventoryUtility.getSelectedSlot(mc.getPlayer()) != itemSlot) {
                return;
            }
        }
        var targetPos = BlockUtility.pos(tntPos[0], tntPos[1], tntPos[2]);
        BlockUtility.useItemOn(mc, new net.minecraft.world.phys.BlockHitResult(
            hitVec, net.minecraft.core.Direction.UP, targetPos, false));
        BlockUtility.swing(mc);
        if (swap.equals("Silent") && originalSlot != -1) {
            InventoryUtility.silentSelectSlot(mc.getPlayer(), originalSlot);
        }
        if (autoDisable) {
            Modules.setEnabled(Igniter.class, false);
        }
    }
    private int[] findNearestTNT(MinecraftWrapper mc) {
        var playerPos = mc.getPlayer().blockPosition();
        double r = range;
        int rx = (int) Math.ceil(r);
        int[] closest = null;
        double bestDistSqr = Double.MAX_VALUE;
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -rx; dy <= rx; dy++) {
                for (int dz = -rx; dz <= rx; dz++) {
                    int x = playerPos.getX() + dx, y = playerPos.getY() + dy, z = playerPos.getZ() + dz;
                    var pos = BlockUtility.pos(x, y, z);
                    if (mc.getLevel().isLoaded(pos)) {
                        var state = mc.getLevel().getBlockState(pos);
                        if (BlockUtility.isBlock(state, "tnt")) {
                            double distSqr = mc.getPlayer().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos));
                            if (distSqr <= r * r && distSqr < bestDistSqr) {
                                bestDistSqr = distSqr;
                                closest = new int[]{x, y, z};
                            }
                        }
                    }
                }
            }
        }
        return closest;
    }
    private int findIgnitionItem(MinecraftWrapper mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (!stack.isEmpty()) {
                if (InventoryUtility.isItem(stack, "flint_and_steel") || InventoryUtility.isItem(stack, "fire_charge")) {
                    return i;
                }
            }
        }
        return -1;
    }



}