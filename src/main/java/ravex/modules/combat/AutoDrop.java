package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.MobUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.FallingBlock;





@ModuleInfo(name = "AutoDrop", category = "Combat")
public class AutoDrop implements ModuleAccess {
    @Parameter(name = "BlockType", modes = {"Gravel", "Anvil", "Sand", "Both"})
    public String blockType = "Gravel";
    @Parameter(name = "Target", modes = {"Self", "Nearby", "Enemy"})
    public String target = "Self";
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.5)
    public double range = 4.0;
    @Parameter(name = "DropHeight", min = 2, max = 6, step = 1)
    public double dropHeight = 3;
    @Parameter(name = "AirPlace")
    public boolean airPlace = true;
    @Parameter(name = "Rotate", modes = {"NCP", "NCPStrict", "Strict", "None"})
    public String rotate = "NCP";
    @Parameter(name = "Swap", modes = {"NCP", "NCPStrict", "Strict", "None"})
    public String swapMode = "NCP";
    @Parameter(name = "SwitchBack")
    public boolean swapSwitchBack = true;
    @Parameter(name = "Delay", min = 1, max = 10, step = 1)
    public double placeDelay = 2;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_autodrop");
    static {
        NATIVE.load();
    }
    private static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    private int tickCounter = 0;
    private int originalSlot = -1;
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        tickCounter++;
        if (tickCounter < (int) placeDelay) return;
        tickCounter = 0;
        net.minecraft.world.entity.Entity targetEntity = findTarget(mc);
        if (targetEntity == null) return;
        net.minecraft.core.BlockPos placePos = targetEntity.blockPosition().above((int) dropHeight);
        if (!mc.level.getBlockState(placePos).isAir() && !mc.level.getBlockState(placePos).canBeReplaced()) return;
        int slot = findDropBlock(mc);
        if (slot == -1) return;
        String swap = swapMode;
        if (swap.equals("None")) {
            if (InventoryUtility.getSelectedSlot(mc.player) != slot) return;
            originalSlot = -1;
        } else {
            originalSlot = InventoryUtility.getSelectedSlot(mc.player);
            InventoryUtility.silentSelectSlot(mc.player, slot);
        }
        net.minecraft.world.phys.Vec3 center = net.minecraft.world.phys.Vec3.atCenterOf(placePos);
        rotateTo(mc, center);
        String rot = rotate;
        if ((rot.equals("Strict") || rot.equals("NCPStrict")) && !isRotationAligned(mc, center)) return;
        net.minecraft.core.BlockPos neighbor;
        net.minecraft.core.Direction face = net.minecraft.core.Direction.UP;
        if (airPlace || mc.level.getBlockState(placePos.below()).isAir()) {
            neighbor = null;
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                net.minecraft.core.BlockPos side = placePos.relative(dir);
                if (!mc.level.getBlockState(side).isAir()) { neighbor = side; face = dir.getOpposite(); break; }
            }
            if (neighbor == null) { neighbor = placePos.above(); face = net.minecraft.core.Direction.DOWN; }
        } else { neighbor = placePos.below(); }
        net.minecraft.world.phys.Vec3 hitVec = new net.minecraft.world.phys.Vec3(
            neighbor.getX() + 0.5 + face.getStepX() * 0.5,
            neighbor.getY() + 0.5 + face.getStepY() * 0.5,
            neighbor.getZ() + 0.5 + face.getStepZ() * 0.5
        );
        if (mc.gameMode != null)
            mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(hitVec, face, neighbor, false));
        SwingUtility.swing(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (swapSwitchBack && originalSlot != -1 && !swap.equals("None")) {
            InventoryUtility.silentSelectSlot(mc.player, originalSlot);
        }
    }
    private net.minecraft.world.entity.Entity findTarget(Minecraft mc) {
        String t = target;
        if (t.equals("Self")) return mc.player;
        net.minecraft.world.entity.Entity best = null;
        double bestDist = range;
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            net.minecraft.world.entity.LivingEntity le = MobUtility.asLivingEntity(e);
            if (le == null || MobUtility.isSelf(le) || !e.isAlive()) continue;
            double dist = MobUtility.distanceToPlayer(e);
            if (dist < bestDist) { bestDist = dist; best = e; }
        }
        if (t.equals("Enemy") && !MobUtility.isPlayer(MobUtility.asLivingEntity(best))) return null;
        return best;
    }
    private int findDropBlock(Minecraft mc) {
        String type = blockType;
        for (int i = 0; i < 36; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;
            var block = ((BlockItem) stack.getItem()).getBlock();
            if (type.equals("Gravel") && block == net.minecraft.world.level.block.Blocks.GRAVEL) return i;
            if (type.equals("Sand") && block == net.minecraft.world.level.block.Blocks.SAND) return i;
            if (type.equals("Anvil") && block instanceof net.minecraft.world.level.block.AnvilBlock) return i;
            if (type.equals("Both") && (block instanceof FallingBlock || block instanceof net.minecraft.world.level.block.AnvilBlock)) return i;
        }
        return -1;
    }
    private void rotateTo(Minecraft mc, net.minecraft.world.phys.Vec3 target) {
        String mode = rotate;
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
    private boolean isRotationAligned(Minecraft mc, net.minecraft.world.phys.Vec3 target) {
        return silentRotation.isRotationAligned(mc, target, 10.0f);
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoDrop").getEnabled();
    }
    public static AutoDrop itz() {
        return ravex.manager.ModuleManager.delegate(AutoDrop.class);
    }


}