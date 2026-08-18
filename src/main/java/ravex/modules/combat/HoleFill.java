package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.chat.Component;
import ravex.utility.misc.block.BlockUtility;

import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "HoleFill", category = "Combat")
public class HoleFill {
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
    @Parameter(name = "Color", color = true, visible = "render")
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
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        long now = System.currentTimeMillis();
        switch (state) {
            case IDLE -> state = State.SEARCH;
            case SEARCH -> searchHoles(mc);
            case PLACING -> placeNext(mc, now);
            case DONE -> {
                if (autoDisable) Modules.setEnabled(HoleFill.class, false);
                else state = State.IDLE;
            }
        }
    }
    private void searchHoles(MinecraftWrapper mc) {
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
        var playerPos = mc.getPlayer().blockPosition();
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
    private void searchNative(MinecraftWrapper mc, double range) {
        int[] result = nativeFindHoles(
            mc.getPlayer().getX(), mc.getPlayer().getY(), mc.getPlayer().getZ(),
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
    private void searchJava(MinecraftWrapper mc, double range) {
        var playerPos = mc.getPlayer().blockPosition();
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
    private boolean isValidHole(MinecraftWrapper mc, int x, int y, int z) {
        int by = y - 1;
        if (by < mc.getLevel().getMinY()) return false;
        if (!mc.getLevel().getBlockState(BlockUtility.pos(x, y, z)).isAir()) return false;
        if (!BlockUtility.isSolid(mc.getLevel(), x, by, z)) return false;
        int ay = y + 1;
        if (ay >= mc.getLevel().getMaxY()) return false;
        if (!BlockUtility.isAir(mc.getLevel(), x, ay, z)) return false;
        int solidSides = 0;
        net.minecraft.core.Direction[] horizontals = {net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.WEST};
        for (net.minecraft.core.Direction dir : horizontals) {
            int nx = x + dir.getStepX(), ny = y + dir.getStepY(), nz = z + dir.getStepZ();
            if (!mc.getLevel().getWorldBorder().isWithinBounds(BlockUtility.pos(nx, ny, nz))) return false;
            if (BlockUtility.isSolid(mc.getLevel(), nx, ny, nz)) {
                solidSides++;
            }
        }
        if (fillAll) {
            return solidSides >= 2;
        }
        return solidSides >= 3;
    }
    private void placeNext(MinecraftWrapper mc, long now) {
        if (now - lastActionTime < (long) delay) return;
        lastActionTime = now;
        if (holeIndex >= holes.size()) {
            sendMsg(mc, "Filled " + totalPlaced + " block(s)");
            state = State.DONE;
            return;
        }
        long targetPacked = holes.get(holeIndex);
        if (!BlockUtility.isAir(mc.getLevel(), BlockUtility.unpackX(targetPacked), BlockUtility.unpackY(targetPacked), BlockUtility.unpackZ(targetPacked))) {
            holeIndex++;
            return;
        }
        int slot = findBlockSlot(mc);
        if (slot == -1) {
            sendMsg(mc, "Not enough blocks, disabling");
            Modules.setEnabled(HoleFill.class, false);
            return;
        }
        if (!BlockUtility.placeBlock(mc, BlockUtility.fromPacked(targetPacked), slot)) {
            holeIndex++;
            return;
        }
        totalPlaced++;
        holeIndex++;
    }
    private int findBlockSlot(MinecraftWrapper mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (InventoryUtility.isItem(stack, "obsidian") || InventoryUtility.isItem(stack, "crying_obsidian")) return i;
        }
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (InventoryUtility.isBlockItem(stack)) return i;
        }
        return -1;
    }
    private void sendMsg(MinecraftWrapper mc, String msg) {
        if (mc.getPlayer() != null) {
            mc.getPlayer().displayClientMessage(Component.literal("§8[§5HoleFill§8] §7" + msg), false);
        }
    }
    private static native int[] nativeFindHoles(
        double px, double py, double pz, double range, int maxResults);





}