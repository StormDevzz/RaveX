package ravex.modules.combat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import ravex.parameter.ColorParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.misc.block.BlockUtility;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
public class HoleFill extends Module {
    public static final HoleFill INSTANCE = new HoleFill();
    public final NumberParameter range = new NumberParameter("Range", 4.0, 2.0, 8.0, 0.5);
    public final NumberParameter delay = new NumberParameter("Delay", 80, 20, 300, 10);
    public final NumberParameter maxBlocks = new NumberParameter("MaxBlocks", 6, 1, 24, 1);
    public final BooleanParameter fillAll = new BooleanParameter("FillAll", false);
    public final BooleanParameter autoDisable = new BooleanParameter("AutoDisable", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3F00FF00);
    public static List<Long> holePositions = new ArrayList<>();
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_holefill");
    static {
        NATIVE.load();
    }
    private enum State { IDLE, SEARCH, PLACING, DONE }
    private State state = State.IDLE;
    private List<Long> holes = new ArrayList<>();
    private int holeIndex = 0;
    private long lastActionTime = 0;
    private int totalPlaced = 0;

    @Override
    protected void onEnable() {
        state = State.IDLE;
        holes.clear();
        holeIndex = 0;
        totalPlaced = 0;
    }
    @Override
    protected void onDisable() {
        state = State.IDLE;
        holes.clear();
        holeIndex = 0;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        long now = System.currentTimeMillis();
        switch (state) {
            case IDLE -> state = State.SEARCH;
            case SEARCH -> searchHoles(mc);
            case PLACING -> placeNext(mc, now);
            case DONE -> {
                if (autoDisable.getValue()) setEnabled(false);
                else state = State.IDLE;
            }
        }
    }
    private void searchHoles(Minecraft mc) {
        holes.clear();
        holePositions.clear();
        holeIndex = 0;
        double r = range.getValue();
        if (NATIVE.isLoaded()) {
            searchNative(mc, r);
        } else {
            searchJava(mc, r);
        }
        if (holes.isEmpty()) {
            state = State.DONE;
            return;
        }
        var playerPos = mc.player.blockPosition();
        int px = playerPos.getX(), py = playerPos.getY(), pz = playerPos.getZ();
        holes.sort(Comparator.comparingDouble(p -> {
            double dx = BlockUtility.unpackX(p) - px;
            double dy = BlockUtility.unpackY(p) - py;
            double dz = BlockUtility.unpackZ(p) - pz;
            return dx * dx + dy * dy + dz * dz;
        }));
        int max = maxBlocks.getValue().intValue();
        if (holes.size() > max) holes = holes.subList(0, max);
        holePositions.clear();
        holePositions.addAll(holes);
        sendMsg(mc, "Found " + holes.size() + " hole(s)");
        state = State.PLACING;
    }
    private void searchNative(Minecraft mc, double range) {
        int[] result = nativeFindHoles(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            range, maxBlocks.getValue().intValue() * 2
        );
        if (result == null) return;
        for (int i = 0; i < result.length; i += 3) {
            long packed = BlockUtility.packPos(result[i], result[i + 1], result[i + 2]);
            if (isValidHole(mc, result[i], result[i + 1], result[i + 2])) {
                holes.add(packed);
            }
        }
    }
    private void searchJava(Minecraft mc, double range) {
        var playerPos = mc.player.blockPosition();
        int px = playerPos.getX(), py = playerPos.getY(), pz = playerPos.getZ();
        int r = (int) Math.ceil(range);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > range * range) continue;
                for (int dy = -2; dy <= 1; dy++) {
                    int x = px + dx, y = py + dy, z = pz + dz;
                    if (!isValidHole(mc, x, y, z)) continue;
                    long packed = BlockUtility.packPos(x, y, z);
                    boolean dup = false;
                    for (long existing : holes) {
                        double ex = BlockUtility.unpackX(existing) - x;
                        double ey = BlockUtility.unpackY(existing) - y;
                        double ez = BlockUtility.unpackZ(existing) - z;
                        if (ex * ex + ey * ey + ez * ez < 2.0) {
                            dup = true;
                            break;
                        }
                    }
                    if (!dup) holes.add(packed);
                }
            }
        }
    }
    private boolean isValidHole(Minecraft mc, int x, int y, int z) {
        int by = y - 1;
        if (by < mc.level.getMinY()) return false;
        if (!mc.level.getBlockState(BlockUtility.pos(x, y, z)).isAir()) return false;
        if (!BlockUtility.isSolid(mc.level, x, by, z)) return false;
        int ay = y + 1;
        if (ay >= mc.level.getMaxY()) return false;
        if (!BlockUtility.isAir(mc.level, x, ay, z)) return false;
        int solidSides = 0;
        Direction[] horizontals = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction dir : horizontals) {
            int nx = x + dir.getStepX(), ny = y + dir.getStepY(), nz = z + dir.getStepZ();
            if (!mc.level.getWorldBorder().isWithinBounds(BlockUtility.pos(nx, ny, nz))) return false;
            if (BlockUtility.isSolid(mc.level, nx, ny, nz)) {
                solidSides++;
            }
        }
        if (fillAll.getValue()) {
            return solidSides >= 2;
        }
        return solidSides >= 3;
    }
    private void placeNext(Minecraft mc, long now) {
        if (now - lastActionTime < delay.getValue().longValue()) return;
        lastActionTime = now;
        if (holeIndex >= holes.size()) {
            sendMsg(mc, "Filled " + totalPlaced + " block(s)");
            state = State.DONE;
            return;
        }
        long targetPacked = holes.get(holeIndex);
        if (!BlockUtility.isAir(mc.level, BlockUtility.unpackX(targetPacked), BlockUtility.unpackY(targetPacked), BlockUtility.unpackZ(targetPacked))) {
            holeIndex++;
            return;
        }
        int slot = findBlockSlot(mc);
        if (slot == -1) {
            sendMsg(mc, "Not enough blocks, disabling");
            setEnabled(false);
            return;
        }
        if (!BlockUtility.placeBlock(mc, BlockUtility.fromPacked(targetPacked), slot)) {
            holeIndex++;
            return;
        }
        totalPlaced++;
        holeIndex++;
    }
    private int findBlockSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (InventoryUtility.isItem(stack, "obsidian") || InventoryUtility.isItem(stack, "crying_obsidian")) return i;
        }
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (InventoryUtility.isBlockItem(stack)) return i;
        }
        return -1;
    }
    private void sendMsg(Minecraft mc, String msg) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§8[§5HoleFill§8] §7" + msg), false);
        }
    }
    private static native int[] nativeFindHoles(
        double px, double py, double pz, double range, int maxResults);
}
