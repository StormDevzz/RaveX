package ravex.modules.world;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.network.chat.Component;

import ravex.utility.player.InventoryUtility;
import ravex.utility.misc.block.BlockUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "AutoWither", category = "World")
public class AutoWither {
    @Parameter(name = "Count", min = 1.0, max = 12.0, step = 1.0)
    public double count = 1.0;
    @Parameter(name = "AutoDisable")
    public boolean autoDisable = true;
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
    public void onEnable() {
        state = State.IDLE;
        hasBase = false;
        buildIndex = 0;
        retries = 0;
        hasFailed = false;
        buildsCompleted = 0;
    }
    public void onDisable() {
        state = State.IDLE;
        hasBase = false;
        buildIndex = 0;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        long now = System.currentTimeMillis();
        switch (state) {
            case IDLE -> findPosition(mc);
            case BUILDING -> tryPlaceNext(mc, now);
            case RETRY -> retryPlace(mc, now);
            case DONE -> doDone(mc);
        }
    }
    private void findPosition(MinecraftWrapper mc) {
        var look = mc.getPlayer().getViewVector(1.0F).normalize();
        var playerPos = mc.getPlayer().blockPosition();
        int ppX = playerPos.getX(), ppY = playerPos.getY(), ppZ = playerPos.getZ();
        for (double d = 3.0; d <= 6.0; d += 0.5) {
            int cx = (int) Math.round(mc.getPlayer().getX() + look.x * d);
            int cy = (int) Math.round(mc.getPlayer().getY() + look.y * d);
            int cz = (int) Math.round(mc.getPlayer().getZ() + look.z * d);
            int groundY = -1;
            for (int y = ppY + 3; y >= ppY - 10; y--) {
                if (y < mc.getLevel().getMinY()) break;
                if (y - 1 < mc.getLevel().getMinY()) break;
                if (BlockUtility.isSolid(mc.getLevel(), cx, y - 1, y - 1)
                    && BlockUtility.isAir(mc.getLevel(), cx, y, cz)) {
                    groundY = y;
                    break;
                }
            }
            if (groundY == -1) continue;
            boolean clear = true;
            for (int[] off : BLOCK_OFFSETS) {
                int ox = cx + off[0], oy = groundY + off[1], oz = cz + off[2];
                var st = BlockUtility.getState(mc.getLevel(), ox, oy, oz);
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
        if (mc.getPlayer().onGround()) {
            int fx = ppX + (int) Math.round(look.x);
            int fz = ppZ + (int) Math.round(look.z);
            int groundY = ppY + 1;
            if (groundY - 1 >= mc.getLevel().getMinY()
                && BlockUtility.isAir(mc.getLevel(), fx, ppY + 1, fz)
                && BlockUtility.isSolid(mc.getLevel(), fx, groundY - 1, fz)) {
                boolean clear = true;
                for (int[] off : BLOCK_OFFSETS) {
                    int ox = fx + off[0], oy = (ppY + 1) + off[1], oz = fz + off[2];
                    var st = BlockUtility.getState(mc.getLevel(), ox, oy, oz);
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
        Modules.setEnabled(AutoWither.class, false);
    }
    private void tryPlaceNext(MinecraftWrapper mc, long now) {
        if (now - lastActionTime < 50) return;
        lastActionTime = now;
        if (!hasBase) { state = State.IDLE; return; }
        if (buildIndex >= BLOCK_OFFSETS.length) {
            state = State.DONE;
            return;
        }
        int[] off = BLOCK_OFFSETS[buildIndex];
        int tx = baseX + off[0], ty = baseY + off[1], tz = baseZ + off[2];
        var existing = BlockUtility.getState(mc.getLevel(), tx, ty, tz);
        if (BlockUtility.isBlock(existing, "soul_sand") || BlockUtility.isBlock(existing, "soul_soil")
            || BlockUtility.isBlock(existing, "wither_skeleton_skull") || BlockUtility.isBlock(existing, "wither_skeleton_wall_skull")) {
            buildIndex++;
            retries = 0;
            return;
        }
        int slot = findItemSlot(mc);
        if (slot == -1) {
            sendMsg(mc, getMissingMsg());
            Modules.setEnabled(AutoWither.class, false);
            return;
        }
        int prev = InventoryUtility.getSelectedSlot(mc.getPlayer());
        InventoryUtility.selectSlot(mc.getPlayer(), slot);
        var hit = BlockUtility.findPlaceTarget(mc, BlockUtility.pos(tx, ty, tz));
        if (hit == null) {
            InventoryUtility.selectSlot(mc.getPlayer(), prev);
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
    private void retryPlace(MinecraftWrapper mc, long now) {
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
        var st = BlockUtility.getState(mc.getLevel(), failX, failY, failZ);
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
            Modules.setEnabled(AutoWither.class, false);
            return;
        }
        int prev = InventoryUtility.getSelectedSlot(mc.getPlayer());
        InventoryUtility.selectSlot(mc.getPlayer(), slot);
        var hit = BlockUtility.findPlaceTarget(mc, BlockUtility.pos(failX, failY, failZ));
        if (hit == null) {
            InventoryUtility.selectSlot(mc.getPlayer(), prev);
            return;
        }
        BlockUtility.useItemOn(mc, hit);
        BlockUtility.swing(mc);
    }
    private void doDone(MinecraftWrapper mc) {
        buildsCompleted++;
        int target = (int) count;
        if (buildsCompleted < target && hasBase) {
            baseX += 5;
            buildIndex = 0;
            retries = 0;
            hasFailed = false;
            state = State.BUILDING;
        } else {
            if (autoDisable) {
                Modules.setEnabled(AutoWither.class, false);
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
    private int findItemSlot(MinecraftWrapper mc) {
        boolean needSand = buildIndex < SOUL_SAND_COUNT;
        for (int i = 0; i < 36; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (stack.isEmpty()) continue;
            if (needSand && (InventoryUtility.isItem(stack, "soul_sand") || InventoryUtility.isItem(stack, "soul_soil"))) {
                if (i < 9) return i;
                int free = InventoryUtility.findEmptyHotbarSlot(mc.getPlayer());
                if (free != -1) {
                    InventoryUtility.selectSlot(mc.getPlayer(), free);
                    InventoryUtility.handleInventoryClick(mc, mc.getPlayer(), i, free, InventoryUtility.SWAP);
                    return free;
                }
                return i;
            }
            if (!needSand && InventoryUtility.isItem(stack, "wither_skeleton_skull")) {
                if (i < 9) return i;
                int free = InventoryUtility.findEmptyHotbarSlot(mc.getPlayer());
                if (free != -1) {
                    InventoryUtility.selectSlot(mc.getPlayer(), free);
                    InventoryUtility.handleInventoryClick(mc, mc.getPlayer(), i, free, InventoryUtility.SWAP);
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
    private void sendMsg(MinecraftWrapper mc, String msg) {
        if (mc.getPlayer() != null) {
            mc.getPlayer().displayClientMessage(Component.literal("§8[§5AutoWither§8] §7" + msg), false);
        }
    }



}