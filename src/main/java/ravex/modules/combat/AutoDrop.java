package ravex.modules.combat;
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import ravex.utility.misc.MobUtility;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotation;
import ravex.utility.player.SwingUtility;
public class AutoDrop extends Module {
    public final ModeParameter blockType = new ModeParameter("BlockType", "Gravel",
        java.util.List.of("Gravel", "Anvil", "Sand", "Both"));
    public final ModeParameter target = new ModeParameter("Target", "Self",
        java.util.List.of("Self", "Nearby", "Enemy"));
    public final NumberParameter range = new NumberParameter("Range", 4.0, 1.0, 6.0, 0.5);
    public final NumberParameter dropHeight = new NumberParameter("DropHeight", 3, 2, 6, 1);
    public final BooleanParameter airPlace = new BooleanParameter("AirPlace", true);
    public final ModeParameter rotate = new ModeParameter("Rotate", "NCP",
            java.util.List.of("NCP", "NCPStrict", "Strict", "None"));
    public final ModeParameter swapMode = new ModeParameter("Swap", "NCP",
            java.util.List.of("NCP", "NCPStrict", "Strict", "None"));
    public final BooleanParameter swapSwitchBack = new BooleanParameter("SwitchBack", true);
    public final NumberParameter placeDelay = new NumberParameter("Delay", 2, 1, 10, 1);
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_autodrop");
    static {
        NATIVE.load();
    }
    private static final SilentRotation silentRotation = new SilentRotation();
    private int tickCounter = 0;
    private int originalSlot = -1;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        tickCounter++;
        if (tickCounter < placeDelay.getValue().intValue()) return;
        tickCounter = 0;
        Entity targetEntity = findTarget(mc);
        if (targetEntity == null) return;
        BlockPos placePos = targetEntity.blockPosition().above(dropHeight.getValue().intValue());
        if (!mc.level.getBlockState(placePos).isAir() && !mc.level.getBlockState(placePos).canBeReplaced()) return;
        int slot = findDropBlock(mc);
        if (slot == -1) return;
        String swap = swapMode.getValue();
        if (swap.equals("None")) {
            if (InventoryUtility.getSelectedSlot(mc.player) != slot) return;
            originalSlot = -1;
        } else {
            originalSlot = InventoryUtility.getSelectedSlot(mc.player);
            InventoryUtility.silentSelectSlot(mc.player, slot);
        }
        Vec3 center = Vec3.atCenterOf(placePos);
        rotateTo(mc, center);
        String rot = rotate.getValue();
        if ((rot.equals("Strict") || rot.equals("NCPStrict")) && !isRotationAligned(mc, center)) return;
        BlockPos neighbor;
        Direction face = Direction.UP;
        if (airPlace.getValue() || mc.level.getBlockState(placePos.below()).isAir()) {
            neighbor = null;
            for (Direction dir : Direction.values()) {
                BlockPos side = placePos.relative(dir);
                if (!mc.level.getBlockState(side).isAir()) { neighbor = side; face = dir.getOpposite(); break; }
            }
            if (neighbor == null) { neighbor = placePos.above(); face = Direction.DOWN; }
        } else { neighbor = placePos.below(); }
        Vec3 hitVec = new Vec3(
            neighbor.getX() + 0.5 + face.getStepX() * 0.5,
            neighbor.getY() + 0.5 + face.getStepY() * 0.5,
            neighbor.getZ() + 0.5 + face.getStepZ() * 0.5
        );
        if (mc.gameMode != null)
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hitVec, face, neighbor, false));
        SwingUtility.swing(mc.player, InteractionHand.MAIN_HAND);
        if (swapSwitchBack.getValue() && originalSlot != -1 && !swap.equals("None")) {
            InventoryUtility.silentSelectSlot(mc.player, originalSlot);
        }
    }
    private Entity findTarget(Minecraft mc) {
        String t = target.getValue();
        if (t.equals("Self")) return mc.player;
        Entity best = null;
        double bestDist = range.getValue();
        for (Entity e : mc.level.entitiesForRendering()) {
            LivingEntity le = MobUtility.asLivingEntity(e);
            if (le == null || MobUtility.isSelf(le) || !e.isAlive()) continue;
            double dist = MobUtility.distanceToPlayer(e);
            if (dist < bestDist) { bestDist = dist; best = e; }
        }
        if (t.equals("Enemy") && !MobUtility.isPlayer(MobUtility.asLivingEntity(best))) return null;
        return best;
    }
    private int findDropBlock(Minecraft mc) {
        String type = blockType.getValue();
        for (int i = 0; i < 36; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;
            var block = ((BlockItem) stack.getItem()).getBlock();
            if (type.equals("Gravel") && block == Blocks.GRAVEL) return i;
            if (type.equals("Sand") && block == Blocks.SAND) return i;
            if (type.equals("Anvil") && block instanceof net.minecraft.world.level.block.AnvilBlock) return i;
            if (type.equals("Both") && (block instanceof FallingBlock || block instanceof net.minecraft.world.level.block.AnvilBlock)) return i;
        }
        return -1;
    }
    private void rotateTo(Minecraft mc, Vec3 target) {
        String mode = rotate.getValue();
        if (mode.equals("None")) return;
        float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), target);
        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();
        if (!silentRotation.initialized) { silentRotation.init(currentYaw, currentPitch); }
        currentYaw = silentRotation.lastYaw;
        currentPitch = silentRotation.lastPitch;
        float maxSpeed = 180.0f;
        float[] limited = AimUtility.limitAngles(currentYaw, angles[0], currentPitch, angles[1], maxSpeed);
        float finalYaw = limited[0], finalPitch = limited[1];
        silentRotation.set(finalYaw, finalPitch);
        silentRotation.lastYaw = finalYaw;
        silentRotation.lastPitch = finalPitch;
    }
    private boolean isRotationAligned(Minecraft mc, Vec3 target) {
        return silentRotation.isRotationAligned(mc, target, 10.0f);
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(AutoDrop.class);
    }
    public static AutoDrop itz() {
        return ModuleManager.get(AutoDrop.class);
    }
}