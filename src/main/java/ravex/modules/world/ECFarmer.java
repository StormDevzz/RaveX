package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;


import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.network.NetworkUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.client.ClientAlertUtility;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
@Module(name = "ECFarmer", category = "World")
public class ECFarmer {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.5)
    public double range = 4.5;
    @Parameter(name = "RestoreSlot")
    public boolean restoreSlot = true;
    @Parameter(name = "AutoDisable")
    public boolean autoDisable = false;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Rotate")
    public boolean rotate = true;
    @Parameter(name = "RotationSpeed", min = 10.0, max = 360.0, step = 5.0, visible = "rotate")
    public double rotationSpeed = 120.0;
    @Parameter(name = "Color", color = true, visible = "render")
    public int color = 0x3F40A9F8;
    private enum State { IDLE, FIND_BREAK, BREAKING, FIND_PLACE, PLACING }
    private State state = State.IDLE;
    private int ecX, ecY, ecZ;
    private boolean hasEc;
    private long lastActionTime = 0;
    private long breakStartTime = 0;
    private int prevSlot = -1;
    private static int targetX, targetY, targetZ;
    private static boolean hasRenderTarget;

    // last finished block, fades out as a highlight
    public static final long HIGHLIGHT_FADE_MS = 900L;
    private static int lastX, lastY, lastZ;
    private static long lastAt = 0L;
    private static boolean hasLast;

    public static float viewYaw = 0f;
    public static float viewPitch = 0f;
    public static boolean capturingView = false;
    private int activeToolSlot = -1;

    @Nullable
    public static net.minecraft.core.BlockPos getCurrentTarget() {
        if (!hasRenderTarget) return null;
        return BlockUtility.pos(targetX, targetY, targetZ);
    }

    @Nullable
    public static net.minecraft.core.BlockPos getLastTarget() {
        if (!hasLast || System.currentTimeMillis() - lastAt > HIGHLIGHT_FADE_MS) return null;
        return BlockUtility.pos(lastX, lastY, lastZ);
    }

    public static float getLastFade() {
        if (!hasLast) return 0f;
        float t = Math.min(1f, (System.currentTimeMillis() - lastAt) / (float) HIGHLIGHT_FADE_MS);
        return 1f - t;
    }

    private static void markLast(int x, int y, int z) {
        lastX = x; lastY = y; lastZ = z;
        lastAt = System.currentTimeMillis();
        hasLast = true;
    }

    public void onEnable() {
        state = State.IDLE;
        hasEc = false;
        hasRenderTarget = false;
        hasLast = false;
        prevSlot = -1;
        activeToolSlot = -1;
        capturingView = false;
    }

    public void onDisable() {
        releaseView();
        if (hasEc) {
            var st = BlockUtility.getState(MinecraftWrapper.getWrapper().getLevel(), ecX, ecY, ecZ);
            if (BlockUtility.isBlock(st, "ender_chest")) {
                MinecraftWrapper.getWrapper().getGameMode().stopDestroyBlock();
            }
            if (prevSlot != -1) swapBack(MinecraftWrapper.getWrapper(), prevSlot);
        }
        hasEc = false;
        hasRenderTarget = false;
        prevSlot = -1;
        state = State.IDLE;
        activeToolSlot = -1;
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        long now = System.currentTimeMillis();
        if (rotate && state == State.BREAKING && hasEc) {
            updateRotation(mc);
        } else {
            releaseView();
        }
        switch (state) {
            case IDLE -> state = State.FIND_BREAK;
            case FIND_BREAK -> findBreakTarget(mc);
            case BREAKING -> doBreak(mc, now);
            case FIND_PLACE -> findPlaceTarget(mc);
            case PLACING -> doPlace(mc, now);
        }
    }

    private void updateRotation(MinecraftWrapper mc) {
        var p = mc.getPlayer();
        var center = PhysicUtility.centerOf(BlockUtility.pos(ecX, ecY, ecZ));
        if (!capturingView) {
            viewYaw = p.getYRot();
            viewPitch = p.getXRot();
            capturingView = true;
        }
        float[] target = RotationUtility.anglesTo(p.getEyePosition(), center);
        float[] limited = AimUtility.limitAngles(
            p.getYRot(), target[0],
            p.getXRot(), target[1],
            (float) (rotationSpeed / 20f)
        );
        p.setYRot(limited[0]);
        p.setXRot(limited[1]);
    }

    private void releaseView() {
        if (!capturingView) return;
        capturingView = false;
        var p = MinecraftWrapper.getWrapper().getPlayer();
        if (p != null) {
            p.setYRot(viewYaw);
            p.setXRot(viewPitch);
        }
    }

    private void findBreakTarget(MinecraftWrapper mc) {
        int[] found = scanForEC(mc);
        if (found != null) {
            ecX = found[0]; ecY = found[1]; ecZ = found[2];
            hasEc = true;
            targetX = ecX; targetY = ecY; targetZ = ecZ; hasRenderTarget = true;
            state = State.BREAKING;
            breakStartTime = 0;
            activeToolSlot = -1;
            prevSlot = -1;
            return;
        }
        state = State.FIND_PLACE;
    }

    private void findPlaceTarget(MinecraftWrapper mc) {
        int ecSlot = findECSlot(mc);
        if (ecSlot == -1) {
            sendMsg(ravex.utility.misc.LanguageUtility.t("NoEnderChestsLeft"));
            ravex.modules.Modules.setEnabled(ECFarmer.class, false);
            return;
        }
        int[] placeOn = findPlacePos(mc);
        if (placeOn == null) return;
        ecX = placeOn[0]; ecY = placeOn[1] + 1; ecZ = placeOn[2];
        hasEc = true;
        targetX = ecX; targetY = ecY; targetZ = ecZ; hasRenderTarget = true;
        state = State.PLACING;
    }

    private void doPlace(MinecraftWrapper mc, long now) {
        if (now - lastActionTime < 100) return;
        lastActionTime = now;
        if (!hasEc || !BlockUtility.isAir(mc.getLevel(), ecX, ecY, ecZ)) {
            state = State.IDLE;
            return;
        }
        // grab the slot before any shuffling
        int original = InventoryUtility.getSelectedSlot(mc.getPlayer());
        int ecSlot = findECSlot(mc);
        if (ecSlot == -1) {
            state = State.IDLE;
            return;
        }
        if (original != ecSlot) selectSlot(mc, ecSlot);
        var below = BlockUtility.pos(ecX, ecY - 1, ecZ);
        BlockUtility.useItemOn(mc, new net.minecraft.world.phys.BlockHitResult(
            PhysicUtility.centerOf(below), net.minecraft.core.Direction.UP, below, false));
        BlockUtility.swing(mc);
        swapBack(mc, original);
        markLast(ecX, ecY, ecZ);
        hasRenderTarget = false;
        hasEc = false;
        state = State.IDLE;
        if (autoDisable) ravex.modules.Modules.setEnabled(ECFarmer.class, false);
    }

    private void doBreak(MinecraftWrapper mc, long now) {
        if (!hasEc) {
            state = State.IDLE;
            return;
        }
        var cur = BlockUtility.getState(mc.getLevel(), ecX, ecY, ecZ);
        if (!BlockUtility.isBlock(cur, "ender_chest")) {
            if (prevSlot != -1) swapBack(mc, prevSlot);
            markLast(ecX, ecY, ecZ);
            hasEc = false;
            hasRenderTarget = false;
            prevSlot = -1;
            activeToolSlot = -1;
            state = State.IDLE;
            return;
        }
        if (breakStartTime == 0) {
            // best tool is always selected, hotbar or inventory
            int toolSlot = findToolSlot(mc, cur);
            if (toolSlot == -1) {
                sendMsg(ravex.utility.misc.LanguageUtility.t("NoPickaxeFound"));
                ravex.modules.Modules.setEnabled(ECFarmer.class, false);
                return;
            }
            prevSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
            selectSlot(mc, toolSlot);
            activeToolSlot = toolSlot;
            breakStartTime = now;
            var dir = getDirection(mc.getPlayer().getEyePosition(), ecX, ecY, ecZ);
            mc.getGameMode().startDestroyBlock(BlockUtility.pos(ecX, ecY, ecZ), dir);
            BlockUtility.swing(mc);
            return;
        }
        var dir2 = getDirection(mc.getPlayer().getEyePosition(), ecX, ecY, ecZ);
        mc.getGameMode().continueDestroyBlock(BlockUtility.pos(ecX, ecY, ecZ), dir2);
        BlockUtility.swing(mc);
        var st = BlockUtility.getState(mc.getLevel(), ecX, ecY, ecZ);
        if (st.isAir() || !BlockUtility.isBlock(st, "ender_chest")) {
            if (prevSlot != -1) swapBack(mc, prevSlot);
            markLast(ecX, ecY, ecZ);
            hasEc = false;
            hasRenderTarget = false;
            prevSlot = -1;
            activeToolSlot = -1;
            breakStartTime = 0;
            state = State.IDLE;
            if (autoDisable) ravex.modules.Modules.setEnabled(ECFarmer.class, false);
        }
    }

    // vanilla keybind handler misses programmatic slot changes
    private void selectSlot(MinecraftWrapper mc, int slot) {
        InventoryUtility.selectSlot(mc.getPlayer(), slot);
        NetworkUtility.sendSetCarriedItem(slot);
    }

    private void swapBack(MinecraftWrapper mc, int originalSlot) {
        if (originalSlot == -1 || !restoreSlot) return;
        selectSlot(mc, originalSlot);
    }

    private void sendMsg(String msg) {
        ClientAlertUtility.alert("§8[§5ECFarmer§8] §7" + msg);
    }

    private int[] scanForEC(MinecraftWrapper mc) {
        double r = range;
        var eye = mc.getPlayer().getEyePosition();
        var pPos = mc.getPlayer().blockPosition();
        int minX = (int) Math.floor(pPos.getX() - r);
        int maxX = (int) Math.ceil(pPos.getX() + r);
        int minY = (int) Math.max(mc.getLevel().getMinY(), Math.floor(pPos.getY() - r));
        int maxY = (int) Math.min(mc.getLevel().getMaxY(), Math.ceil(pPos.getY() + r));
        int minZ = (int) Math.floor(pPos.getZ() - r);
        int maxZ = (int) Math.ceil(pPos.getZ() + r);
        int[] closest = null;
        double closestDist = Double.MAX_VALUE;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    var st = BlockUtility.getState(mc.getLevel(), x, y, z);
                    if (BlockUtility.isBlock(st, "ender_chest")) {
                        double dist = BlockUtility.distToSqr(mc.getLevel(), x, y, z, eye.x, eye.y, eye.z);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = new int[]{x, y, z};
                        }
                    }
                }
            }
        }
        return closest;
    }

    // no side effects, doPlace captures the slot first
    private int findECSlot(MinecraftWrapper mc) {
        int slot = InventoryUtility.findHotbarSlot(mc.getPlayer(), "ender_chest");
        if (slot != -1) return slot;
        slot = InventoryUtility.findSlot(mc.getPlayer(), "ender_chest", 9, 36);
        if (slot != -1) {
            int free = InventoryUtility.findEmptyHotbarSlot(mc.getPlayer());
            if (free != -1) {
                InventoryUtility.handleInventoryClick(mc, mc.getPlayer(), slot, free, InventoryUtility.SWAP);
                return free;
            }
        }
        return -1;
    }

    // pickaxe always wins, else the fastest tool
    private int findToolSlot(MinecraftWrapper mc, net.minecraft.world.level.block.state.BlockState state) {
        var player = mc.getPlayer();
        int selected = InventoryUtility.getSelectedSlot(player);
        int best = -1;
        float bestScore = 1.0f; // must beat the bare hand
        for (int i = 0; i < 36; i++) {
            ItemStack stack = InventoryUtility.getItem(player, i);
            if (stack.isEmpty()) continue;
            boolean pickaxe = InventoryUtility.isPickaxeItem(stack);
            if (!pickaxe && state.requiresCorrectToolForDrops() && !stack.isCorrectToolForDrops(state)) continue;
            float score = stack.getDestroySpeed(state) + (pickaxe ? 1000f : 0f);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        if (best == -1) return -1;
        if (best < 9) return best;
        int free = InventoryUtility.findEmptyHotbarSlot(player);
        int dest = free != -1 ? free : selected;
        InventoryUtility.handleInventoryClick(mc, player, best, dest, InventoryUtility.SWAP);
        return dest;
    }

    @Nullable
    private int[] findPlacePos(MinecraftWrapper mc) {
        var eye = mc.getPlayer().getEyePosition();
        var facing = mc.getPlayer().getDirection();
        double r = range;
        var start = mc.getPlayer().blockPosition();
        int sx = start.getX(), sy = start.getY(), sz = start.getZ();
        for (int f = 1; f <= 3; f++) {
            for (int dy = -1; dy <= 1; dy++) {
                int px = sx + facing.getStepX() * f;
                int py = sy + dy;
                int pz = sz + facing.getStepZ() * f;
                if (BlockUtility.distToSqr(mc.getLevel(), px, py, pz, eye.x, eye.y, eye.z) > r * r) continue;
                if (BlockUtility.isSolid(mc.getLevel(), px, py - 1, pz) && BlockUtility.isAir(mc.getLevel(), px, py, pz)) {
                    return new int[]{px, py - 1, pz};
                }
            }
        }
        return null;
    }

    private static net.minecraft.core.Direction getDirection(net.minecraft.world.phys.Vec3 eye, int x, int y, int z) {
        var center = PhysicUtility.centerOf(BlockUtility.pos(x, y, z));
        double dx = eye.x - center.x;
        double dy = eye.y - y - 0.5;
        double dz = eye.z - center.z;
        double absX = Math.abs(dx);
        double absY = Math.abs(dy);
        double absZ = Math.abs(dz);
        if (absY <= absX && absY <= absZ) {
            if (absX >= absZ) return dx > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
            else return dz > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
        } else if (absX <= absY && absX <= absZ) {
            if (absY >= absZ) return dy > 0 ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.DOWN;
            else return dz > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
        } else {
            if (absY >= absX) return dy > 0 ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.DOWN;
            else return dx > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
        }
    }
}
