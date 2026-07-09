package ravex.modules.combat;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
<<<<<<< HEAD
=======
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
<<<<<<< HEAD
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
<<<<<<< HEAD
import ravex.utility.player.InventoryUtility;
import java.util.List;
public class BedBomb extends Module {
=======
import java.util.List;
public class BedBomb extends Module {
    public static final BedBomb INSTANCE = new BedBomb();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter range = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.5);
    public final NumberParameter targetRange = new NumberParameter("TargetRange", 6.0, 1.0, 12.0, 0.5);
    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
    public final BooleanParameter autoSwitch = new BooleanParameter("AutoSwitch", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3FFF4444);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public static BlockPos currentTarget = null;
    private enum State { IDLE, FIND_TARGET, PLACING, WAITING, DETONATE }
    private State state = State.IDLE;
    private BlockPos bedPos = null;
    private BlockPos placePos = null;
    private long lastActionTime = 0;
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_bedbomb");
    static {
        NATIVE.load();
    }

    @Override
    protected void onEnable() {
        state = State.IDLE;
        bedPos = null;
        placePos = null;
        currentTarget = null;
    }
    @Override
    protected void onDisable() {
        bedPos = null;
        placePos = null;
        currentTarget = null;
        state = State.IDLE;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (!canExplode(mc.level)) return;
        long now = System.currentTimeMillis();
        switch (state) {
            case IDLE -> state = State.FIND_TARGET;
            case FIND_TARGET -> findTarget(mc);
            case PLACING -> doPlace(mc, now);
            case WAITING -> doWait(mc, now);
            case DETONATE -> doDetonate(mc, now);
        }
    }
    private boolean canExplode(Level level) {
        var dim = level.dimension();
        return dim == Level.NETHER || dim == Level.END;
    }
    private void findTarget(Minecraft mc) {
        var target = findNearestEnemy(mc);
        if (target == null) return;
        BlockPos enemyPos = target.blockPosition();
        BlockPos bestPos = null;
        if (NATIVE.isLoaded()) {
            double[] result = new double[4];
            nativeFindBestPlace(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                enemyPos.getX(), enemyPos.getY(), enemyPos.getZ(), range.getValue(), result);
            if (result[0] != Double.MAX_VALUE) {
                bestPos = BlockPos.containing(result[0], result[1], result[2]);
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
    private void doPlace(Minecraft mc, long now) {
        if (now - lastActionTime < 150) return;
        lastActionTime = now;
        if (placePos == null || !mc.level.getBlockState(placePos).isAir()) {
            state = State.IDLE;
            return;
        }
        int slot = findBedSlot(mc);
        if (slot == -1) { state = State.IDLE; return; }
<<<<<<< HEAD
        int prev = InventoryUtility.getSelectedSlot(mc.player);
        InventoryUtility.selectSlot(mc.player, slot);
=======
        int prev = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(placePos), Direction.UP, placePos, false
        );
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
<<<<<<< HEAD
        InventoryUtility.selectSlot(mc.player, prev);
=======
        mc.player.getInventory().setSelectedSlot(prev);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        state = State.WAITING;
    }
    private void doWait(Minecraft mc, long now) {
        if (now - lastActionTime < 100) return;
        state = State.DETONATE;
    }
    private void doDetonate(Minecraft mc, long now) {
        if (now - lastActionTime < 50) return;
        lastActionTime = now;
        if (bedPos == null) { state = State.IDLE; return; }
        BlockState st = mc.level.getBlockState(bedPos);
        if (!st.is(Blocks.RED_BED) && !st.is(Blocks.WHITE_BED)) {
            boolean isBed = st.getBlock() instanceof BedBlock;
            if (!isBed) { state = State.IDLE; return; }
        }
        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(bedPos), Direction.UP, bedPos, false
        );
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
        state = State.IDLE;
    }
    private net.minecraft.world.entity.LivingEntity findNearestEnemy(Minecraft mc) {
        double maxDist = targetRange.getValue();
        net.minecraft.world.entity.LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof net.minecraft.world.entity.player.Player p)) continue;
            if (p == mc.player) continue;
            if (p.isDeadOrDying()) continue;
            double dist = mc.player.distanceTo(p);
            if (dist > maxDist) continue;
            if (dist < closestDist) {
                closestDist = dist;
                closest = p;
            }
        }
        return closest;
    }
    private BlockPos findPlacePos(Minecraft mc, BlockPos near) {
        double r = range.getValue();
        Vec3 eye = mc.player.getEyePosition();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos pos = near.offset(dx, dy, dz);
                    if (pos.distToCenterSqr(eye) > r * r) continue;
                    BlockState below = mc.level.getBlockState(pos.below());
                    BlockState target = mc.level.getBlockState(pos);
                    BlockState above = mc.level.getBlockState(pos.above());
                    if (below.isCollisionShapeFullBlock(mc.level, pos.below())
                        && target.isAir() && above.isAir()) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
    private int findBedSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
<<<<<<< HEAD
            var stack = InventoryUtility.getItem(mc.player, i);
            if (stack.getItem() instanceof net.minecraft.world.item.BedItem) return i;
        }
        for (int i = 9; i < 36; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (stack.getItem() instanceof net.minecraft.world.item.BedItem) {
                int free = findEmptySlot(mc);
                if (free != -1) {
                    InventoryUtility.selectSlot(mc.player, free);
=======
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof net.minecraft.world.item.BedItem) return i;
        }
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof net.minecraft.world.item.BedItem) {
                int free = findEmptySlot(mc);
                if (free != -1) {
                    mc.player.getInventory().setSelectedSlot(free);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                    mc.gameMode.handleInventoryMouseClick(
                        mc.player.containerMenu.containerId, i, free,
                        net.minecraft.world.inventory.ClickType.SWAP, mc.player
                    );
                    return free;
                }
            }
        }
        return -1;
    }
    private int findEmptySlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
<<<<<<< HEAD
            if (InventoryUtility.getItem(mc.player, i).isEmpty()) return i;
=======
            if (mc.player.getInventory().getItem(i).isEmpty()) return i;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
        return -1;
    }
    private static native void nativeFindBestPlace(double px, double py, double pz, double ex, double ey, double ez, double range, double[] out);
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(BedBomb.class);
    }
    public static BedBomb itz() {
        return ModuleManager.get(BedBomb.class);
    }

}
=======
}
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
