package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;

import ravex.utility.player.InventoryUtility;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.GameModeUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.utility.client.ClientAlertUtility;
import org.jetbrains.annotations.Nullable;

@Module(name = "AutoWither", category = "World")
public class AutoWither {
    @Parameter(name = "Swap", modes = {"None", "Normal", "Silent"})
    public String swapMode = "Normal";
    @Parameter(name = "AutoDisable")
    public boolean autoDisable = true;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true, visible = "render")
    public int color = 0xFF40A9F8;

    private enum State { IDLE, BUILDING, DONE }
    private State state = State.IDLE;
    private int baseX, baseY, baseZ;
    private boolean hasBase;
    private int buildIndex = 0;
    private int retries = 0;
    private long lastActionTime = 0;
    private boolean positionNotified = false;

    // sent placement not confirmed locally yet
    private boolean hasPending = false;
    private int pendingX, pendingY, pendingZ;
    private long pendingSince = 0;

    // order matters, each cell needs a clickable neighbour
    private static final int[][] PLACE_OFFSETS = {
        {1, 0, 0},   // stem
        {1, 1, 0},   // soul sand row, on the stem
        {0, 1, 0},
        {2, 1, 0},
        {0, 2, 0},   // skulls on top
        {2, 2, 0},
        {1, 2, 0},
    };
    // pattern cells that must be free to spawn
    private static final int[][] CLEARANCE_OFFSETS = {
        {1, 0, 0}, {0, 1, 0}, {1, 1, 0}, {2, 1, 0}, {0, 2, 0}, {2, 2, 0}, {1, 2, 0},
        {0, 0, 0}, {2, 0, 0},
    };
    public static final int[][] RENDER_OFFSETS = PLACE_OFFSETS;
    private static final int SOUL_SAND_COUNT = 4;
    private static final long PLACE_INTERVAL_MS = 50L;
    private static final long PENDING_TIMEOUT_MS = 1000L;
    private static final int MAX_RETRIES = 4;
    private static final long LINGER_MS = 1400L;

    private final long[] placedAt = new long[PLACE_OFFSETS.length];
    private long lingerUntil = 0L;

    public boolean hasRenderBase() {
        return hasBase && (state == State.BUILDING || System.currentTimeMillis() < lingerUntil);
    }
    public int getBaseX() { return baseX; }
    public int getBaseY() { return baseY; }
    public int getBaseZ() { return baseZ; }
    public int getBuildIndex() { return buildIndex; }
    public int getCellCount() { return PLACE_OFFSETS.length; }
    public long getPlacedAt(int index) {
        return index >= 0 && index < placedAt.length ? placedAt[index] : 0L;
    }

    // placed cells settle, next cell breathes
    public float getCellFillAlpha(int index, long now) {
        long placed = getPlacedAt(index);
        if (placed > 0L) {
            float t = Math.min(1f, (now - placed) / (float) LINGER_MS);
            float ease = t * t * (3f - 2f * t);
            return 0.30f * (1f - ease) + 0.10f * ease;
        }
        if (index == buildIndex) {
            float breathe = 0.5f + 0.5f * (float) Math.sin(now * 0.0045);
            return 0.13f + 0.09f * breathe;
        }
        return 0.06f;
    }

    public float getCellLineAlpha(int index, long now) {
        long placed = getPlacedAt(index);
        if (placed > 0L) {
            float t = Math.min(1f, (now - placed) / (float) LINGER_MS);
            float ease = t * t * (3f - 2f * t);
            return 0.95f * (1f - ease) + 0.45f * ease;
        }
        if (index == buildIndex) {
            float breathe = 0.5f + 0.5f * (float) Math.sin(now * 0.0045);
            return 0.5f + 0.35f * breathe;
        }
        return 0.22f;
    }

    public void onEnable() {
        resetBuild();
        state = State.IDLE;
        hasBase = false;
        positionNotified = false;
        lingerUntil = 0L;
    }

    public void onDisable() {
        state = State.IDLE;
        hasPending = false;
        if (System.currentTimeMillis() >= lingerUntil) {
            hasBase = false;
        }
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        if (GameModeUtility.isLocalSpectator()) {
            reportMissing(mc);
            return;
        }
        long now = System.currentTimeMillis();
        switch (state) {
            case IDLE -> {
                // wither needs 4 soul sand and 3 skulls
                if (!GameModeUtility.isLocalCreative() && !GameModeUtility.isLocalAdventure()
                    && !hasFullSet(mc)) {
                    reportMissing(mc);
                    return;
                }
                findPosition(mc);
            }
            case BUILDING -> tryPlaceNext(mc, now);
            case DONE -> doDone(mc, now);
        }
    }

    private void resetBuild() {
        buildIndex = 0;
        retries = 0;
        hasPending = false;
        java.util.Arrays.fill(placedAt, 0L);
    }

    private void findPosition(MinecraftWrapper mc) {
        var look = mc.getPlayer().getViewVector(1.0F).normalize();
        var playerPos = mc.getPlayer().blockPosition();
        int ppX = playerPos.getX(), ppY = playerPos.getY(), ppZ = playerPos.getZ();
        for (double d = 3.0; d <= 6.0; d += 0.5) {
            int cx = (int) Math.round(mc.getPlayer().getX() + look.x * d);
            int cz = (int) Math.round(mc.getPlayer().getZ() + look.z * d);
            int groundY = -1;
            for (int y = ppY + 3; y >= ppY - 10; y--) {
                if (y < mc.getLevel().getMinY()) break;
                if (y - 1 < mc.getLevel().getMinY()) break;
                if (BlockUtility.isSolid(mc.getLevel(), cx, y - 1, cz)
                    && isFreeCell(BlockUtility.getState(mc.getLevel(), cx, y, cz))) {
                    groundY = y;
                    break;
                }
            }
            if (groundY == -1) continue;
            if (!validateSpot(mc, cx, groundY, cz)) continue;
            startBuild(cx, groundY, cz);
            return;
        }
        if (mc.getPlayer().onGround()) {
            int fx = ppX + (int) Math.round(look.x);
            int fz = ppZ + (int) Math.round(look.z);
            int groundY = ppY + 1;
            if (groundY - 1 >= mc.getLevel().getMinY() && validateSpot(mc, fx, groundY, fz)) {
                startBuild(fx, groundY, fz);
                return;
            }
        }
        if (!positionNotified) {
            sendMsg(ravex.utility.misc.LanguageUtility.t("NoSuitablePositionFound"));
            positionNotified = true;
        }
        Modules.setEnabled(AutoWither.class, false);
    }

    private void startBuild(int cx, int groundY, int cz) {
        baseX = cx;
        baseY = groundY;
        baseZ = cz;
        hasBase = true;
        resetBuild();
        state = State.BUILDING;
    }

    // full vanilla pattern check, corners included
    private boolean validateSpot(MinecraftWrapper mc, int cx, int cy, int cz) {
        // never build on a finished wither
        for (int[] off : PLACE_OFFSETS) {
            var st = BlockUtility.getState(mc.getLevel(), cx + off[0], cy + off[1], cz + off[2]);
            if (BlockUtility.isBlock(st, "wither_skeleton_skull")
                || BlockUtility.isBlock(st, "wither_skeleton_wall_skull")) return false;
        }
        if (!BlockUtility.isSolid(mc.getLevel(), cx + 1, cy - 1, cz)) return false;
        for (int[] off : CLEARANCE_OFFSETS) {
            if (!isFreeCell(BlockUtility.getState(mc.getLevel(), cx + off[0], cy + off[1], cz + off[2]))) {
                return false;
            }
        }
        return true;
    }

    private void tryPlaceNext(MinecraftWrapper mc, long now) {
        if (!hasBase) {
            state = State.IDLE;
            return;
        }
        if (buildIndex >= PLACE_OFFSETS.length) {
            state = State.DONE;
            return;
        }

        // confirm pending placement before moving on
        if (hasPending) {
            var st = BlockUtility.getState(mc.getLevel(), pendingX, pendingY, pendingZ);
            if (isWitherBlock(st)) {
                markPlaced(buildIndex);
                advance();
                hasPending = false;
                return;
            }
            if (now - pendingSince > PENDING_TIMEOUT_MS) {
                hasPending = false;
                retries++;
                if (retries > MAX_RETRIES) {
                    advance();
                }
            }
            return;
        }

        if (now - lastActionTime < PLACE_INTERVAL_MS) return;

        int[] off = PLACE_OFFSETS[buildIndex];
        int tx = baseX + off[0], ty = baseY + off[1], tz = baseZ + off[2];
        var existing = BlockUtility.getState(mc.getLevel(), tx, ty, tz);
        if (isWitherBlock(existing)) {
            markPlaced(buildIndex);
            advance();
            return;
        }
        if (!isFreeCell(existing)) {
            // world changed, drop this cell
            lastActionTime = now;
            retries++;
            if (retries > MAX_RETRIES) advance();
            return;
        }

        int slot = findItemSlot(mc);
        if (slot == -1) {
            reportMissing(mc);
            return;
        }
        var hit = findSupportHit(mc, BlockUtility.pos(tx, ty, tz));
        if (hit == null) {
            // no neighbour yet, wait for the previous block
            lastActionTime = now;
            retries++;
            if (retries > 20) advance();
            return;
        }
        int prev = InventoryUtility.getSelectedSlot(mc.getPlayer());
        if (!swapTo(mc, slot)) return;
        BlockUtility.useItemOn(mc, hit);
        BlockUtility.swing(mc);
        swapBack(mc, prev);
        lastActionTime = now;
        pendingX = tx;
        pendingY = ty;
        pendingZ = tz;
        pendingSince = now;
        hasPending = true;
    }

    // clicks a neighbour towards the target, no fallback
    // sturdy faces first, then any collision shape
    @Nullable
    private static net.minecraft.world.phys.BlockHitResult findSupportHit(MinecraftWrapper mc, net.minecraft.core.BlockPos target) {
        var level = mc.getLevel();
        var eye = mc.getPlayer().getEyePosition();
        net.minecraft.core.BlockPos best = null;
        net.minecraft.core.Direction bestFace = null;
        double bestDist = Double.MAX_VALUE;
        for (int pass = 0; pass < 2 && best == null; pass++) {
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                var neighbor = target.relative(dir);
                var st = level.getBlockState(neighbor);
                if (st.isAir() || st.liquid()) continue;
                var face = dir.getOpposite();
                boolean usable = pass == 0
                    ? st.isFaceSturdy(level, neighbor, face)
                    : !st.getCollisionShape(level, neighbor).isEmpty();
                if (!usable) continue;
                double d = neighbor.distToCenterSqr(eye);
                if (d < bestDist) {
                    bestDist = d;
                    best = neighbor;
                    bestFace = face;
                }
            }
        }
        if (best == null || bestFace == null) return null;
        var hitVec = net.minecraft.world.phys.Vec3.atCenterOf(best)
            .add(new net.minecraft.world.phys.Vec3(bestFace.getStepX(), bestFace.getStepY(), bestFace.getStepZ()).scale(0.5));
        return new net.minecraft.world.phys.BlockHitResult(hitVec, bestFace, best, false);
    }

    private void markPlaced(int index) {
        if (index >= 0 && index < placedAt.length) {
            placedAt[index] = System.currentTimeMillis();
        }
        lingerUntil = System.currentTimeMillis() + LINGER_MS;
    }

    private void advance() {
        buildIndex++;
        retries = 0;
        hasPending = false;
        lastActionTime = System.currentTimeMillis();
    }

    private void doDone(MinecraftWrapper mc, long now) {
        lingerUntil = Math.max(lingerUntil, now + LINGER_MS);
        if (autoDisable) {
            Modules.setEnabled(AutoWither.class, false);
            return;
        }
        // step five blocks aside and build again
        int nx = baseX + 5;
        if (hasBase && validateSpot(mc, nx, baseY, baseZ)) {
            baseX = nx;
            resetBuild();
            state = State.BUILDING;
            return;
        }
        hasBase = false;
        resetBuild();
        state = State.IDLE;
    }

    private static boolean isWitherBlock(net.minecraft.world.level.block.state.BlockState state) {        return BlockUtility.isBlock(state, "soul_sand")
            || BlockUtility.isBlock(state, "soul_soil")
            || BlockUtility.isBlock(state, "wither_skeleton_skull")
            || BlockUtility.isBlock(state, "wither_skeleton_wall_skull");
    }

    private static boolean isFreeCell(net.minecraft.world.level.block.state.BlockState state) {
        if (state.isAir() || isWitherBlock(state)) return true;
        return !state.liquid() && state.canBeReplaced();
    }

    // hotbar only, no inventory pulls
    private int findItemSlot(MinecraftWrapper mc) {
        boolean needSand = buildIndex < SOUL_SAND_COUNT;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (stack.isEmpty()) continue;
            boolean matches = needSand
                ? (InventoryUtility.isItem(stack, "soul_sand") || InventoryUtility.isItem(stack, "soul_soil"))
                : InventoryUtility.isItem(stack, "wither_skeleton_skull");
            if (matches) return i;
        }
        return -1;
    }

    private int countInHotbar(MinecraftWrapper mc, String... ids) {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (stack.isEmpty()) continue;
            for (String id : ids) {
                if (InventoryUtility.isItem(stack, id)) {
                    count += stack.getCount();
                    break;
                }
            }
        }
        return count;
    }

    private boolean hasFullSet(MinecraftWrapper mc) {
        return countInHotbar(mc, "soul_sand", "soul_soil") >= SOUL_SAND_COUNT
            && countInHotbar(mc, "wither_skeleton_skull") >= (PLACE_OFFSETS.length - SOUL_SAND_COUNT);
    }

    private boolean swapTo(MinecraftWrapper mc, int slot) {
        int cur = InventoryUtility.getSelectedSlot(mc.getPlayer());
        if (cur == slot) return true;
        if ("None".equals(swapMode)) return false;
        InventoryUtility.swapToSlot(mc.getPlayer(), slot, swapMode);
        return true;
    }

    private void swapBack(MinecraftWrapper mc, int prev) {
        if (!"Silent".equals(swapMode)) return;
        InventoryUtility.swapBackSlot(mc.getPlayer(), prev, swapMode);
    }

    private void reportMissing(MinecraftWrapper mc) {
        if (GameModeUtility.isLocalSpectator()) {
            sendMsg(ravex.utility.misc.LanguageUtility.t("CannotPlaceBlocksInSpectator"));
            Modules.setEnabled(AutoWither.class, false);
            return;
        }
        if (GameModeUtility.isLocalCreative() || GameModeUtility.isLocalAdventure()) {
            Modules.setEnabled(AutoWither.class, false);
            return;
        }
        int placedSand = Math.min(buildIndex, SOUL_SAND_COUNT);
        int placedSkulls = Math.max(0, buildIndex - SOUL_SAND_COUNT);
        int needSand = Math.max(0, SOUL_SAND_COUNT - placedSand);
        int needSkulls = Math.max(0, (PLACE_OFFSETS.length - SOUL_SAND_COUNT) - placedSkulls);
        int haveSand = countInHotbar(mc, "soul_sand", "soul_soil");
        int haveSkulls = countInHotbar(mc, "wither_skeleton_skull");
        int missingSand = Math.max(0, needSand - haveSand);
        int missingSkulls = Math.max(0, needSkulls - haveSkulls);
        sendMsg(ravex.utility.misc.LanguageUtility.t("autowither_missing",
            missingSand, missingSkulls));
        Modules.setEnabled(AutoWither.class, false);
    }

    private void sendMsg(String msg) {
        ClientAlertUtility.alert("§8[§5AutoWither§8] §7" + msg);
    }
}
