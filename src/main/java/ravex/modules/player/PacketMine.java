package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;

import ravex.utility.network.NetworkUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "PacketMine", category = "Player")
public class PacketMine {
    @Parameter(name = "Mode", modes = {"Normal", "Grim", "NCP"})
    public String mode = "Normal";
    @Parameter(name = "Range", min = 2.0, max = 10.0, step = 0.5)
    public double range = 6.0;
    @Parameter(name = "Rotate", modes = {"Silent", "Normal", "None"})
    public String rotate = "Silent";
    @Parameter(name = "SwapMode", modes = {"Silent", "Normal", "None"})
    public String swapMode = "Silent";
    @Parameter(name = "AutoTool")
    public boolean autoTool = true;
    @Parameter(name = "SwitchBack")
    public boolean switchBack = true;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true, visible = "render")
    public int color = 0x3FFF4444;
    @Parameter(name = "DoubleMine")
    public boolean doubleMine = false;
    @Parameter(name = "MaxBlocks", min = 2, max = 10, step = 1)
    public double maxBlocks = 2;
    @Parameter(name = "Speed", min = 0.2, max = 5.0, step = 0.1)
    public double speed = 1.0;
    @Parameter(name = "Raycast")
    public boolean raycast = false;
    @Parameter(name = "GrimRange", min = 0.0, max = 6.0, step = 0.1, visible = "mode=Grim")
    public double grimRange = 4.5;
    @Parameter(name = "GrimMode", modes = {"Strict", "Normal", "Dev"}, visible = "mode=Grim")
    public String grimMode = "Strict";
    public static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_packetmine");
    public static class MiningBlock {
        public net.minecraft.core.BlockPos pos;
        public long startTime;
        public long breakAt;
        public boolean done;
        public boolean sentStop;
        public boolean started;
        public long visibleUntil;
        public String blockName;
        public MiningBlock(net.minecraft.core.BlockPos pos, long breakAt, String blockName) {
            this.pos = pos;
            this.startTime = System.currentTimeMillis();
            this.breakAt = breakAt;
            this.done = false;
            this.sentStop = false;
            this.started = false;
            this.visibleUntil = Long.MAX_VALUE;
            this.blockName = blockName;
        }
    }
    public static final List<MiningBlock> miningBlocks = new ArrayList<>();
    private int restoreSlot = -1;
    private int toolSlot = -1;
    private boolean needRestore = false;
    private boolean attackWasDown = false;
    public void onEnable() {
        miningBlocks.clear();
        restoreSlot = -1;
        toolSlot = -1;
        needRestore = false;
        attackWasDown = false;
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() != null && mc.getGameMode() != null) {
            mc.getGameMode().stopDestroyBlock();
        }
        for (var block : miningBlocks) {
            if (!block.sentStop) {
                if (!mode.equals("Grim")) {
                    sendStop(mc, block.pos);
                }
            }
        }
        miningBlocks.clear();
        restoreSlotNow();
        attackWasDown = false;
    }
    public long calcBreakTime(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        var state = BlockUtility.getState(mc.getLevel(), pos.getX(), pos.getY(), pos.getZ());
        float destroyProgress = state.getDestroyProgress(mc.getPlayer(), mc.getLevel(), pos);
        if (destroyProgress <= 0) return 2000;
        float ticks = (float)Math.ceil(1.0 / destroyProgress);
        long ms = (long)(ticks * 50);
        ms = Math.max(100, Math.min(mode.equals("Grim") ? 20000 : mode.equals("NCP") ? 10000 : 5000, ms));
        if (doubleMine) {
            ms = (long)(ms / speed);
        }
        return Math.max(50, ms);
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        boolean leftClick = mc.getOptions().keyAttack.isDown();
        boolean clicked = leftClick && !attackWasDown;
        attackWasDown = leftClick;
        if (clicked && mc.getHitResult() != null && mc.getHitResult().getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            net.minecraft.core.BlockPos target = ((net.minecraft.world.phys.BlockHitResult) mc.getHitResult()).getBlockPos();
            if (isBreakable(mc, target) && !isTargetBlock(target)) {
                int max = doubleMine ? (int) maxBlocks : 1;
                long activeCount = miningBlocks.stream().filter(m -> !m.done).count();
                if (activeCount >= max) return;
                String name = BlockUtility.getState(mc.getLevel(), target.getX(), target.getY(), target.getZ()).getBlock().getName().getString();
                long breakMs = calcBreakTime(mc, target);
                MiningBlock mb = new MiningBlock(target, breakMs, name);
                miningBlocks.add(mb);
                if (mode.equals("Grim")) {
                    mc.getOptions().keyAttack.setDown(false);
                }
            }
        }
        silentRotation.hasRotation = false;
        long now = System.currentTimeMillis();
        miningBlocks.removeIf(m -> m.done && now > m.visibleUntil);
        var server = mc.getSingleplayerServer();
        var serverLevel = (server != null) ? server.getLevel(mc.getLevel().dimension()) : null;
        net.minecraft.core.BlockPos firstPos = null;
        for (MiningBlock mb : miningBlocks) {
            if (!mb.done) { firstPos = mb.pos; break; }
        }
        if (firstPos != null) {
            toolSlot = autoTool ? findBestToolSlot(mc, firstPos) : -1;
            applySwap(mc);
            rotateTo(mc, firstPos);
        }
        if (mode.equals("Grim") && !doubleMine) {
            MiningBlock mb = miningBlocks.stream().filter(m -> !m.done).findFirst().orElse(null);
            if (mb != null) {
                if (!mb.started) {
                    mc.getGameMode().stopDestroyBlock();
                    net.minecraft.core.Direction dir = getDirection(mc, mb.pos);
                    mc.getGameMode().startDestroyBlock(mb.pos, dir);
                    mb.started = true;
                    mb.startTime = now;
                }
                net.minecraft.core.Direction dir = getDirection(mc, mb.pos);
                mc.getGameMode().continueDestroyBlock(mb.pos, dir);
                long predTime = now - mb.startTime;
                if (serverLevel != null && mc.getPlayer() != null) {
                    if (predTime >= mb.breakAt) {
                        serverLevel.destroyBlock(mb.pos, true, mc.getPlayer());
                        mb.done = true;
                        mb.visibleUntil = now + 2500;
                    }
                } else if (BlockUtility.isAir(mc.getLevel(), mb.pos) || predTime > 20000) {
                    mb.done = true;
                    mb.visibleUntil = now + 2500;
                }
            }
        } else {
            for (MiningBlock mb : miningBlocks) {
                if (mb.done) continue;
                if (now - mb.startTime >= mb.breakAt) {
                    if (serverLevel != null && mc.getPlayer() != null) {
                        serverLevel.destroyBlock(mb.pos, true, mc.getPlayer());
                    }
                    sendStart(mc, mb.pos, 0);
                    sendStop(mc, mb.pos);
                    mb.sentStop = true;
                    mb.done = true;
                    mb.visibleUntil = now + 2500;
                }
            }
            for (MiningBlock mb : miningBlocks) {
                if (mb.done) continue;
                sendStart(mc, mb.pos, 0);
            }
        }
        if (needRestore && miningBlocks.stream().noneMatch(m -> !m.done)) {
            restoreSlotNow();
        }
    }
    public boolean isTargetBlock(net.minecraft.core.BlockPos pos) {
        for (MiningBlock mb : miningBlocks) {
            if (mb.pos.equals(pos) && !mb.done) return true;
        }
        return false;
    }
    private boolean isBreakable(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        var state = BlockUtility.getState(mc.getLevel(), pos.getX(), pos.getY(), pos.getZ());
        if (state.isAir()) return false;
        if (BlockUtility.isBlock(state, "bedrock")) return false;
        if (BlockUtility.destroySpeed(mc.getLevel(), pos) < 0) return false;
        if (!mc.getLevel().getWorldBorder().isWithinBounds(pos)) return false;
        double dist = PhysicUtility.centerOf(pos).distanceTo(mc.getPlayer().getEyePosition());
        double maxDist = mode.equals("Grim") ? grimRange
            : mode.equals("NCP") ? 4.5 : range;
        if (dist > maxDist) return false;
        if (!checkVisibility(mc, pos)) return false;
        return true;
    }
    private void sendStart(MinecraftWrapper mc, net.minecraft.core.BlockPos pos, int seq) {
        NetworkUtility.sendStartDestroy(pos, getDirection(mc, pos), seq);
    }
    private void sendStop(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        NetworkUtility.sendStopDestroy(pos, getDirection(mc, pos), 0);
    }
    private net.minecraft.core.Direction getDirection(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.phys.Vec3 eye = mc.getPlayer().getEyePosition();
        net.minecraft.world.phys.Vec3 blockCenter = PhysicUtility.centerOf(pos);
        net.minecraft.world.phys.Vec3 diff = blockCenter.subtract(eye);
        double ax = Math.abs(diff.x), ay = Math.abs(diff.y), az = Math.abs(diff.z);
        if (ay >= ax && ay >= az) return diff.y > 0 ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.DOWN;
        if (ax >= az) return diff.x > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
        return diff.z > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
    }
    private int findBestToolSlot(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        var state = BlockUtility.getState(mc.getLevel(), pos.getX(), pos.getY(), pos.getZ());
        int bestSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        float bestSpeed = InventoryUtility.getItem(mc.getPlayer(), bestSlot).getDestroySpeed(state);
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (stack.isEmpty()) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot != InventoryUtility.getSelectedSlot(mc.getPlayer()) ? bestSlot : -1;
    }
    private void applySwap(MinecraftWrapper mc) {
        String swap = mode.equals("Grim") ? (swapMode.equals("None") ? "None" : "Normal") : swapMode;
        if (toolSlot < 0 || swap.equals("None")) return;
        int prev = InventoryUtility.getSelectedSlot(mc.getPlayer());
        if (swap.equals("Silent")) {
            NetworkUtility.sendSetCarriedItem(toolSlot);
        } else if (swap.equals("Normal")) {
            InventoryUtility.selectSlot(mc.getPlayer(), toolSlot);
        }
        restoreSlot = prev;
        needRestore = true;
    }
    private void restoreSlotNow() {
        if (!needRestore || !switchBack || restoreSlot < 0) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        String swap = mode.equals("Grim") ? (swapMode.equals("None") ? "None" : "Normal") : swapMode;
        if (swap.equals("Silent")) {
            NetworkUtility.sendSetCarriedItem(restoreSlot);
        } else if (swap.equals("Normal")) {
            InventoryUtility.selectSlot(mc.getPlayer(), restoreSlot);
        }
        needRestore = false;
    }
    private void rotateTo(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        String modeVal = rotate;
        if (modeVal.equals("None")) return;
        float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), PhysicUtility.centerOf(pos));
        if (modeVal.equals("Normal")) {
            mc.getPlayer().setYRot(angles[0]);
            mc.getPlayer().setXRot(angles[1]);
        } else if (modeVal.equals("Silent")) {
            silentRotation.set(angles[0], angles[1]);
        }
    }
    public static native int[] nativeFindTargets(
        double px, double py, double pz, double range, int maxResults, int targetBlockId);
    public static native boolean nativeCanSee(
        double ex, double ey, double ez,
        double tx, double ty, double tz,
        int[] solidBlocks);
    public static native int[] nativeFilterVisible(
        int[] candidates, int[] solidBlocks,
        double ex, double ey, double ez);

    private int[] collectSolidBlocks(MinecraftWrapper mc, net.minecraft.core.BlockPos center, double range) {
        Set<net.minecraft.core.BlockPos> blocks = new HashSet<>();
        int r = (int) Math.ceil(range);
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    net.minecraft.core.BlockPos p = center.offset(x, y, z);
                    var state = mc.getLevel().getBlockState(p);
                    if (!state.isAir() && state.canOcclude()) {
                        blocks.add(p.immutable());
                    }
                }
            }
        }
        int[] arr = new int[blocks.size() * 3];
        int i = 0;
        for (net.minecraft.core.BlockPos p : blocks) {
            arr[i++] = p.getX();
            arr[i++] = p.getY();
            arr[i++] = p.getZ();
        }
        return arr;
    }

    private boolean checkVisibility(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        if (!raycast || !NATIVE.isLoaded()) return true;
        net.minecraft.world.phys.Vec3 eye = mc.getPlayer().getEyePosition();
        double maxDist = mode.equals("Grim") ? grimRange
            : mode.equals("NCP") ? 4.5 : range;
        int[] solids = collectSolidBlocks(mc, mc.getPlayer().blockPosition(), maxDist + 2);
        return nativeCanSee(eye.x, eye.y, eye.z, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, solids);
    }




}