package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.misc.NativeLoader;

public class PortalBuild extends Module {
    public static final PortalBuild INSTANCE = new PortalBuild();

    public final NumberParameter range = new NumberParameter("Range", 6.0, 2.0, 12.0, 0.5);
    public final NumberParameter minRange = new NumberParameter("Min Range", 2.0, 1.0, 4.0, 0.5);
    public final NumberParameter avoidRange = new NumberParameter("Avoid", 8.0, 1.0, 24.0, 1.0);
    public final BooleanParameter build = new BooleanParameter("Build", true);
    public final BooleanParameter light = new BooleanParameter("Light", true);
    public final NumberParameter portalsToBuild = new NumberParameter("Portals", 2.0, 1.0, 6.0, 1.0);
    public final BooleanParameter autoDisable = new BooleanParameter("Auto Disable", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3FAA00FF);

    public static BlockPos currentTarget = null;

    private enum State { IDLE, FIND, BUILDING, RETRY, VERIFY, LIGHTING, DONE }
    private State state = State.IDLE;
    private BlockPos portalBase = null;
    private int frameIndex = 0;
    private int retries = 0;
    private BlockPos lastFailedPos = null;
    private long lastActionTime = 0;
    private int portalBuildCount = 0;
    private BlockPos firstPortalBase = null;
    private int portalSpacing = 5;

    private static final int[][] FRAME_OFFSETS = {
        {0, 0}, {1, 0}, {2, 0}, {3, 0},
        {0, 1}, {0, 2}, {0, 3},
        {3, 1}, {3, 2}, {3, 3},
        {0, 4}, {1, 4}, {2, 4}, {3, 4},
    };

    private static boolean nativeAvailable = false;
    static {
        try {
            nativeAvailable = NativeLoader.loadLibrary("ravex_portalbuild");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[PortalBuild JNI] " + e.getMessage());
        }
    }

    private PortalBuild() {
        super("PortalBuild", Category.WORLD);
        addParameter(range);
        addParameter(minRange);
        addParameter(avoidRange);
        addParameter(build);
        addParameter(light);
        addParameter(portalsToBuild);
        addParameter(autoDisable);
        addParameter(render);
        addParameter(color);
    }

    @Override
    protected void onEnable() {
        state = State.IDLE;
        portalBase = null;
        frameIndex = 0;
        retries = 0;
        lastFailedPos = null;
        currentTarget = null;
        portalBuildCount = 0;
        firstPortalBase = null;
    }

    @Override
    protected void onDisable() {
        state = State.IDLE;
        portalBase = null;
        frameIndex = 0;
        currentTarget = null;
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

        if (firstPortalBase != null) {
            px = firstPortalBase.getX() + portalBuildCount * portalSpacing;
            pz = firstPortalBase.getZ();
        } else {
            px = mc.player.getX();
            pz = mc.player.getZ();
            float yaw = mc.player.getYRot();
            double[] result = new double[4];
            if (nativeAvailable) {
                double[] existingPortals = findExistingPortals(mc);
                nativeFindBestPortalPos(px, py, pz, yaw,
                    minRange.getValue(), range.getValue(),
                    avoidRange.getValue(), existingPortals, null, result);
                if (result[3] < 0) return;
                px = result[0];
                py = result[1];
                pz = result[2];
            } else {
                px = px + Math.sin(-yaw * Math.PI / 180) * 4;
                pz = pz + Math.cos(yaw * Math.PI / 180) * 4;
            }
        }

        // find ground: scan from py+3 down to py-10
        int startY = (int)Math.round(py) + 3;
        BlockPos ground = null;
        for (int y = startY; y >= startY - 16; y--) {
            if (y < mc.level.getMinY()) break;
            BlockPos check = BlockPos.containing(px, y, pz);
            BlockPos below = check.below();
            if (below.getY() < mc.level.getMinY()) break;
            if (mc.level.getBlockState(below).isCollisionShapeFullBlock(mc.level, below)
                && mc.level.getBlockState(check).isAir()) {
                ground = check;
                break;
            }
        }
        if (ground == null) return;

        for (int[] off : FRAME_OFFSETS) {
            BlockPos p = ground.offset(off[0], off[1], 0);
            if (!mc.level.getBlockState(p).isAir()
                && !mc.level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.OBSIDIAN)) return;
        }

