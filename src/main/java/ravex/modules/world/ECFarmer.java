package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;

import ravex.utility.nativelib.NativeLibraryUtility;

import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.client.ClientAlertUtility;
import org.jetbrains.annotations.Nullable;
@Module(name = "ECFarmer", category = "World")
public class ECFarmer {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.5)
    public double range = 4.5;
    @Parameter(name = "Swap", modes = {"None", "Normal", "Silent"})
    public String swapMode = "Silent";
    @Parameter(name = "AutoTool")
    public boolean autoTool = true;
    @Parameter(name = "AutoDisable")
    public boolean autoDisable = false;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Rotate")
    public boolean rotate = true;
    @Parameter(name = "RotationSpeed", min = 10.0, max = 360.0, step = 5.0, visible = "rotate")
    public double rotationSpeed = 120.0;
    @Parameter(name = "Color", color = true, visible = "render")
    public int color = 0x3F8800FF;
    private enum State { IDLE, FIND_BREAK, BREAKING, FIND_PLACE, PLACING }
    private State state = State.IDLE;
    private int ecX, ecY, ecZ;
    private boolean hasEc;
    private long lastActionTime = 0;
    private long breakStartTime = 0;
    private int prevSlot = -1;
    private String activeSwapMode = "Silent";
    private static int targetX, targetY, targetZ;
    private static boolean hasRenderTarget;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_ecfarmer");
    private final ravex.utility.render.animate.EasingAnimationUtility placeAnim = new ravex.utility.render.animate.EasingAnimationUtility();
    private final ravex.utility.render.animate.EasingAnimationUtility breakAnim = new ravex.utility.render.animate.EasingAnimationUtility();
    private final ravex.utility.render.animate.SlideAnimationUtility slideAnim = new ravex.utility.render.animate.SlideAnimationUtility();
    public static float renderProgress = 0f;
    public static float placePulse = 0f;
    public static int renderState = 0;
    public static long breakAnimStart = 0L;
    public static long placeAnimStart = 0L;
    public static long placeAnimEnd = 0L;
    public static float breakingProgress = 0f;
    public static float viewYaw = 0f;
    public static float viewPitch = 0f;
    public static boolean capturingView = false;
    private int activeToolSlot = -1;
    private float animBreak = 0f;
    private float animPlace = 0f;

    @Nullable
    public static net.minecraft.core.BlockPos getCurrentTarget() {
        if (!hasRenderTarget) return null;
        return BlockUtility.pos(targetX, targetY, targetZ);
    }
    public void onEnable() {
        state = State.IDLE;
        hasEc = false;
        hasRenderTarget = false;
        prevSlot = -1;
        placeAnim.reset();
        breakAnim.reset();
        slideAnim.reset();
        animBreak = 0f;
        animPlace = 0f;
        renderProgress = 0f;
        placePulse = 0f;
        renderState = 0;
        breakAnimStart = 0L;
        placeAnimStart = 0L;
        placeAnimEnd = 0L;
        breakingProgress = 0f;
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
        }
        if (prevSlot != -1) swapBack(MinecraftWrapper.getWrapper(), prevSlot);
        hasEc = false;
        hasRenderTarget = false;
        prevSlot = -1;
        state = State.IDLE;
        animBreak = 0f;
        animPlace = 0f;
        renderProgress = 0f;
        placePulse = 0f;
        renderState = 0;
        breakAnimStart = 0L;
        placeAnimStart = 0L;
        placeAnimEnd = 0L;
        breakingProgress = 0f;
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
        updateAnimations(now);
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
    private void updateAnimations(long now) {
        boolean breaking = state == State.BREAKING && breakStartTime > 0;
        boolean placing = now < placeAnimEnd;
        animBreak = breakAnim.updateFloat(breaking, 0.35f);
        animPlace = placeAnim.updateFloat(placing, 0.45f);
        if (breaking && breakStartTime > 0) {
            renderProgress = breakingProgress;
        } else {
            renderProgress = animBreak;
        }
        if (placing) {
            placePulse = (float) (0.5 + 0.5 * Math.sin((now - placeAnimStart) * 0.02));
        } else {
            placePulse *= 0.85f;
        }
        renderState = breaking ? 1 : (placing ? 2 : 0);
    }
    private void findBreakTarget(MinecraftWrapper mc) {
        int[] found = scanForEC(mc);
        if (found != null) {
            ecX = found[0]; ecY = found[1]; ecZ = found[2];
            hasEc = true;
            targetX = ecX; targetY = ecY; targetZ = ecZ; hasRenderTarget = true;
            state = State.BREAKING;
            breakStartTime = 0;
            breakAnimStart = 0L;
            breakingProgress = 0f;
            activeToolSlot = -1;
            prevSlot = -1;
            return;
        }
        state = State.FIND_PLACE;
    }
    private void findPlaceTarget(MinecraftWrapper mc) {
        int ecSlot = findECSlot(mc);
        if (ecSlot == -1) {
            sendMsg(mc, ravex.utility.misc.LanguageUtility.t("NoEnderChestsLeft"));
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
            PhysicUtility.centerOf(below), net.minecraft.core.Direction.UP, below, false));
        BlockUtility.swing(mc);
        swapBack(mc, original);
        placeAnimStart = now;
        placeAnimEnd = now + 500;
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
            hasEc = false;
            hasRenderTarget = false;
            prevSlot = -1;
            state = State.IDLE;
            return;
        }
        if (breakStartTime == 0) {
            int toolSlot = autoTool ? findBestTool(mc, cur) : findPickaxeSlot(mc);
            if (toolSlot == -1) {
                sendMsg(mc, ravex.utility.misc.LanguageUtility.t("NoPickaxeFound"));
                ravex.modules.Modules.setEnabled(ECFarmer.class, false);
                return;
            }
            prevSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
            activeSwapMode = resolveSwapMode();
            if (!doSwap(mc, toolSlot, activeSwapMode)) {
                prevSlot = -1;
                state = State.IDLE;
                return;
            }
            breakStartTime = now;
            breakAnimStart = now;
            breakingProgress = 0f;
            activeToolSlot = toolSlot;
            var dir = getDirection(mc.getPlayer().getEyePosition(), ecX, ecY, ecZ);
            mc.getGameMode().startDestroyBlock(BlockUtility.pos(ecX, ecY, ecZ), dir);
            BlockUtility.swing(mc);
            return;
        }
        if (autoTool && "Silent".equals(activeSwapMode)) {
            int toolSlot = findBestTool(mc, cur);
            if (toolSlot != -1) {
                InventoryUtility.silentSelectSlot(mc.getPlayer(), toolSlot);
                activeToolSlot = toolSlot;
            }
        }
        breakingProgress = Math.min(1f, breakingProgress + miningRate(mc, cur));
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
            breakAnimStart = 0L;
            breakingProgress = 0f;
            activeToolSlot = -1;
            if (autoDisable) ravex.modules.Modules.setEnabled(ECFarmer.class, false);
        }
    }
    private float miningRate(MinecraftWrapper mc, net.minecraft.world.level.block.state.BlockState state) {
        var player = mc.getPlayer();
        int cur = InventoryUtility.getSelectedSlot(player);
        if (activeToolSlot >= 0 && activeToolSlot != cur) {
            InventoryUtility.selectSlot(player, activeToolSlot);
        }
        float rate = state.getDestroyProgress(player, mc.getLevel(), BlockUtility.pos(ecX, ecY, ecZ));
        if (activeToolSlot >= 0 && activeToolSlot != cur) {
            InventoryUtility.selectSlot(player, cur);
        }
        return Math.max(rate, 0f);
    }
    private String resolveSwapMode() {
        if ("None".equals(swapMode)) return autoTool ? "Silent" : "None";
        return swapMode;
    }
    private boolean doSwap(MinecraftWrapper mc, int targetSlot) {
        return doSwap(mc, targetSlot, resolveSwapMode());
    }
    private boolean doSwap(MinecraftWrapper mc, int targetSlot, String mode) {
        if (mode.equals("None")) {
            return InventoryUtility.getSelectedSlot(mc.getPlayer()) == targetSlot;
        } else if (mode.equals("Normal")) {
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
        String mode = activeSwapMode;
        if (mode.equals("Normal")) {
            InventoryUtility.selectSlot(mc.getPlayer(), originalSlot);
        } else if (mode.equals("Silent")) {
            InventoryUtility.silentSelectSlot(mc.getPlayer(), originalSlot);
        }
    }
    private void sendMsg(MinecraftWrapper mc, String msg) {
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
    private int findECSlot(MinecraftWrapper mc) {
        int slot = InventoryUtility.findHotbarSlot(mc.getPlayer(), "ender_chest");
        if (slot != -1) return slot;
        slot = InventoryUtility.findSlot(mc.getPlayer(), "ender_chest", 9, 36);
        if (slot != -1) {
            int free = InventoryUtility.findEmptyHotbarSlot(mc.getPlayer());
            if (free != -1) {
                InventoryUtility.selectSlot(mc.getPlayer(), free);
                InventoryUtility.handleInventoryClick(mc, mc.getPlayer(), slot, free, InventoryUtility.SWAP);
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
    private int findBestTool(MinecraftWrapper mc, net.minecraft.world.level.block.state.BlockState state) {
        var player = mc.getPlayer();
        int best = ravex.utility.player.ToolUtility.findBestToolSlot(player, state);
        if (best >= 0) return best;
        int selected = InventoryUtility.getSelectedSlot(player);
        float invSpeed = 1.0f;
        int bestInv = -1;
        for (int i = 9; i < 36; i++) {
            var stack = InventoryUtility.getItem(player, i);
            if (stack.isEmpty()) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > invSpeed) {
                invSpeed = speed;
                bestInv = i;
            }
        }
        if (bestInv != -1) {
            float selSpeed = InventoryUtility.getItem(player, selected).getDestroySpeed(state);
            if (invSpeed > selSpeed) {
                int free = InventoryUtility.findEmptyHotbarSlot(player);
                if (free != -1) {
                    InventoryUtility.handleInventoryClick(mc, player, bestInv, free, InventoryUtility.SWAP);
                    return free;
                }
                InventoryUtility.handleInventoryClick(mc, player, bestInv, selected, InventoryUtility.SWAP);
                return selected;
            }
        }
        if (InventoryUtility.getItem(player, selected).getDestroySpeed(state) > 1.0f) return selected;
        return -1;
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
            if (absY >= absZ) return dy > 0 ? net.minecraft.core.Direction.DOWN : net.minecraft.core.Direction.UP;
            else return dz > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
        } else {
            if (absY >= absX) return dy > 0 ? net.minecraft.core.Direction.DOWN : net.minecraft.core.Direction.UP;
            else return dx > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
        }
    }
    private static native double nativeCalcBreakTime(String toolId, int efficiency, int haste, int durability, int maxDura);
    private static native int nativeCalcDurabilityLoss(String toolId, int efficiency);





}