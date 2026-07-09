package ravex.modules.world;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
<<<<<<< HEAD
import ravex.utility.player.InventoryUtility;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.rotation.RotationUtility;
public class Igniter extends Module {
=======
public class Igniter extends Module {
    public static final Igniter INSTANCE = new Igniter();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter  range        = new NumberParameter("Range",        4.0, 1.0, 6.0, 0.1);
    public final ModeParameter    swapMode     = new ModeParameter("SwapMode", "Silent",
            java.util.List.of("Silent", "Normal", "None"));
    public final BooleanParameter autoDisable  = new BooleanParameter("AutoDisable",  false);
    public final BooleanParameter rotate       = new BooleanParameter("Rotate",       true);

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
<<<<<<< HEAD
        int[] tntPos = findNearestTNT(mc);
        if (tntPos == null) return;
        int itemSlot = findIgnitionItem(mc);
        if (itemSlot == -1) return;
        var hitVec = net.minecraft.world.phys.Vec3.atCenterOf(BlockUtility.pos(tntPos[0], tntPos[1], tntPos[2]));
=======
        BlockPos tntPos = findNearestTNT(mc);
        if (tntPos == null) return;
        int itemSlot = findIgnitionItem(mc);
        if (itemSlot == -1) return;
        Vec3 hitVec = Vec3.atCenterOf(tntPos);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (rotate.getValue()) {
            float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), hitVec);
            mc.player.setYRot(angles[0]);
            mc.player.setXRot(angles[1]);
        }
<<<<<<< HEAD
        int originalSlot = InventoryUtility.getSelectedSlot(mc.player);
=======
        int originalSlot = mc.player.getInventory().getSelectedSlot();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        String swap = swapMode.getValue();
        if (swap.equals("Normal")) {
            InventoryUtility.selectSlot(mc.player, itemSlot);
        } else if (swap.equals("Silent")) {
            InventoryUtility.silentSelectSlot(mc.player, itemSlot);
        } else if (swap.equals("None")) {
            if (InventoryUtility.getSelectedSlot(mc.player) != itemSlot) {
                return;
            }
        }
<<<<<<< HEAD
        var targetPos = BlockUtility.pos(tntPos[0], tntPos[1], tntPos[2]);
        BlockUtility.useItemOn(mc, new net.minecraft.world.phys.BlockHitResult(
            hitVec, Direction.UP, targetPos, false));
        BlockUtility.swing(mc);
=======
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, tntPos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (swap.equals("Silent") && originalSlot != -1) {
            InventoryUtility.silentSelectSlot(mc.player, originalSlot);
        }
        if (autoDisable.getValue()) {
            setEnabled(false);
        }
    }
<<<<<<< HEAD
    private int[] findNearestTNT(Minecraft mc) {
        var playerPos = mc.player.blockPosition();
        double r = range.getValue();
        int rx = (int) Math.ceil(r);
        int[] closest = null;
=======
    private BlockPos findNearestTNT(Minecraft mc) {
        BlockPos playerPos = mc.player.blockPosition();
        double r = range.getValue();
        int rx = (int) Math.ceil(r);
        int ry = (int) Math.ceil(r);
        int rz = (int) Math.ceil(r);
        BlockPos closest = null;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        double bestDistSqr = Double.MAX_VALUE;
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -rx; dy <= rx; dy++) {
                for (int dz = -rx; dz <= rx; dz++) {
                    int x = playerPos.getX() + dx, y = playerPos.getY() + dy, z = playerPos.getZ() + dz;
                    var pos = BlockUtility.pos(x, y, z);
                    if (mc.level.isLoaded(pos)) {
                        var state = mc.level.getBlockState(pos);
                        if (BlockUtility.isBlock(state, "tnt")) {
                            double distSqr = mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos));
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
    private int findIgnitionItem(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (!stack.isEmpty()) {
                if (InventoryUtility.isItem(stack, "flint_and_steel") || InventoryUtility.isItem(stack, "fire_charge")) {
                    return i;
                }
            }
        }
        return -1;
    }
<<<<<<< HEAD
    public static Igniter itz() {
        return ModuleManager.get(Igniter.class);
=======
    private void rotateTo(Minecraft mc, Vec3 target) {
        Vec3 eyes = mc.player.getEyePosition();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx*dx + dz*dz);
        float targetYaw   = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        mc.player.setYRot(targetYaw);
        mc.player.setXRot(targetPitch);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }
}
