package ravex.modules.player;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.network.NetworkUtility;
import ravex.utility.nativelib.NativeLibrary;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotation;
import java.util.ArrayList;
import java.util.List;
public class PacketMine extends Module {
    public static final PacketMine INSTANCE = new PacketMine();
    public final NumberParameter range = new NumberParameter("Range", 6.0, 2.0, 10.0, 0.5);
    public final BooleanParameter grimStrict = new BooleanParameter("GrimStrict", false);
    public final NumberParameter grimRange = new NumberParameter("GrimRange", 4.3, 2.0, 10.0, 0.1);
    public final BooleanParameter doubleMine = new BooleanParameter("DoubleMine", false);
    public final NumberParameter maxBlocks = new NumberParameter("MaxBlocks", 2, 2, 10, 1);
    public final NumberParameter speed = new NumberParameter("Speed", 1.0, 0.2, 5.0, 0.1);
    public final ModeParameter rotate = new ModeParameter("Rotate", "Silent",
        java.util.List.of("Silent", "Normal", "None"));
    public final ModeParameter swapMode = new ModeParameter("SwapMode", "Silent",
        java.util.List.of("Silent", "Normal", "None"));
    public final BooleanParameter autoTool = new BooleanParameter("AutoTool", true);
    public final BooleanParameter switchBack = new BooleanParameter("SwitchBack", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3FFF4444);
    public static final SilentRotation silentRotation = new SilentRotation();
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_packetmine");
    static {
        NATIVE.load();
    }
    public static class MiningBlock {
        public BlockPos pos;
        public long startTime;
        public long breakAt;
        public boolean done;
        public boolean sentStop;
        public boolean started;
        public long visibleUntil;
        public String blockName;
        public MiningBlock(BlockPos pos, long breakAt, String blockName) {
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
    private PacketMine() {
        super("PacketMine");
        grimRange.setVisible(grimStrict::getValue);
        maxBlocks.setVisible(doubleMine::getValue);
        speed.setVisible(doubleMine::getValue);
    }
    @Override
    protected void onEnable() {
        miningBlocks.clear();
        restoreSlot = -1;
        toolSlot = -1;
        needRestore = false;
        attackWasDown = false;
    }
    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
        for (var block : miningBlocks) {
            if (!block.sentStop) {
                if (!grimStrict.getValue()) {
                    sendStop(mc, block.pos);
                }
            }
        }
        miningBlocks.clear();
        restoreSlotNow();
        attackWasDown = false;
    }
    public long calcBreakTime(Minecraft mc, BlockPos pos) {
        var state = BlockUtility.getState(mc.level, pos.getX(), pos.getY(), pos.getZ());
        float destroyProgress = state.getDestroyProgress(mc.player, mc.level, pos);
        if (destroyProgress <= 0) return 2000;
        float ticks = (float)Math.ceil(1.0 / destroyProgress);
        long ms = (long)(ticks * 50);
        ms = Math.max(100, Math.min(grimStrict.getValue() ? 20000 : 5000, ms));
        if (doubleMine.getValue()) {
            ms = (long)(ms / speed.getValue());
        }
        return Math.max(50, ms);
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        boolean leftClick = mc.options.keyAttack.isDown();
        boolean clicked = leftClick && !attackWasDown;
        attackWasDown = leftClick;
        if (clicked && mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            BlockPos target = ((net.minecraft.world.phys.BlockHitResult) mc.hitResult).getBlockPos();
            if (isBreakable(mc, target) && !isTargetBlock(target)) {
                int max = doubleMine.getValue() ? maxBlocks.getValue().intValue() : 1;
                long activeCount = miningBlocks.stream().filter(m -> !m.done).count();
                if (activeCount >= max) return;
                String name = BlockUtility.getState(mc.level, target.getX(), target.getY(), target.getZ()).getBlock().getName().getString();
                long breakMs = calcBreakTime(mc, target);
                MiningBlock mb = new MiningBlock(target, breakMs, name);
                miningBlocks.add(mb);
                if (grimStrict.getValue()) {
                    mc.options.keyAttack.setDown(false);
                }
            }
        }
        silentRotation.hasRotation = false;
        long now = System.currentTimeMillis();
        miningBlocks.removeIf(m -> m.done && now > m.visibleUntil);
        var server = mc.getSingleplayerServer();
        var serverLevel = (server != null) ? server.getLevel(mc.level.dimension()) : null;
        BlockPos firstPos = null;
        for (MiningBlock mb : miningBlocks) {
            if (!mb.done) { firstPos = mb.pos; break; }
        }
        if (firstPos != null) {
            toolSlot = autoTool.getValue() ? findBestToolSlot(mc, firstPos) : -1;
            applySwap(mc);
            rotateTo(mc, firstPos);
        }
        if (grimStrict.getValue() && !doubleMine.getValue()) {
            MiningBlock mb = miningBlocks.stream().filter(m -> !m.done).findFirst().orElse(null);
            if (mb != null) {
                if (!mb.started) {
                    mc.gameMode.stopDestroyBlock();
                    Direction dir = getDirection(mc, mb.pos);
                    mc.gameMode.startDestroyBlock(mb.pos, dir);
                    mb.started = true;
                    mb.startTime = now;
                }
                Direction dir = getDirection(mc, mb.pos);
                mc.gameMode.continueDestroyBlock(mb.pos, dir);
                long predTime = now - mb.startTime;
                if (serverLevel != null && mc.player != null) {
                    if (predTime >= mb.breakAt) {
                        serverLevel.destroyBlock(mb.pos, true, mc.player);
                        mb.done = true;
                        mb.visibleUntil = now + 2500;
                    }
                } else if (BlockUtility.isAir(mc.level, mb.pos) || predTime > 20000) {
                    mb.done = true;
                    mb.visibleUntil = now + 2500;
                }
            }
        } else {
            for (MiningBlock mb : miningBlocks) {
                if (mb.done) continue;
                if (now - mb.startTime >= mb.breakAt) {
                    if (serverLevel != null && mc.player != null) {
                        serverLevel.destroyBlock(mb.pos, true, mc.player);
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
    public boolean isTargetBlock(BlockPos pos) {
        for (MiningBlock mb : miningBlocks) {
            if (mb.pos.equals(pos) && !mb.done) return true;
        }
        return false;
    }
    private boolean isBreakable(Minecraft mc, BlockPos pos) {
        var state = BlockUtility.getState(mc.level, pos.getX(), pos.getY(), pos.getZ());
        if (state.isAir()) return false;
        if (BlockUtility.isBlock(state, "bedrock")) return false;
        if (BlockUtility.destroySpeed(mc.level, pos) < 0) return false;
        if (!mc.level.getWorldBorder().isWithinBounds(pos)) return false;
        double dist = Vec3.atCenterOf(pos).distanceTo(mc.player.getEyePosition());
        double maxDist = grimStrict.getValue() ? grimRange.getValue() : range.getValue();
        return dist <= maxDist;
    }
    private void sendStart(Minecraft mc, BlockPos pos, int seq) {
        NetworkUtility.sendStartDestroy(pos, getDirection(mc, pos), seq);
    }
    private void sendStop(Minecraft mc, BlockPos pos) {
        NetworkUtility.sendStopDestroy(pos, getDirection(mc, pos), 0);
    }
    private Direction getDirection(Minecraft mc, BlockPos pos) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 blockCenter = Vec3.atCenterOf(pos);
        Vec3 diff = blockCenter.subtract(eye);
        double ax = Math.abs(diff.x), ay = Math.abs(diff.y), az = Math.abs(diff.z);
        if (ay >= ax && ay >= az) return diff.y > 0 ? Direction.UP : Direction.DOWN;
        if (ax >= az) return diff.x > 0 ? Direction.EAST : Direction.WEST;
        return diff.z > 0 ? Direction.SOUTH : Direction.NORTH;
    }
    private int findBestToolSlot(Minecraft mc, BlockPos pos) {
        var state = BlockUtility.getState(mc.level, pos.getX(), pos.getY(), pos.getZ());
        int bestSlot = InventoryUtility.getSelectedSlot(mc.player);
        float bestSpeed = InventoryUtility.getItem(mc.player, bestSlot).getDestroySpeed(state);
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (stack.isEmpty()) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot != InventoryUtility.getSelectedSlot(mc.player) ? bestSlot : -1;
    }
    private void applySwap(Minecraft mc) {
        String swap = grimStrict.getValue() ? (swapMode.getValue().equals("None") ? "None" : "Normal") : swapMode.getValue();
        if (toolSlot < 0 || swap.equals("None")) return;
        int prev = InventoryUtility.getSelectedSlot(mc.player);
        if (swap.equals("Silent")) {
            NetworkUtility.sendSetCarriedItem(toolSlot);
        } else if (swap.equals("Normal")) {
            InventoryUtility.selectSlot(mc.player, toolSlot);
        }
        restoreSlot = prev;
        needRestore = true;
    }
    private void restoreSlotNow() {
        if (!needRestore || !switchBack.getValue() || restoreSlot < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String swap = grimStrict.getValue() ? (swapMode.getValue().equals("None") ? "None" : "Normal") : swapMode.getValue();
        if (swap.equals("Silent")) {
            NetworkUtility.sendSetCarriedItem(restoreSlot);
        } else if (swap.equals("Normal")) {
            InventoryUtility.selectSlot(mc.player, restoreSlot);
        }
        needRestore = false;
    }
    private void rotateTo(Minecraft mc, BlockPos pos) {
        String mode = rotate.getValue();
        if (mode.equals("None")) return;
        float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), Vec3.atCenterOf(pos));
        if (mode.equals("Normal")) {
            mc.player.setYRot(angles[0]);
            mc.player.setXRot(angles[1]);
        } else if (mode.equals("Silent")) {
            silentRotation.set(angles[0], angles[1]);
        }
    }
    public static native int[] nativeFindTargets(
        double px, double py, double pz, double range, int maxResults, int targetBlockId);
}
