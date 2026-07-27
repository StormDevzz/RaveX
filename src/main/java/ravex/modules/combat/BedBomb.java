package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import org.jetbrains.annotations.Nullable;


@Module(name = "BedBomb", category = "Combat")
public class BedBomb {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.5)
    public double range = 4.5;
    @Parameter(name = "TargetRange", min = 1.0, max = 12.0, step = 0.5)
    public double targetRange = 6.0;
    @Parameter(name = "Rotate")
    public boolean rotate = true;
    @Parameter(name = "AutoSwitch")
    public boolean autoSwitch = true;
    @Parameter(name = "Color", color = true)
    public int color = 0x3FFF4444;
    @Parameter(name = "Render")
    public boolean render = true;
    public static net.minecraft.core.BlockPos currentTarget = null;
    private enum State { IDLE, FIND_TARGET, PLACING, WAITING, DETONATE }
    private State state = State.IDLE;
    private net.minecraft.core.BlockPos bedPos = null;
    private net.minecraft.core.BlockPos placePos = null;
    private long lastActionTime = 0;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_bedbomb");
    static {
        NATIVE.load();
    }
    public void onEnable() {
        state = State.IDLE;
        bedPos = null;
        placePos = null;
        currentTarget = null;
    }
    public void onDisable() {
        bedPos = null;
        placePos = null;
        currentTarget = null;
        state = State.IDLE;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        if (!canExplode(mc)) return;
        long now = System.currentTimeMillis();
        switch (state) {
            case IDLE -> state = State.FIND_TARGET;
            case FIND_TARGET -> findTarget(mc);
            case PLACING -> doPlace(mc, now);
            case WAITING -> doWait(mc, now);
            case DETONATE -> doDetonate(mc, now);
        }
    }
    private boolean canExplode(MinecraftWrapper mc) {
        return BlockUtility.isExplodable(mc.getLevel());
    }
    private void findTarget(MinecraftWrapper mc) {
        var target = findNearestEnemy(mc);
        if (target == null) return;
        net.minecraft.core.BlockPos enemyPos = target.blockPosition();
        net.minecraft.core.BlockPos bestPos = null;
        if (NATIVE.isLoaded()) {
            double[] result = new double[4];
            nativeFindBestPlace(mc.getPlayer().getX(), mc.getPlayer().getY(), mc.getPlayer().getZ(),
                enemyPos.getX(), enemyPos.getY(), enemyPos.getZ(), range, result);
            if (result[0] != Double.MAX_VALUE) {
                bestPos = net.minecraft.core.BlockPos.containing(result[0], result[1], result[2]);
            }
        } else {
            bestPos = findPlacePos(mc, enemyPos);
        }
        if (bestPos == null) return;
        int slot = findBedSlot(mc);
        if (slot == -1) return;
        placePos = bestPos;
        bedPos = bestPos.above();
        currentTarget = bedPos;
        state = State.PLACING;
    }
    private void doPlace(MinecraftWrapper mc, long now) {
        if (now - lastActionTime < 150) return;
        lastActionTime = now;
        if (placePos == null || !mc.getLevel().getBlockState(placePos).isAir()) {
            state = State.IDLE;
            return;
        }
        int slot = findBedSlot(mc);
        if (slot == -1) { state = State.IDLE; return; }
        int prev = InventoryUtility.getSelectedSlot(mc.getPlayer());
        InventoryUtility.selectSlot(mc.getPlayer(), slot);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
            net.minecraft.world.phys.Vec3.atCenterOf(placePos), net.minecraft.core.Direction.UP, placePos, false
        );
        mc.getGameMode().useItemOn(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
        InventoryUtility.selectSlot(mc.getPlayer(), prev);
        state = State.WAITING;
    }
    private void doWait(MinecraftWrapper mc, long now) {
        if (now - lastActionTime < 100) return;
        state = State.DETONATE;
    }
    private void doDetonate(MinecraftWrapper mc, long now) {
        if (now - lastActionTime < 50) return;
        lastActionTime = now;
        if (bedPos == null) { state = State.IDLE; return; }
        net.minecraft.world.level.block.state.BlockState st = mc.getLevel().getBlockState(bedPos);
        if (!st.is(net.minecraft.world.level.block.Blocks.RED_BED) && !st.is(net.minecraft.world.level.block.Blocks.WHITE_BED)) {
            boolean isBed = st.getBlock() instanceof BedBlock;
            if (!isBed) { state = State.IDLE; return; }
        }
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
            net.minecraft.world.phys.Vec3.atCenterOf(bedPos), net.minecraft.core.Direction.UP, bedPos, false
        );
        mc.getGameMode().useItemOn(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
        state = State.IDLE;
    }
    private net.minecraft.world.entity.LivingEntity findNearestEnemy(MinecraftWrapper mc) {
        double maxDist = targetRange;
        net.minecraft.world.entity.LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (net.minecraft.world.entity.Entity e : mc.getLevel().entitiesForRendering()) {
            if (!(e instanceof net.minecraft.world.entity.player.Player p)) continue;
            if (p == mc.getPlayer()) continue;
            if (p.isDeadOrDying()) continue;
            double dist = mc.getPlayer().distanceTo(p);
            if (dist > maxDist) continue;
            if (dist < closestDist) {
                closestDist = dist;
                closest = p;
            }
        }
        return closest;
    }
    @Nullable
    private net.minecraft.core.BlockPos findPlacePos(MinecraftWrapper mc, net.minecraft.core.BlockPos near) {
        double r = range;
        net.minecraft.world.phys.Vec3 eye = mc.getPlayer().getEyePosition();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    net.minecraft.core.BlockPos pos = near.offset(dx, dy, dz);
                    if (pos.distToCenterSqr(eye) > r * r) continue;
                    net.minecraft.world.level.block.state.BlockState below = mc.getLevel().getBlockState(pos.below());
                    net.minecraft.world.level.block.state.BlockState target = mc.getLevel().getBlockState(pos);
                    net.minecraft.world.level.block.state.BlockState above = mc.getLevel().getBlockState(pos.above());
                    if (below.isCollisionShapeFullBlock(mc.getLevel(), pos.below())
                        && target.isAir() && above.isAir()) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
    private int findBedSlot(MinecraftWrapper mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (stack.getItem() instanceof net.minecraft.world.item.BedItem) return i;
        }
        for (int i = 9; i < 36; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (stack.getItem() instanceof net.minecraft.world.item.BedItem) {
                int free = findEmptySlot(mc);
                if (free != -1) {
                    InventoryUtility.selectSlot(mc.getPlayer(), free);
                    mc.getGameMode().handleInventoryMouseClick(
                        mc.getPlayer().containerMenu.containerId, i, free,
                        InventoryUtility.SWAP, mc.getPlayer()
                    );
                    return free;
                }
            }
        }
        return -1;
    }
    private int findEmptySlot(MinecraftWrapper mc) {
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.getItem(mc.getPlayer(), i).isEmpty()) return i;
        }
        return -1;
    }
    private static native void nativeFindBestPlace(double px, double py, double pz, double ex, double ey, double ez, double range, double[] out);




}