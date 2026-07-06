package ravex.modules.world;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.utility.player.InventoryUtility;
import ravex.utility.misc.block.BlockUtility;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
public class AutoWither extends Module {
    public static final AutoWither INSTANCE = new AutoWither();
    public final NumberParameter count = new NumberParameter("Count", 1.0, 1.0, 12.0, 1.0);
    public final BooleanParameter autoDisable = new BooleanParameter("AutoDisable", true);
    private enum State { IDLE, BUILDING, RETRY, DONE }
    private State state = State.IDLE;
    private int baseX, baseY, baseZ;
    private boolean hasBase;
    private int buildIndex = 0;
    private int retries = 0;
    private int failX, failY, failZ;
    private boolean hasFailed;
    private long lastActionTime = 0;
    private int buildsCompleted = 0;
    private static final int[][] BLOCK_OFFSETS = {
        {1, 0, 0}, {0, 1, 0}, {1, 1, 0}, {2, 1, 0}, {0, 2, 0}, {2, 2, 0}, {1, 2, 0},
    };
    private static final int SOUL_SAND_COUNT = 4;

    @Override
    protected void onEnable() {
        state = State.IDLE;
        hasBase = false;
        buildIndex = 0;
        retries = 0;
        hasFailed = false;
        buildsCompleted = 0;
    }
    @Override
    protected void onDisable() {
        state = State.IDLE;
        hasBase = false;
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
        var look = mc.player.getViewVector(1.0F).normalize();
        var playerPos = mc.player.blockPosition();
        int ppX = playerPos.getX(), ppY = playerPos.getY(), ppZ = playerPos.getZ();
        for (double d = 3.0; d <= 6.0; d += 0.5) {
            int cx = (int) Math.round(mc.player.getX() + look.x * d);
            int cy = (int) Math.round(mc.player.getY() + look.y * d);
            int cz = (int) Math.round(mc.player.getZ() + look.z * d);
            int groundY = -1;
            for (int y = ppY + 3; y >= ppY - 10; y--) {
                if (y < mc.level.getMinY()) break;
                if (y - 1 < mc.level.getMinY()) break;
                if (BlockUtility.isSolid(mc.level, cx, y - 1, y - 1)
                    && BlockUtility.isAir(mc.level, cx, y, cz)) {
                    groundY = y;
                    break;
                }
            }
            if (groundY == -1) continue;
            boolean clear = true;
            for (int[] off : BLOCK_OFFSETS) {
                int ox = cx + off[0], oy = groundY + off[1], oz = cz + off[2];
                var st = BlockUtility.getState(mc.level, ox, oy, oz);
                if (!st.isAir() && !BlockUtility.isBlock(st, "soul_sand") && !BlockUtility.isBlock(st, "soul_soil")
                    && !BlockUtility.isBlock(st, "wither_skeleton_skull") && !BlockUtility.isBlock(st, "wither_skeleton_wall_skull")) {
                    clear = false;
                    break;
                }
            }
            if (!clear) continue;
            baseX = cx; baseY = groundY; baseZ = cz;
            hasBase = true;
            buildIndex = 0;
            retries = 0;
            state = State.BUILDING;
            return;
        }
        if (mc.player.onGround()) {
            int fx = ppX + (int) Math.round(look.x);
            int fz = ppZ + (int) Math.round(look.z);
            int groundY = ppY + 1;
            if (groundY - 1 >= mc.level.getMinY()
                && BlockUtility.isAir(mc.level, fx, ppY + 1, fz)
                && BlockUtility.isSolid(mc.level, fx, groundY - 1, fz)) {
                boolean clear = true;
                for (int[] off : BLOCK_OFFSETS) {
                    int ox = fx + off[0], oy = (ppY + 1) + off[1], oz = fz + off[2];
                    var st = BlockUtility.getState(mc.level, ox, oy, oz);
                    if (!st.isAir() && !BlockUtility.isBlock(st, "soul_sand") && !BlockUtility.isBlock(st, "soul_soil")
                        && !BlockUtility.isBlock(st, "wither_skeleton_skull") && !BlockUtility.isBlock(st, "wither_skeleton_wall_skull")) {
                        clear = false;
                        break;
                    }
                }
                if (clear) {
                    baseX = fx; baseY = ppY + 1; baseZ = fz;
                    hasBase = true;
                    buildIndex = 0;
                    retries = 0;
                    state = State.BUILDING;
                    return;
                }
            }
        }
        sendMsg(mc, "NoSuitablePositionFound");
        setEnabled(false);
    }
    private void tryPlaceNext(Minecraft mc, long now) {
        if (now - lastActionTime < 50) return;
        lastActionTime = now;
        if (!hasBase) { state = State.IDLE; return; }
        if (buildIndex >= BLOCK_OFFSETS.length) {
            state = State.DONE;
            return;
        }
        int[] off = BLOCK_OFFSETS[buildIndex];
        int tx = baseX + off[0], ty = baseY + off[1], tz = baseZ + off[2];
        var existing = BlockUtility.getState(mc.level, tx, ty, tz);
        if (BlockUtility.isBlock(existing, "soul_sand") || BlockUtility.isBlock(existing, "soul_soil")
            || BlockUtility.isBlock(existing, "wither_skeleton_skull") || BlockUtility.isBlock(existing, "wither_skeleton_wall_skull")) {
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
        int prev = InventoryUtility.getSelectedSlot(mc.player);
        InventoryUtility.selectSlot(mc.player, slot);
        var hit = BlockUtility.findPlaceTarget(mc, BlockUtility.pos(tx, ty, tz));
        if (hit == null) {
            InventoryUtility.selectSlot(mc.player, prev);
            failX = tx; failY = ty; failZ = tz;
            hasFailed = true;
            retries = 0;
            state = State.RETRY;
            return;
        }
        BlockUtility.useItemOn(mc, hit);
        BlockUtility.swing(mc);
        lastActionTime = now;
        retries = 0;
        buildIndex++;
    }
    private void retryPlace(Minecraft mc, long now) {
        if (now - lastActionTime < 100) return;
        lastActionTime = now;
        if (!hasFailed) { state = State.BUILDING; return; }
        retries++;
        if (retries > 5) {
            buildIndex++;
            retries = 0;
            hasFailed = false;
            state = State.BUILDING;
            return;
        }
        var st = BlockUtility.getState(mc.level, failX, failY, failZ);
        if (BlockUtility.isBlock(st, "soul_sand") || BlockUtility.isBlock(st, "soul_soil")
            || BlockUtility.isBlock(st, "wither_skeleton_skull") || BlockUtility.isBlock(st, "wither_skeleton_wall_skull")) {
            buildIndex++;
            retries = 0;
            hasFailed = false;
            state = State.BUILDING;
            return;
        }
        int slot = findItemSlot(mc);
        if (slot == -1) {
            sendMsg(mc, getMissingMsg());
            setEnabled(false);
            return;
        }
        int prev = InventoryUtility.getSelectedSlot(mc.player);
        InventoryUtility.selectSlot(mc.player, slot);
        var hit = BlockUtility.findPlaceTarget(mc, BlockUtility.pos(failX, failY, failZ));
        if (hit == null) {
            InventoryUtility.selectSlot(mc.player, prev);
            return;
        }
        BlockUtility.useItemOn(mc, hit);
        BlockUtility.swing(mc);
    }
    private void doDone(Minecraft mc) {
        buildsCompleted++;
        int target = count.getValue().intValue();
        if (buildsCompleted < target && hasBase) {
            baseX += 5;
            buildIndex = 0;
            retries = 0;
            hasFailed = false;
            state = State.BUILDING;
        } else {
            if (autoDisable.getValue()) {
                setEnabled(false);
            } else {
                state = State.IDLE;
            }
        }
    }
    private static boolean isAirOrWitherBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.isAir() || BlockUtility.isBlock(state, "soul_sand")
            || BlockUtility.isBlock(state, "soul_soil")
            || BlockUtility.isBlock(state, "wither_skeleton_skull")
            || BlockUtility.isBlock(state, "wither_skeleton_wall_skull");
    }
    private int findItemSlot(Minecraft mc) {
        boolean needSand = buildIndex < SOUL_SAND_COUNT;
        for (int i = 0; i < 36; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (stack.isEmpty()) continue;
            if (needSand && (InventoryUtility.isItem(stack, "soul_sand") || InventoryUtility.isItem(stack, "soul_soil"))) {
                if (i < 9) return i;
                int free = InventoryUtility.findEmptyHotbarSlot(mc.player);
                if (free != -1) {
                    InventoryUtility.selectSlot(mc.player, free);
                    InventoryUtility.handleInventoryClick(mc, mc.player, i, free, net.minecraft.world.inventory.ClickType.SWAP);
                    return free;
                }
                return i;
            }
            if (!needSand && InventoryUtility.isItem(stack, "wither_skeleton_skull")) {
                if (i < 9) return i;
                int free = InventoryUtility.findEmptyHotbarSlot(mc.player);
                if (free != -1) {
                    InventoryUtility.selectSlot(mc.player, free);
                    InventoryUtility.handleInventoryClick(mc, mc.player, i, free, net.minecraft.world.inventory.ClickType.SWAP);
                    return free;
                }
            }
        }
        return -1;
    }
    private String getMissingMsg() {
        return buildIndex < SOUL_SAND_COUNT
            ? "NotEnoughSoulSand/soil"
            : "NotEnoughWitherSkeletonSkulls";
    }
    private void sendMsg(Minecraft mc, String msg) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§8[§5AutoWither§8] §7" + msg), false);
        }
    }
}
