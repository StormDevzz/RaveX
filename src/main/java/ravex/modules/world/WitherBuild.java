package ravex.modules.world;

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

public class WitherBuild extends Module {
    public static final WitherBuild INSTANCE = new WitherBuild();

    public final NumberParameter count = new NumberParameter("Count", 1.0, 1.0, 12.0, 1.0);
    public final BooleanParameter autoDisable = new BooleanParameter("Auto Disable", true);

    private enum State { IDLE, BUILDING, RETRY, DONE }
    private State state = State.IDLE;
    private BlockPos base = null;
    private int buildIndex = 0;
    private int retries = 0;
    private BlockPos lastFailedPos = null;
    private long lastActionTime = 0;
    private int buildsCompleted = 0;

    // T-shape: bottom + row + skulls on top
    private static final int[][] BLOCK_OFFSETS = {
        {1, 0, 0},     // bottom center (soul sand)
        {0, 1, 0},     // top-left (soul sand)
        {1, 1, 0},     // top-center (soul sand)
        {2, 1, 0},     // top-right (soul sand)
        {0, 2, 0},     // left skull
        {2, 2, 0},     // right skull
        {1, 2, 0},     // center skull — placed last (spawns wither)
    };

    private static final int SOUL_SAND_COUNT = 4;

    private WitherBuild() {
        super("WitherBuild", Category.WORLD);
        addParameter(count);
        addParameter(autoDisable);
    }

    @Override
    protected void onEnable() {
        state = State.IDLE;
        base = null;
        buildIndex = 0;
        retries = 0;
        lastFailedPos = null;
        buildsCompleted = 0;
    }

    @Override
    protected void onDisable() {
        state = State.IDLE;
        base = null;
        buildIndex = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        long now = System.currentTimeMillis();

        switch (state) {
            case IDLE -> findPosition(mc);
            case BUILDING -> tryPlaceNext(mc, now);
            case RETRY -> retryPlace(mc, now);
            case DONE -> doDone(mc);
        }
    }

    private void findPosition(Minecraft mc) {
        Vec3 look = mc.player.getViewVector(1.0F).normalize();
        BlockPos playerPos = mc.player.blockPosition();

        // Try positions in front of player (3-6 blocks away)
        for (double d = 3.0; d <= 6.0; d += 0.5) {
            BlockPos candidate = BlockPos.containing(
                mc.player.getX() + look.x * d,
                mc.player.getY() + look.y * d,
                mc.player.getZ() + look.z * d
            );

            // Find ground from playerY+3 down
            BlockPos ground = null;
            for (int y = playerPos.getY() + 3; y >= playerPos.getY() - 10; y--) {
                if (y < mc.level.getMinY()) break;
                BlockPos check = BlockPos.containing(candidate.getX(), y, candidate.getZ());
                BlockPos below = check.below();
                if (below.getY() < mc.level.getMinY()) break;
                if (mc.level.getBlockState(below).isCollisionShapeFullBlock(mc.level, below)
                    && mc.level.getBlockState(check).isAir()) {
                    ground = check;
                    break;
                }
            }
            if (ground == null) continue;

            // Validate all positions are clear
            boolean clear = true;
            for (int[] off : BLOCK_OFFSETS) {
                BlockPos p = ground.offset(off[0], off[1], off[2]);
                BlockState st = mc.level.getBlockState(p);
                if (!st.isAir() && !st.is(Blocks.SOUL_SAND) && !st.is(Blocks.SOUL_SOIL)
                    && !st.is(Blocks.WITHER_SKELETON_SKULL) && !st.is(Blocks.WITHER_SKELETON_WALL_SKULL)) {
                    clear = false;
                    break;
                }
            }
            if (!clear) continue;

            base = ground;
            buildIndex = 0;
            retries = 0;
            state = State.BUILDING;
            return;
        }

        // If on ground, try directly in front 3 blocks
        if (mc.player.onGround()) {
            BlockPos feet = playerPos;
            BlockPos inFront = feet.offset((int)Math.round(look.x), 0, (int)Math.round(look.z)).above();
            BlockPos ground = inFront.below();
            if (ground.getY() >= mc.level.getMinY()
                && mc.level.getBlockState(inFront).isAir()
                && mc.level.getBlockState(ground).isCollisionShapeFullBlock(mc.level, ground)) {

                boolean clear = true;
                for (int[] off : BLOCK_OFFSETS) {
                    BlockPos p = inFront.offset(off[0], off[1], off[2]);
                    BlockState st = mc.level.getBlockState(p);
                    if (!st.isAir() && !st.is(Blocks.SOUL_SAND) && !st.is(Blocks.SOUL_SOIL)
                        && !st.is(Blocks.WITHER_SKELETON_SKULL) && !st.is(Blocks.WITHER_SKELETON_WALL_SKULL)) {
                        clear = false;
                        break;
                    }
                }
                if (clear) {
                    base = inFront;
                    buildIndex = 0;
                    retries = 0;
                    state = State.BUILDING;
                    return;
                }
            }
        }

        sendMsg(mc, "No suitable position found");
        setEnabled(false);
    }

