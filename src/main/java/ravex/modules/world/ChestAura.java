package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.misc.block.BlockUtility;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "ChestAura", category = "World")
public class ChestAura {
    @Parameter(name = "Range", min = 2.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "Delay", min = 0.0, max = 20.0, step = 1.0)
    public double delay = 2.0;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true, visible = "render")
    public int highlightColor = 0xFF00FF88;
    @Parameter(name = "FadeDuration", min = 0.1, max = 3.0, step = 0.1)
    public double fadeSpeed = 1.0;
    @Parameter(name = "Filled")
    public boolean filled = true;
    @Parameter(name = "AutoSwap")
    public boolean autoSwap = true;
    @Parameter(name = "Silent")
    public boolean silent = true;
    public static class PlacedChest {
        public final long packedPos;
        public final long placeTime;
        public PlacedChest(long packedPos, long placeTime) {
            this.packedPos = packedPos;
            this.placeTime = placeTime;
        }
    }
    public static final List<PlacedChest> placedChests = new CopyOnWriteArrayList<>();
    private int delayTimer = 0;
    public void onEnable() {
        delayTimer = 0;
        placedChests.clear();
    }
    public void onDisable() {
        placedChests.clear();
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getLevel() == null) return;
        long now = System.currentTimeMillis();
        double durationMs = fadeSpeed * 1000.0;
        placedChests.removeIf(chest -> (now - chest.placeTime) > durationMs);
        if (delayTimer > 0) {
            delayTimer--;
            return;
        }
        int chestSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(p, i);
            if (!stack.isEmpty() && (InventoryUtility.isItem(stack, "chest") || InventoryUtility.isItem(stack, "trapped_chest"))) {
                chestSlot = i;
                break;
            }
        }
        if (chestSlot == -1) return;
        double r = range;
        var playerPos = p.blockPosition();
        long targetPacked = 0;
        boolean hasTarget = false;
        double closestDistSq = r * r;
        for (var entity : mc.getLevel().entitiesForRendering()) {
            if (entity == p || !entity.isAlive() || !(entity instanceof net.minecraft.world.entity.LivingEntity)) continue;
            if (p.distanceTo(entity) > r) continue;
            var entityPos = entity.blockPosition();
            int ex = entityPos.getX(), ey = entityPos.getY(), ez = entityPos.getZ();
            for (var dir : net.minecraft.core.Direction.values()) {
                if (dir == net.minecraft.core.Direction.DOWN || dir == net.minecraft.core.Direction.UP) continue;
                int ax = ex + dir.getStepX(), ay = ey + dir.getStepY(), az = ez + dir.getStepZ();
                if (BlockUtility.isAir(mc.getLevel(), ax, ay, az)) {
                    int by = BlockUtility.belowY(ay);
                    if (BlockUtility.isSolid(mc.getLevel(), ax, by, az)) {
                        double distSq = p.distanceToSqr(ax + 0.5, ay + 0.5, az + 0.5);
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            targetPacked = BlockUtility.packPos(ax, ay, az);
                            hasTarget = true;
                        }
                    }
                }
            }
        }
        if (!hasTarget) {
            int rangeInt = (int) Math.ceil(r);
            int px = playerPos.getX(), py = playerPos.getY(), pz = playerPos.getZ();
            for (int x = -rangeInt; x <= rangeInt; x++) {
                for (int y = -rangeInt; y <= rangeInt; y++) {
                    for (int z = -rangeInt; z <= rangeInt; z++) {
                        int wx = px + x, wy = py + y, wz = pz + z;
                        double distSq = p.distanceToSqr(wx + 0.5, wy + 0.5, wz + 0.5);
                        if (distSq < closestDistSq) {
                            if (BlockUtility.isAir(mc.getLevel(), wx, wy, wz)) {
                                int by = BlockUtility.belowY(wy);
                                if (BlockUtility.isSolid(mc.getLevel(), wx, by, wz)) {
                                    closestDistSq = distSq;
                                    targetPacked = BlockUtility.packPos(wx, wy, wz);
                                    hasTarget = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (hasTarget) {
            int tx = BlockUtility.unpackX(targetPacked), ty = BlockUtility.unpackY(targetPacked), tz = BlockUtility.unpackZ(targetPacked);
            int prevSlot = InventoryUtility.getSelectedSlot(p);
            if (autoSwap && chestSlot != prevSlot) {
                InventoryUtility.selectSlot(p, chestSlot);
            }
            var below = BlockUtility.pos(tx, BlockUtility.belowY(ty), tz);
            BlockUtility.useItemOn(ravex.mcwrapper.MinecraftWrapper.getWrapper(), new net.minecraft.world.phys.BlockHitResult(
                PhysicUtility.centerOf(below).add(0, 0.5, 0),
                net.minecraft.core.Direction.UP, below, false));
            ravex.utility.player.SwingUtility.swingMainHand(p);
            if (autoSwap && silent && chestSlot != prevSlot) {
                InventoryUtility.selectSlot(p, prevSlot);
            }
            placedChests.add(new PlacedChest(targetPacked, now));
            delayTimer = (int) delay;
        }
    }





}