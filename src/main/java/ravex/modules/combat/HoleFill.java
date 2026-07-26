package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.network.chat.Component;
import ravex.utility.misc.PhysicUtility;

import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
@ModuleInfo(name = "HoleFill", category = "Combat")
public class HoleFill implements ModuleAccess {
    @Parameter(name = "Range", min = 2.0, max = 8.0, step = 0.5)
    public double range = 4.0;
    @Parameter(name = "Delay", min = 20, max = 300, step = 10)
    public double delay = 80;
    @Parameter(name = "MaxBlocks", min = 1, max = 24, step = 1)
    public double maxBlocks = 6;
    @Parameter(name = "FillAll")
    public boolean fillAll = false;
    @Parameter(name = "AutoDisable")
    public boolean autoDisable = true;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true)
    public int color = 0x3F00FF00;
    public static List<Long> holePositions = new ArrayList<>();
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_holefill");
    static {
        NATIVE.load();
    }
    private enum State { IDLE, SEARCH, PLACING, DONE }
    private State state = State.IDLE;
    private List<Long> holes = new ArrayList<>();
    private int holeIndex = 0;
    private long lastActionTime = 0;
    private int totalPlaced = 0;
    public void onEnable() {
        state = State.IDLE;
        holes.clear();
        holeIndex = 0;
        totalPlaced = 0;
    }
    public void onDisable() {
        state = State.IDLE;
        holes.clear();
        holeIndex = 0;
    }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        long now = System.currentTimeMillis();
        switch (state) {
            case IDLE -> state = State.SEARCH;
            case SEARCH -> searchHoles(mc);
            case PLACING -> placeNext(mc, now);
            case DONE -> {
                if (autoDisable) ravex.manager.ModuleManager.INSTANCE.getByName("HoleFill").setEnabled(false);
                else state = State.IDLE;
            }
        }
    }
    private void searchHoles(Minecraft mc) {
        holes.clear();
        holePositions.clear();
        holeIndex = 0;
        double r = range;
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
        int max = (int) maxBlocks;
        if (holes.size() > max) holes = holes.subList(0, max);
        holePositions.clear();
        holePositions.addAll(holes);
        sendMsg(mc, "Found " + holes.size() + " hole(s)");
        state = State.PLACING;
    }
    private void searchNative(Minecraft mc, double range) {
        int[] result = nativeFindHoles(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            range, (int) maxBlocks * 2
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
        net.minecraft.core.Direction[] horizontals = {net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.WEST};
        for (net.minecraft.core.Direction dir : horizontals) {
            int nx = x + dir.getStepX(), ny = y + dir.getStepY(), nz = z + dir.getStepZ();
            if (!mc.level.getWorldBorder().isWithinBounds(BlockUtility.pos(nx, ny, nz))) return false;
            if (BlockUtility.isSolid(mc.level, nx, ny, nz)) {
                solidSides++;
            }
        }
        if (fillAll) {
            return solidSides >= 2;
        }
        return solidSides >= 3;
    }
    private void placeNext(Minecraft mc, long now) {
        if (now - lastActionTime < (long) delay) return;
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
            ravex.manager.ModuleManager.INSTANCE.getByName("HoleFill").setEnabled(false);
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

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("HoleFill").getEnabled();
    }
    public static HoleFill itz() {
        return ravex.manager.ModuleManager.delegate(HoleFill.class);
    }


}