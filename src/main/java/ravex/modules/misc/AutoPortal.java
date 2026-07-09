package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.utility.player.InventoryUtility;
import ravex.utility.misc.block.BlockUtility;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
public class AutoPortal extends Module {
    public final NumberParameter range = new NumberParameter("Range", 6.0, 2.0, 12.0, 0.5);
    public final NumberParameter minRange = new NumberParameter("MinRange", 2.0, 1.0, 4.0, 0.5);
    public final NumberParameter avoidRange = new NumberParameter("Avoid", 8.0, 1.0, 24.0, 1.0);
    public final BooleanParameter build = new BooleanParameter("Build", true);
    public final BooleanParameter light = new BooleanParameter("Light", true);
    public final NumberParameter portalsToBuild = new NumberParameter("Portals", 2.0, 1.0, 6.0, 1.0);
    public final BooleanParameter autoDisable = new BooleanParameter("AutoDisable", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3FAA00FF);
    private enum State { IDLE, FIND, BUILDING, RETRY, VERIFY, LIGHTING, DONE }
    private State state = State.IDLE;
    private int baseX, baseY, baseZ;
    private boolean hasBase;
    private int frameIndex = 0;
    private int retries = 0;
    private int failX, failY, failZ;
    private boolean hasFailed;
    private long lastActionTime = 0;
    private int portalBuildCount = 0;
    private int firstBaseX, firstBaseY, firstBaseZ;
    private boolean hasFirstBase;
    private int portalSpacing = 5;
    private static int targetX, targetY, targetZ;
    private static boolean hasRenderTarget;
    private static final int[][] FRAME_OFFSETS = {
        {0, 0}, {1, 0}, {2, 0}, {3, 0},
        {0, 1}, {0, 2}, {0, 3},
        {3, 1}, {3, 2}, {3, 3},
        {0, 4}, {1, 4}, {2, 4}, {3, 4},
    };

    public static net.minecraft.core.BlockPos getCurrentTarget() {
        if (!hasRenderTarget) return null;
        return BlockUtility.pos(targetX, targetY, targetZ);
    }

