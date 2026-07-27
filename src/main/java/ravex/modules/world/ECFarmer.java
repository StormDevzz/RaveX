package ravex.modules.world;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;

import ravex.utility.nativelib.NativeLibraryUtility;

import ravex.utility.player.InventoryUtility;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "ECFarmer", category = "World")
public class ECFarmer implements ModuleAccess {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.5)
    public double range = 4.5;
    @Parameter(name = "Swap", modes = {"Silent", "Normal"})
    public String swapMode = "Silent";
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true)
    public int color = 0x3F8800FF;
    private enum State { IDLE, FIND_BREAK, BREAKING, FIND_PLACE, PLACING }
    private State state = State.IDLE;
    private int ecX, ecY, ecZ;
    private boolean hasEc;
    private long lastActionTime = 0;
    private long breakStartTime = 0;
    private int prevSlot = -1;
    private static int targetX, targetY, targetZ;
    private static boolean hasRenderTarget;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_ecfarmer");
    static {
        NATIVE.load();
    }

    public static net.minecraft.core.BlockPos getCurrentTarget() {
        if (!hasRenderTarget) return null;
        return BlockUtility.pos(targetX, targetY, targetZ);
    }
    public void onEnable() {
        state = State.IDLE;
        hasEc = false;
        hasRenderTarget = false;
        prevSlot = -1;
    }
    public void onDisable() {
        if (hasEc) {
            var st = BlockUtility.getState(MinecraftWrapper.getWrapper().getLevel(), ecX, ecY, ecZ);
            if (BlockUtility.isBlock(st, "ender_chest")) {
                MinecraftWrapper.getWrapper().getGameMode().stopDestroyBlock();
            }
        }
        if (prevSlot != -1) swapBack(MinecraftWrapper.getWrapper(), prevSlot);
        hasEc = false;
        hasRenderTarget = false;
        prevSlot = -1;
        state = State.IDLE;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        long now = System.currentTimeMillis();
        switch (state) {
            case IDLE -> state = State.FIND_BREAK;
            case FIND_BREAK -> findBreakTarget(mc);
            case BREAKING -> doBreak(mc, now);
            case FIND_PLACE -> findPlaceTarget(mc);
            case PLACING -> doPlace(mc, now);
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
            prevSlot = -1;
            return;
        }
        state = State.FIND_PLACE;
    }
    private void findPlaceTarget(MinecraftWrapper mc) {
        int ecSlot = findECSlot(mc);
        if (ecSlot == -1) return;
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
        int ecSlot = findECSlot(mc);
        if (ecSlot == -1) {
            state = State.IDLE;
            return;
        }
        int original = InventoryUtility.getSelectedSlot(mc.getPlayer());
        if (!doSwap(mc, ecSlot)) {
            state = State.IDLE;
            return;
        }
        var below = BlockUtility.pos(ecX, ecY - 1, ecZ);
        BlockUtility.useItemOn(mc, new net.minecraft.world.phys.BlockHitResult(
            net.minecraft.world.phys.Vec3.atCenterOf(below), net.minecraft.core.Direction.UP, below, false));
        BlockUtility.swing(mc);
        swapBack(mc, original);
        state = State.IDLE;
    }
    private void doBreak(MinecraftWrapper mc, long now) {
        if (!hasEc) {
            state = State.IDLE;
            return;
        }
        var cur = BlockUtility.getState(mc.getLevel(), ecX, ecY, ecZ);
        if (!BlockUtility.isBlock(cur, "ender_chest")) {
            if (prevSlot != -1) swapBack(mc, prevSlot);
            hasEc = false;
            hasRenderTarget = false;
            prevSlot = -1;
            state = State.IDLE;
            return;
        }
        if (breakStartTime == 0) {
            int pickSlot = findPickaxeSlot(mc);
            if (pickSlot == -1) {
                state = State.IDLE;
                return;
            }
            prevSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
            if (!doSwap(mc, pickSlot)) {
                prevSlot = -1;
                state = State.IDLE;
                return;
            }
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
            hasEc = false;
            hasRenderTarget = false;
            prevSlot = -1;
            state = State.IDLE;
        }
    }
    private boolean doSwap(MinecraftWrapper mc, int targetSlot) {
        String mode = swapMode;
        if (mode.equals("Normal")) {
            InventoryUtility.selectSlot(mc.getPlayer(), targetSlot);
            return true;
        } else if (mode.equals("Silent")) {
            InventoryUtility.silentSelectSlot(mc.getPlayer(), targetSlot);
            return true;
        }
        return false;
    }
    private void swapBack(MinecraftWrapper mc, int originalSlot) {
        if (originalSlot == -1) return;
        String mode = swapMode;
        if (mode.equals("Normal")) {
            InventoryUtility.selectSlot(mc.getPlayer(), originalSlot);
        } else if (mode.equals("Silent")) {
            InventoryUtility.silentSelectSlot(mc.getPlayer(), originalSlot);
        }
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
    private int findECSlot(MinecraftWrapper mc) {
        int slot = InventoryUtility.findHotbarSlot(mc.getPlayer(), "ender_chest");
        if (slot != -1) return slot;
        slot = InventoryUtility.findSlot(mc.getPlayer(), "ender_chest", 9, 36);
        if (slot != -1) {
            int free = InventoryUtility.findEmptyHotbarSlot(mc.getPlayer());
            if (free != -1) {
                InventoryUtility.selectSlot(mc.getPlayer(), free);
                InventoryUtility.handleInventoryClick(mc, mc.getPlayer(), slot, free, net.minecraft.world.inventory.ClickType.SWAP);
                return free;
            }
        }
        return -1;
    }
    private int findPickaxeSlot(MinecraftWrapper mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (InventoryUtility.isItem(stack, "netherite_pickaxe") || InventoryUtility.isItem(stack, "diamond_pickaxe")
                || InventoryUtility.isItem(stack, "iron_pickaxe") || InventoryUtility.isItem(stack, "stone_pickaxe")
                || InventoryUtility.isItem(stack, "wooden_pickaxe")) return i;
        }
        return -1;
    }
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
        var center = net.minecraft.world.phys.Vec3.atCenterOf(BlockUtility.pos(x, y, z));
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
            if (absY >= absZ) return dy > 0 ? net.minecraft.core.Direction.DOWN : net.minecraft.core.Direction.UP;
            else return dz > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
        } else {
            if (absY >= absX) return dy > 0 ? net.minecraft.core.Direction.DOWN : net.minecraft.core.Direction.UP;
            else return dx > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
        }
    }
    private static native double nativeCalcBreakTime(String toolId, int efficiency, int haste, int durability, int maxDura);
    private static native int nativeCalcDurabilityLoss(String toolId, int efficiency);

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ECFarmer").getEnabled();
    }
    public static ECFarmer itz() {
        return ravex.manager.ModuleManager.delegate(ECFarmer.class);
    }


}