package ravex.modules.combat;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
=======
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import ravex.parameter.ColorParameter;
<<<<<<< HEAD
import ravex.utility.player.InventoryUtility;
import ravex.utility.misc.block.BlockUtility;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
public class HoleFill extends Module {
<<<<<<< HEAD
=======
    public static final HoleFill INSTANCE = new HoleFill();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter range = new NumberParameter("Range", 4.0, 2.0, 8.0, 0.5);
    public final NumberParameter delay = new NumberParameter("Delay", 80, 20, 300, 10);
    public final NumberParameter maxBlocks = new NumberParameter("MaxBlocks", 6, 1, 24, 1);
    public final BooleanParameter fillAll = new BooleanParameter("FillAll", false);
    public final BooleanParameter autoDisable = new BooleanParameter("AutoDisable", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3F00FF00);
<<<<<<< HEAD
    public static List<Long> holePositions = new ArrayList<>();
=======
    public static List<BlockPos> holePositions = new ArrayList<>();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_holefill");
    static {
        NATIVE.load();
    }
    private enum State { IDLE, SEARCH, PLACING, DONE }
    private State state = State.IDLE;
<<<<<<< HEAD
    private List<Long> holes = new ArrayList<>();
=======
    private List<BlockPos> holes = new ArrayList<>();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private int holeIndex = 0;
    private long lastActionTime = 0;
    private int totalPlaced = 0;

    @Override
    protected void onEnable() {
        state = State.IDLE;
        holes.clear();
        holeIndex = 0;
        totalPlaced = 0;
    }
    @Override
    protected void onDisable() {
        state = State.IDLE;
        holes.clear();
        holeIndex = 0;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        long now = System.currentTimeMillis();
        switch (state) {
            case IDLE -> state = State.SEARCH;
            case SEARCH -> searchHoles(mc);
            case PLACING -> placeNext(mc, now);
            case DONE -> {
                if (autoDisable.getValue()) setEnabled(false);
                else state = State.IDLE;
            }
        }
    }
    private void searchHoles(Minecraft mc) {
        holes.clear();
        holePositions.clear();
        holeIndex = 0;
        double r = range.getValue();
        if (NATIVE.isLoaded()) {
            searchNative(mc, r);
        } else {
            searchJava(mc, r);
        }
        if (holes.isEmpty()) {
            state = State.DONE;
            return;
        }
<<<<<<< HEAD
        var playerPos = mc.player.blockPosition();
        int px = playerPos.getX(), py = playerPos.getY(), pz = playerPos.getZ();
        holes.sort(Comparator.comparingDouble(p -> {
            double dx = BlockUtility.unpackX(p) - px;
            double dy = BlockUtility.unpackY(p) - py;
            double dz = BlockUtility.unpackZ(p) - pz;
            return dx * dx + dy * dy + dz * dz;
        }));
=======
        BlockPos playerPos = mc.player.blockPosition();
        holes.sort(Comparator.comparingDouble(p -> p.distSqr(playerPos)));
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        int max = maxBlocks.getValue().intValue();
        if (holes.size() > max) holes = holes.subList(0, max);
        holePositions.clear();
        holePositions.addAll(holes);
        sendMsg(mc, "Found " + holes.size() + " hole(s)");
        state = State.PLACING;
    }
    private void searchNative(Minecraft mc, double range) {
        int[] result = nativeFindHoles(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            range, maxBlocks.getValue().intValue() * 2
        );
        if (result == null) return;
        for (int i = 0; i < result.length; i += 3) {
<<<<<<< HEAD
            long packed = BlockUtility.packPos(result[i], result[i + 1], result[i + 2]);
            if (isValidHole(mc, result[i], result[i + 1], result[i + 2])) {
                holes.add(packed);
=======
            BlockPos pos = new BlockPos(result[i], result[i+1], result[i+2]);
            if (isValidHole(mc, pos)) {
                holes.add(pos);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            }
        }
    }
    private void searchJava(Minecraft mc, double range) {
<<<<<<< HEAD
        var playerPos = mc.player.blockPosition();
        int px = playerPos.getX(), py = playerPos.getY(), pz = playerPos.getZ();
=======
        BlockPos playerPos = mc.player.blockPosition();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        int r = (int) Math.ceil(range);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > range * range) continue;
                for (int dy = -2; dy <= 1; dy++) {
<<<<<<< HEAD
                    int x = px + dx, y = py + dy, z = pz + dz;
                    if (!isValidHole(mc, x, y, z)) continue;
                    long packed = BlockUtility.packPos(x, y, z);
                    boolean dup = false;
                    for (long existing : holes) {
                        double ex = BlockUtility.unpackX(existing) - x;
                        double ey = BlockUtility.unpackY(existing) - y;
                        double ez = BlockUtility.unpackZ(existing) - z;
                        if (ex * ex + ey * ey + ez * ez < 2.0) {
=======
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    if (!isValidHole(mc, pos)) continue;
                    boolean dup = false;
                    for (BlockPos existing : holes) {
                        if (existing.distSqr(pos) < 2.0) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                            dup = true;
                            break;
                        }
                    }
<<<<<<< HEAD
                    if (!dup) holes.add(packed);
=======
                    if (!dup) holes.add(pos);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                }
            }
        }
    }