    @Override
    protected void onEnable() {
        state = State.IDLE;
        hasBase = false;
        frameIndex = 0;
        retries = 0;
        hasFailed = false;
        hasRenderTarget = false;
        portalBuildCount = 0;
        hasFirstBase = false;
    }
    @Override
    protected void onDisable() {
        state = State.IDLE;
        hasBase = false;
        frameIndex = 0;
        hasRenderTarget = false;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        long now = System.currentTimeMillis();
        switch (state) {
            case IDLE -> state = State.FIND;
            case FIND -> doFind(mc);
            case BUILDING -> tryPlaceNext(mc, now);
            case RETRY -> retryPlace(mc, now);
            case VERIFY -> doVerify(mc, now);
            case LIGHTING -> doLight(mc, now);
            case DONE -> doDone(mc);
        }
    }
    private void doFind(Minecraft mc) {
        double px, pz;
        double py = mc.player.getY();
        if (hasFirstBase) {
            px = firstBaseX + portalBuildCount * portalSpacing;
            pz = firstBaseZ;
        } else {
            px = mc.player.getX();
            pz = mc.player.getZ();
            float yaw = mc.player.getYRot();
            double[] result = findBestPortalPos(
                px, py, pz, yaw,
                minRange.getValue(), range.getValue(),
                avoidRange.getValue(), findExistingPortals(mc));
            if (result[3] < 0) return;
            px = result[0];
            py = result[1];
            pz = result[2];
        }
        int startY = (int) Math.round(py) + 3;
        int groundY = -1;
        for (int y = startY; y >= startY - 16; y--) {
            if (y < mc.level.getMinY()) break;
            if (y - 1 < mc.level.getMinY()) break;
            int bx = (int) Math.round(px), bz = (int) Math.round(pz);
            if (BlockUtility.isSolid(mc.level, bx, y - 1, bz)
                && BlockUtility.isAir(mc.level, bx, y, bz)) {
                groundY = y;
                break;
            }
        }
        if (groundY == -1) return;
        int gx = (int) Math.round(px), gz = (int) Math.round(pz);
        for (int[] off : FRAME_OFFSETS) {
            int ox = gx + off[0], oz = gz + off[1];
            if (!BlockUtility.isAir(mc.level, ox, groundY + off[1], oz)
                && !BlockUtility.isBlock(mc.level, BlockUtility.pos(ox, groundY + off[1], oz), "obsidian")) return;
        }
        baseX = gx; baseY = groundY; baseZ = gz;
        hasBase = true;
        if (!hasFirstBase) { firstBaseX = gx; firstBaseY = groundY; firstBaseZ = gz; hasFirstBase = true; }
        frameIndex = 0;
        retries = 0;
        state = build.getValue() ? State.BUILDING : State.VERIFY;
    }
    private void tryPlaceNext(Minecraft mc, long now) {
        if (now - lastActionTime < 100) return;
        lastActionTime = now;
        if (!hasBase) { state = State.IDLE; return; }
        if (frameIndex >= FRAME_OFFSETS.length) {
            state = State.VERIFY;
            return;
        }
        int[] off = FRAME_OFFSETS[frameIndex];
        int tx = baseX + off[0], ty = baseY + off[1], tz = baseZ + off[2];
        targetX = tx; targetY = ty; targetZ = tz; hasRenderTarget = true;
        var existing = BlockUtility.getState(mc.level, tx, ty, tz);
        if (BlockUtility.isBlock(existing, "obsidian")
            || BlockUtility.isBlock(existing, "nether_portal")) {
            frameIndex++;
            retries = 0;
            return;
        }
        int slot = findObsidianSlot(mc);
        if (slot == -1) {
            sendMsg(mc, "Not enough obsidian, disabling");
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
        InventoryUtility.selectSlot(mc.player, prev);
        lastActionTime = now;
        retries = 0;
        frameIndex++;
    }
    private void retryPlace(Minecraft mc, long now) {
        if (now - lastActionTime < 200) return;
        lastActionTime = now;
        if (!hasFailed) { state = State.BUILDING; return; }
        retries++;
        if (retries > 5) {
            frameIndex++;
            retries = 0;
            hasFailed = false;
            state = State.BUILDING;
            return;
        }
        var st = BlockUtility.getState(mc.level, failX, failY, failZ);
        if (BlockUtility.isBlock(st, "obsidian")) {
            frameIndex++;
            retries = 0;
            hasFailed = false;
            state = State.BUILDING;
            return;
        }
        int slot = findObsidianSlot(mc);
        if (slot == -1) {
            sendMsg(mc, "Not enough obsidian, disabling");
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
        InventoryUtility.selectSlot(mc.player, prev);
    }
    private void doVerify(Minecraft mc, long now) {
        if (!hasBase) { state = State.IDLE; return; }
        boolean allPlaced = true;
        for (int[] off : FRAME_OFFSETS) {
            int ox = baseX + off[0], oy = baseY + off[1], oz = baseZ + off[2];
            if (!BlockUtility.isBlock(mc.level, BlockUtility.pos(ox, oy, oz), "obsidian")) {
                allPlaced = false;
                break;
            }
        }
        if (!allPlaced) {
            sendMsg(mc, "Some frame blocks missing, skipping lighting");
            state = State.DONE;
            return;
        }
        state = light.getValue() ? State.LIGHTING : State.DONE;
    }
    private void doLight(Minecraft mc, long now) {
        if (now - lastActionTime < 100) return;
        lastActionTime = now;
        if (!hasBase) { state = State.IDLE; return; }
        int ix = baseX + 1, iy = baseY + 1, iz = baseZ + 0;
        targetX = ix; targetY = iy; targetZ = iz; hasRenderTarget = true;
        int slot = findFlintOrFireChargeSlot(mc);
        if (slot == -1) {
            state = State.DONE;
            return;
        }
        int prev = InventoryUtility.getSelectedSlot(mc.player);
        InventoryUtility.selectSlot(mc.player, slot);
        var interiorPos = BlockUtility.pos(ix, iy, iz);
        BlockUtility.useItemOn(mc, new net.minecraft.world.phys.BlockHitResult(
            net.minecraft.world.phys.Vec3.atCenterOf(interiorPos), net.minecraft.core.Direction.UP, interiorPos, false));
        BlockUtility.swing(mc);
        InventoryUtility.selectSlot(mc.player, prev);
        state = State.DONE;
    }
    private void doDone(Minecraft mc) {
        hasRenderTarget = false;
        portalBuildCount++;
        int target = portalsToBuild.getValue().intValue();
        if (portalBuildCount < target) {
            state = State.FIND;
        } else if (autoDisable.getValue()) {
            setEnabled(false);
        } else {
            state = State.IDLE;
        }
    }
    private void sendMsg(Minecraft mc, String msg) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§8[§5AutoPortal§8] §7" + msg), false);
        }
    }
    private static double[] findBestPortalPos(
        double playerX, double playerY, double playerZ,
        double playerYaw,
        double minDist, double maxDist,
        double avoidRange,
        double[] existingPortals
    ) {
        double yawRad = playerYaw * Math.PI / 180.0;
        double dirX = -Math.sin(yawRad);
        double dirZ = Math.cos(yawRad);
        double searchRadius = maxDist;
        int steps = (int)(searchRadius * 2);
        double bestX = 0, bestY = 0, bestZ = 0;
        double bestScore = -1.0;
        int portalCount = existingPortals != null ? existingPortals.length / 3 : 0;
        for (int i = 0; i < steps; i++) {
            for (int j = 0; j < steps; j++) {
                double wx = playerX - searchRadius + (2.0 * searchRadius * i / steps);
                double wz = playerZ - searchRadius + (2.0 * searchRadius * j / steps);
                int bx = (int)Math.round(wx);
                int bz = (int)Math.round(wz);
                double cx = bx + 0.5, cy = playerY, cz = bz + 0.5;
                double dx = cx - playerX, dy = cy - playerY, dz = cz - playerZ;
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                if (dist < minDist || dist > maxDist) continue;
                boolean tooClose = false;
                for (int p = 0; p < portalCount; p++) {
                    double px2 = existingPortals[p * 3];
                    double py2 = existingPortals[p * 3 + 1];
                    double pz2 = existingPortals[p * 3 + 2];
                    double ex = cx - px2, ey = cy - py2, ez = cz - pz2;
                    double pd = Math.sqrt(ex*ex + ey*ey + ez*ez);
                    if (pd < avoidRange) { tooClose = true; break; }
                }
                if (tooClose) continue;
                double tdx = cx - playerX, tdz = cz - playerZ;
                double tLen = Math.sqrt(tdx*tdx + tdz*tdz);
                if (tLen < 0.01) continue;
                tdx /= tLen; tdz /= tLen;
                double dot = dirX * tdx + dirZ * tdz;
                double score = dot * (1.0 - dist / maxDist);
                if (score > bestScore) {
                    bestScore = score;
                    bestX = bx;
                    bestY = playerY;
                    bestZ = bz;
                }
            }
        }
        return new double[]{bestX, bestY, bestZ, bestScore};
    }
    private double[] findExistingPortals(Minecraft mc) {
        double r = avoidRange.getValue();
        var eye = mc.player.getEyePosition();
        java.util.ArrayList<Double> list = new java.util.ArrayList<>();
        int minX = (int)(eye.x - r), minY = (int)(eye.y - 5), minZ = (int)(eye.z - r);
        int maxX = (int)(eye.x + r), maxY = (int)(eye.y + 5), maxZ = (int)(eye.z + r);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (BlockUtility.isBlock(mc.level, BlockUtility.pos(x, y, z), "nether_portal")) {
                        list.add((double) x);
                        list.add((double) y);
                        list.add((double) z);
                    }
                }
            }
        }
        if (list.isEmpty()) return null;
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
    private int findObsidianSlot(Minecraft mc) {
        int slot = InventoryUtility.findHotbarSlot(mc.player, "obsidian");
        if (slot != -1) return slot;
        slot = InventoryUtility.findSlot(mc.player, "obsidian", 9, 36);
        if (slot != -1) {
            int free = InventoryUtility.findEmptyHotbarSlot(mc.player);
            if (free != -1) {
                InventoryUtility.selectSlot(mc.player, free);
                InventoryUtility.handleInventoryClick(mc, mc.player, slot, free, net.minecraft.world.inventory.ClickType.SWAP);
                return free;
            }
        }
        return -1;
    }
    private int findFlintOrFireChargeSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.isItemInSlot(mc.player, i, "flint_and_steel")
                || InventoryUtility.isItemInSlot(mc.player, i, "fire_charge")) return i;
        }
        return -1;
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(AutoPortal.class);
    }

    public static AutoPortal itz() {
        return ModuleManager.get(AutoPortal.class);
    }
}