    private void tryPlaceNext(Minecraft mc, long now) {
        if (now - lastActionTime < 50) return;
        lastActionTime = now;

        if (base == null) { state = State.IDLE; return; }
        if (buildIndex >= BLOCK_OFFSETS.length) {
            state = State.DONE;
            return;
        }

        int[] off = BLOCK_OFFSETS[buildIndex];
        BlockPos targetPos = base.offset(off[0], off[1], off[2]);

        BlockState existing = mc.level.getBlockState(targetPos);
        if (existing.is(Blocks.SOUL_SAND) || existing.is(Blocks.SOUL_SOIL)
            || existing.is(Blocks.WITHER_SKELETON_SKULL) || existing.is(Blocks.WITHER_SKELETON_WALL_SKULL)) {
            buildIndex++;
            retries = 0;
            return;
        }

        int slot = findItemSlot(mc);
        if (slot == -1) {
            sendMsg(mc, getMissingMsg());
            setEnabled(false);
            return;
        }

        int prev = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);

        BlockHitResult hit = findPlaceTarget(mc, targetPos);
        if (hit == null) {
            mc.player.getInventory().setSelectedSlot(prev);
            lastFailedPos = targetPos;
            retries = 0;
            state = State.RETRY;
            return;
        }

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
        // skip slot restore for speed, do it after completion

        lastActionTime = now;
        retries = 0;
        buildIndex++;
    }

    private void retryPlace(Minecraft mc, long now) {
        if (now - lastActionTime < 100) return;
        lastActionTime = now;

        if (lastFailedPos == null) { state = State.BUILDING; return; }

        retries++;
        if (retries > 5) {
            buildIndex++;
            retries = 0;
            lastFailedPos = null;
            state = State.BUILDING;
            return;
        }

        BlockState st = mc.level.getBlockState(lastFailedPos);
        if (st.is(Blocks.SOUL_SAND) || st.is(Blocks.SOUL_SOIL)
            || st.is(Blocks.WITHER_SKELETON_SKULL) || st.is(Blocks.WITHER_SKELETON_WALL_SKULL)) {
            buildIndex++;
            retries = 0;
            lastFailedPos = null;
            state = State.BUILDING;
            return;
        }

        int slot = findItemSlot(mc);
        if (slot == -1) {
            sendMsg(mc, getMissingMsg());
            setEnabled(false);
            return;
        }

        int prev = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);

        BlockHitResult hit = findPlaceTarget(mc, lastFailedPos);
        if (hit == null) {
            mc.player.getInventory().setSelectedSlot(prev);
            return;
        }

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void doDone(Minecraft mc) {
        buildsCompleted++;
        int target = count.getValue().intValue();

        if (buildsCompleted < target && base != null) {
            base = base.offset(5, 0, 0);
            buildIndex = 0;
            retries = 0;
            lastFailedPos = null;
            state = State.BUILDING;
        } else {
            if (autoDisable.getValue()) {
                setEnabled(false);
            } else {
                state = State.IDLE;
            }
        }
    }

    private static boolean isAirOrWitherBlock(BlockState state) {
        return state.isAir() || state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL)
            || state.is(Blocks.WITHER_SKELETON_SKULL) || state.is(Blocks.WITHER_SKELETON_WALL_SKULL);
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

    private int findItemSlot(Minecraft mc) {
        boolean needSand = buildIndex < SOUL_SAND_COUNT;
        // Search hotbar + full inventory
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (needSand && (stack.is(Items.SOUL_SAND) || stack.is(Items.SOUL_SOIL))) {
                if (i < 9) return i;
                int free = -1;
                for (int j = 0; j < 9; j++) {
                    if (mc.player.getInventory().getItem(j).isEmpty()) { free = j; break; }
                }
                if (free != -1) {
                    mc.player.getInventory().setSelectedSlot(free);
                    mc.gameMode.handleInventoryMouseClick(
                        mc.player.containerMenu.containerId, i, free,
                        net.minecraft.world.inventory.ClickType.SWAP, mc.player);
                    return free;
                }
                return i; // can't swap, return the slot anyway
            }
            if (!needSand && stack.is(Items.WITHER_SKELETON_SKULL)) {
                if (i < 9) return i;
                int free = -1;
                for (int j = 0; j < 9; j++) {
                    if (mc.player.getInventory().getItem(j).isEmpty()) { free = j; break; }
                }
                if (free != -1) {
                    mc.player.getInventory().setSelectedSlot(free);
                    mc.gameMode.handleInventoryMouseClick(
                        mc.player.containerMenu.containerId, i, free,
                        net.minecraft.world.inventory.ClickType.SWAP, mc.player);
                    return free;
                }
            }
        }
        return -1;
    }

    private String getMissingMsg() {
        return buildIndex < SOUL_SAND_COUNT
            ? "Not enough soul sand/soil"
            : "Not enough wither skeleton skulls";
    }

    private void sendMsg(Minecraft mc, String msg) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§8[§5WitherBuild§8] §7" + msg), false);
        }
    }
}