        portalBase = ground;
        if (firstPortalBase == null) firstPortalBase = ground;
        frameIndex = 0;
        retries = 0;
        state = build.getValue() ? State.BUILDING : State.VERIFY;
    }

    private void tryPlaceNext(Minecraft mc, long now) {
        if (now - lastActionTime < 100) return;
        lastActionTime = now;

        if (portalBase == null) { state = State.IDLE; return; }
        if (frameIndex >= FRAME_OFFSETS.length) {
            state = State.VERIFY;
            return;
        }

        int[] off = FRAME_OFFSETS[frameIndex];
        BlockPos targetPos = portalBase.offset(off[0], off[1], 0);
        currentTarget = targetPos;

        BlockState existing = mc.level.getBlockState(targetPos);
        if (existing.is(net.minecraft.world.level.block.Blocks.OBSIDIAN)
            || existing.getBlock() instanceof NetherPortalBlock) {
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
        mc.player.getInventory().setSelectedSlot(prev);

        lastActionTime = now;
        retries = 0;
        frameIndex++;
    }

    private void retryPlace(Minecraft mc, long now) {
        if (now - lastActionTime < 200) return;
        lastActionTime = now;

        if (lastFailedPos == null) { state = State.BUILDING; return; }

        retries++;
        if (retries > 5) {
            frameIndex++;
            retries = 0;
            lastFailedPos = null;
            state = State.BUILDING;
            return;
        }

        BlockState st = mc.level.getBlockState(lastFailedPos);
        if (st.is(net.minecraft.world.level.block.Blocks.OBSIDIAN)) {
            frameIndex++;
            retries = 0;
            lastFailedPos = null;
            state = State.BUILDING;
            return;
        }

        int slot = findObsidianSlot(mc);
        if (slot == -1) {
            sendMsg(mc, "Not enough obsidian, disabling");
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
        mc.player.getInventory().setSelectedSlot(prev);
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

    private void doVerify(Minecraft mc, long now) {
        if (portalBase == null) { state = State.IDLE; return; }

        boolean allPlaced = true;
        for (int[] off : FRAME_OFFSETS) {
            BlockPos p = portalBase.offset(off[0], off[1], 0);
            if (!mc.level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.OBSIDIAN)) {
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

        if (portalBase == null) { state = State.IDLE; return; }

        BlockPos interior = portalBase.offset(1, 1, 0);
        currentTarget = interior;

        int slot = findFlintOrFireChargeSlot(mc);
        if (slot == -1) {
            state = State.DONE;
            return;
        }

        int prev = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(interior), Direction.UP, interior, false));
        mc.player.swing(InteractionHand.MAIN_HAND);
        mc.player.getInventory().setSelectedSlot(prev);

        state = State.DONE;
    }

    private void doDone(Minecraft mc) {
        currentTarget = null;
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
            mc.player.displayClientMessage(Component.literal("§8[§5PortalBuild§8] §7" + msg), false);
        }
    }

    private double[] findExistingPortals(Minecraft mc) {
        double r = avoidRange.getValue();
        Vec3 eye = mc.player.getEyePosition();
        java.util.ArrayList<Double> list = new java.util.ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
            (int)(eye.x - r), (int)(eye.y - 5), (int)(eye.z - r),
            (int)(eye.x + r), (int)(eye.y + 5), (int)(eye.z + r))) {
            if (mc.level.getBlockState(pos).getBlock() instanceof NetherPortalBlock) {
                list.add((double)pos.getX());
                list.add((double)pos.getY());
                list.add((double)pos.getZ());
            }
        }
        if (list.isEmpty()) return null;
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private int findObsidianSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.OBSIDIAN)) return i;
        }
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.OBSIDIAN)) {
                int free = findEmptySlot(mc);
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

    private int findFlintOrFireChargeSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            var item = mc.player.getInventory().getItem(i);
            if (item.is(Items.FLINT_AND_STEEL) || item.is(Items.FIRE_CHARGE)) return i;
        }
        return -1;
    }

    private int findEmptySlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) return i;
        }
        return -1;
    }

    private static native void nativeFindBestPortalPos(
        double px, double py, double pz, double yaw,
        double minDist, double maxDist, double avoidRange,
        double[] existingPortals, double[] groundHeights, double[] out);
}