<<<<<<< HEAD
    private boolean isValidHole(Minecraft mc, int x, int y, int z) {
        int by = y - 1;
        if (by < mc.level.getMinY()) return false;
        if (!mc.level.getBlockState(BlockUtility.pos(x, y, z)).isAir()) return false;
        if (!BlockUtility.isSolid(mc.level, x, by, z)) return false;
        int ay = y + 1;
        if (ay >= mc.level.getMaxY()) return false;
        if (!BlockUtility.isAir(mc.level, x, ay, z)) return false;
        int solidSides = 0;
        Direction[] horizontals = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction dir : horizontals) {
            int nx = x + dir.getStepX(), ny = y + dir.getStepY(), nz = z + dir.getStepZ();
            if (!mc.level.getWorldBorder().isWithinBounds(BlockUtility.pos(nx, ny, nz))) return false;
            if (BlockUtility.isSolid(mc.level, nx, ny, nz)) {
                solidSides++;
=======
    private boolean isValidHole(Minecraft mc, BlockPos pos) {
        BlockPos below = pos.below();
        if (below.getY() < mc.level.getMinY()) return false;
        if (!mc.level.getBlockState(pos).isAir()) return false;
        BlockState floorState = mc.level.getBlockState(below);
        if (!floorState.isCollisionShapeFullBlock(mc.level, below)) return false;
        BlockPos above = pos.above();
        if (above.getY() >= mc.level.getMaxY()) return false;
        if (!mc.level.getBlockState(above).isAir()) return false;
        int solidSides = 0;
        Direction[] horizontals = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction dir : horizontals) {
            BlockPos neighbor = pos.relative(dir);
            if (!mc.level.getWorldBorder().isWithinBounds(neighbor)) return false;
            BlockState neighborState = mc.level.getBlockState(neighbor);
            if (neighborState.isCollisionShapeFullBlock(mc.level, neighbor)) {
                solidSides++;
            } else {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            }
        }
        if (fillAll.getValue()) {
            return solidSides >= 2;
        }
        return solidSides >= 3;
    }
    private void placeNext(Minecraft mc, long now) {
        if (now - lastActionTime < delay.getValue().longValue()) return;
        lastActionTime = now;
        if (holeIndex >= holes.size()) {
            sendMsg(mc, "Filled " + totalPlaced + " block(s)");
            state = State.DONE;
            return;
        }
<<<<<<< HEAD
        long targetPacked = holes.get(holeIndex);
        if (!BlockUtility.isAir(mc.level, BlockUtility.unpackX(targetPacked), BlockUtility.unpackY(targetPacked), BlockUtility.unpackZ(targetPacked))) {
=======
        BlockPos target = holes.get(holeIndex);
        BlockState existing = mc.level.getBlockState(target);
        if (!existing.isAir()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            holeIndex++;
            return;
        }
        int slot = findBlockSlot(mc);
        if (slot == -1) {
            sendMsg(mc, "Not enough blocks, disabling");
            setEnabled(false);
            return;
        }
<<<<<<< HEAD
        if (!BlockUtility.placeBlock(mc, BlockUtility.fromPacked(targetPacked), slot)) {
            holeIndex++;
            return;
        }
        totalPlaced++;
        holeIndex++;
    }
    private int findBlockSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (InventoryUtility.isItem(stack, "obsidian") || InventoryUtility.isItem(stack, "crying_obsidian")) return i;
        }
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (InventoryUtility.isBlockItem(stack)) return i;
=======
        int prev = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        BlockHitResult hit = findPlaceTarget(mc, target);
        if (hit == null) {
            mc.player.getInventory().setSelectedSlot(prev);
            holeIndex++;
            return;
        }
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
        mc.player.getInventory().setSelectedSlot(prev);
        totalPlaced++;
        holeIndex++;
    }
    private BlockHitResult findPlaceTarget(Minecraft mc, BlockPos target) {
        Vec3 eye = mc.player.getEyePosition();
        BlockPos bestNeighbor = null;
        Direction bestFace = Direction.UP;
        double bestDist = Double.MAX_VALUE;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = target.relative(dir);
            BlockState st = mc.level.getBlockState(neighbor);
            if (st.isCollisionShapeFullBlock(mc.level, neighbor)) {
                double d = neighbor.distToCenterSqr(eye);
                if (d < bestDist) {
                    bestDist = d;
                    bestNeighbor = neighbor;
                    bestFace = dir.getOpposite();
                }
            }
        }
        if (bestNeighbor != null) {
            Vec3 hitVec = Vec3.atCenterOf(bestNeighbor)
                .add(new Vec3(bestFace.getStepX(), bestFace.getStepY(), bestFace.getStepZ()).scale(0.5));
            return new BlockHitResult(hitVec, bestFace, bestNeighbor, false);
        }
        if (target.distToCenterSqr(eye) < 36.0) {
            return new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
        }
        return null;
    }
    private int findBlockSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(Items.OBSIDIAN) || stack.is(Items.CRYING_OBSIDIAN)) return i;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) return i;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
        return -1;
    }
    private void sendMsg(Minecraft mc, String msg) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§8[§5HoleFill§8] §7" + msg), false);
        }
    }
    private static native int[] nativeFindHoles(
        double px, double py, double pz, double range, int maxResults);
<<<<<<< HEAD

    public static boolean maybeEnabled() {
        return maybeEnabled(HoleFill.class);
    }
    public static HoleFill itz() {
        return ModuleManager.get(HoleFill.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
