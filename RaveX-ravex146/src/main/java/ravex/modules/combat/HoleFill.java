package ravex.modules.combat;

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
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.misc.NativeLoader;
import ravex.parameter.ColorParameter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HoleFill extends Module {
    public static final HoleFill INSTANCE = new HoleFill();

    public final NumberParameter range = new NumberParameter("Range", 4.0, 2.0, 8.0, 0.5);
    public final NumberParameter delay = new NumberParameter("Delay", 80, 20, 300, 10);
    public final NumberParameter maxBlocks = new NumberParameter("Max Blocks", 6, 1, 24, 1);
    public final BooleanParameter fillAll = new BooleanParameter("Fill All", false);
    public final BooleanParameter autoDisable = new BooleanParameter("Auto Disable", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3F00FF00);

    public static List<BlockPos> holePositions = new ArrayList<>();

    private static boolean nativeAvailable = false;
    static {
        try {
            nativeAvailable = NativeLoader.loadLibrary("ravex_holefill");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[HoleFill JNI] " + e.getMessage());
        }
    }

    private enum State { IDLE, SEARCH, PLACING, DONE }
    private State state = State.IDLE;
    private List<BlockPos> holes = new ArrayList<>();
    private int holeIndex = 0;
    private long lastActionTime = 0;
    private int totalPlaced = 0;

    private HoleFill() {
        super("HoleFill", Category.COMBAT);
        addParameter(range);
        addParameter(delay);
        addParameter(maxBlocks);
        addParameter(fillAll);
        addParameter(autoDisable);
        addParameter(render);
        addParameter(color);
    }

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

        if (nativeAvailable) {
            searchNative(mc, r);
        } else {
            searchJava(mc, r);
        }

        if (holes.isEmpty()) {
            state = State.DONE;
            return;
        }


        BlockPos playerPos = mc.player.blockPosition();
        holes.sort(Comparator.comparingDouble(p -> p.distSqr(playerPos)));


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
            BlockPos pos = new BlockPos(result[i], result[i+1], result[i+2]);
            if (isValidHole(mc, pos)) {
                holes.add(pos);
            }
        }
    }

    private void searchJava(Minecraft mc, double range) {
        BlockPos playerPos = mc.player.blockPosition();
        int r = (int) Math.ceil(range);

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {

                if (dx * dx + dz * dz > range * range) continue;

                for (int dy = -2; dy <= 1; dy++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    if (!isValidHole(mc, pos)) continue;


                    boolean dup = false;
                    for (BlockPos existing : holes) {
                        if (existing.distSqr(pos) < 2.0) {
                            dup = true;
                            break;
                        }
                    }
                    if (!dup) holes.add(pos);
                }
            }
        }
    }

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

        BlockPos target = holes.get(holeIndex);
        BlockState existing = mc.level.getBlockState(target);
        if (!existing.isAir()) {
            holeIndex++;
            return;
        }

        int slot = findBlockSlot(mc);
        if (slot == -1) {
            sendMsg(mc, "Not enough blocks, disabling");
            setEnabled(false);
            return;
        }

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
}
