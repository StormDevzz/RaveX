package ravex.modules.world;
<<<<<<< HEAD
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import ravex.manager.ModuleManager;
import ravex.utility.player.InventoryUtility;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.client.Minecraft;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
public class ChestAura extends Module {
<<<<<<< HEAD
=======
    public static final ChestAura INSTANCE = new ChestAura();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter range = new NumberParameter("Range", 4.5, 2.0, 6.0, 0.1);
    public final NumberParameter delay = new NumberParameter("Delay", 2.0, 0.0, 20.0, 1.0);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter highlightColor = new ColorParameter("Color", 0xFF00FF88);
    public final NumberParameter fadeSpeed = new NumberParameter("FadeDuration", 1.0, 0.1, 3.0, 0.1);
    public final BooleanParameter filled = new BooleanParameter("Filled", true);
    public final BooleanParameter autoSwap = new BooleanParameter("AutoSwap", true);
    public final BooleanParameter silent = new BooleanParameter("Silent", true);
    public static class PlacedChest {
        public final long packedPos;
        public final long placeTime;
<<<<<<< HEAD
        public PlacedChest(long packedPos, long placeTime) {
            this.packedPos = packedPos;
=======
        public PlacedChest(BlockPos pos, long placeTime) {
            this.pos = pos;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            this.placeTime = placeTime;
        }
    }
    public static final List<PlacedChest> placedChests = new CopyOnWriteArrayList<>();
    private int delayTimer = 0;
    private ChestAura() {
        super("ChestAura");
        highlightColor.setVisible(render::getValue);
        fadeSpeed.setVisible(render::getValue);
        filled.setVisible(render::getValue);
    }
    @Override
    protected void onEnable() {
        delayTimer = 0;
        placedChests.clear();
    }
    @Override
    protected void onDisable() {
        placedChests.clear();
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        double durationMs = fadeSpeed.getValue() * 1000.0;
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
<<<<<<< HEAD
        if (chestSlot == -1) return;
=======
        if (chestSlot == -1) return; 
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        double r = range.getValue();
        var playerPos = p.blockPosition();
        long targetPacked = 0;
        boolean hasTarget = false;
        double closestDistSq = r * r;
<<<<<<< HEAD
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity == p || !entity.isAlive() || !(entity instanceof net.minecraft.world.entity.LivingEntity)) continue;
            if (p.distanceTo(entity) > r) continue;
            var entityPos = entity.blockPosition();
            int ex = entityPos.getX(), ey = entityPos.getY(), ez = entityPos.getZ();
            for (var dir : net.minecraft.core.Direction.values()) {
                if (dir == net.minecraft.core.Direction.DOWN || dir == net.minecraft.core.Direction.UP) continue;
                int ax = ex + dir.getStepX(), ay = ey + dir.getStepY(), az = ez + dir.getStepZ();
                if (BlockUtility.isAir(mc.level, ax, ay, az)) {
                    int by = BlockUtility.belowY(ay);
                    if (BlockUtility.isSolid(mc.level, ax, by, az)) {
                        double distSq = p.distanceToSqr(ax + 0.5, ay + 0.5, az + 0.5);
=======
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == p || !entity.isAlive() || !(entity instanceof LivingEntity)) continue;
            if (p.distanceTo(entity) > r) continue;
            BlockPos entityPos = entity.blockPosition();
            for (Direction dir : Direction.values()) {
                if (dir == Direction.DOWN || dir == Direction.UP) continue;
                BlockPos adjacentPos = entityPos.relative(dir);
                if (mc.level.getBlockState(adjacentPos).isAir()) {
                    BlockPos below = adjacentPos.below();
                    if (!mc.level.getBlockState(below).isAir() && mc.level.getBlockState(below).isCollisionShapeFullBlock(mc.level, below)) {
                        double distSq = p.distanceToSqr(adjacentPos.getX() + 0.5, adjacentPos.getY() + 0.5, adjacentPos.getZ() + 0.5);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            targetPacked = BlockUtility.packPos(ax, ay, az);
                            hasTarget = true;
                        }
                    }
                }
            }
        }
<<<<<<< HEAD
        if (!hasTarget) {
=======
        if (targetPos == null) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            int rangeInt = (int) Math.ceil(r);
            int px = playerPos.getX(), py = playerPos.getY(), pz = playerPos.getZ();
            for (int x = -rangeInt; x <= rangeInt; x++) {
                for (int y = -rangeInt; y <= rangeInt; y++) {
                    for (int z = -rangeInt; z <= rangeInt; z++) {
                        int wx = px + x, wy = py + y, wz = pz + z;
                        double distSq = p.distanceToSqr(wx + 0.5, wy + 0.5, wz + 0.5);
                        if (distSq < closestDistSq) {
                            if (BlockUtility.isAir(mc.level, wx, wy, wz)) {
                                int by = BlockUtility.belowY(wy);
                                if (BlockUtility.isSolid(mc.level, wx, by, wz)) {
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
<<<<<<< HEAD
        if (hasTarget) {
            int tx = BlockUtility.unpackX(targetPacked), ty = BlockUtility.unpackY(targetPacked), tz = BlockUtility.unpackZ(targetPacked);
            int prevSlot = InventoryUtility.getSelectedSlot(p);
=======
        if (targetPos != null) {
            int prevSlot = p.getInventory().getSelectedSlot();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (autoSwap.getValue() && chestSlot != prevSlot) {
                InventoryUtility.selectSlot(p, chestSlot);
            }
<<<<<<< HEAD
            var below = BlockUtility.pos(tx, BlockUtility.belowY(ty), tz);
            BlockUtility.useItemOn(mc, new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(below).add(0, 0.5, 0),
                net.minecraft.core.Direction.UP, below, false));
            ravex.utility.player.SwingUtility.swingMainHand(p);
=======
            BlockPos below = targetPos.below();
            Vec3 hitVec = Vec3.atCenterOf(below).add(0, 0.5, 0); 
            BlockHitResult blockHit = new BlockHitResult(hitVec, Direction.UP, below, false);
            mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, blockHit);
            p.swing(InteractionHand.MAIN_HAND);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (autoSwap.getValue() && silent.getValue() && chestSlot != prevSlot) {
                InventoryUtility.selectSlot(p, prevSlot);
            }
<<<<<<< HEAD
            placedChests.add(new PlacedChest(targetPacked, now));
=======
            placedChests.add(new PlacedChest(targetPos, now));
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            delayTimer = delay.getValue().intValue();
        }
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(ChestAura.class);
    }
    public static ChestAura itz() {
        return ModuleManager.get(ChestAura.class);
    }
}
