package ravex.modules.world;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
public class ChestAura extends Module {
    public static final ChestAura INSTANCE = new ChestAura();
    public final NumberParameter range = new NumberParameter("Range", 4.5, 2.0, 6.0, 0.1);
    public final NumberParameter delay = new NumberParameter("Delay", 2.0, 0.0, 20.0, 1.0);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter highlightColor = new ColorParameter("Color", 0xFF00FF88);
    public final NumberParameter fadeSpeed = new NumberParameter("Fade Duration", 1.0, 0.1, 3.0, 0.1);
    public final BooleanParameter filled = new BooleanParameter("Filled", true);
    public final BooleanParameter autoSwap = new BooleanParameter("Auto Swap", true);
    public final BooleanParameter silent = new BooleanParameter("Silent", true);
    public static class PlacedChest {
        public final BlockPos pos;
        public final long placeTime;
        public PlacedChest(BlockPos pos, long placeTime) {
            this.pos = pos;
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
        LocalPlayer p = mc.player;
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
            ItemStack stack = p.getInventory().getItem(i);
            if (!stack.isEmpty() && (stack.is(Items.CHEST) || stack.is(Items.TRAPPED_CHEST))) {
                chestSlot = i;
                break;
            }
        }
        if (chestSlot == -1) return; 
        double r = range.getValue();
        BlockPos playerPos = p.blockPosition();
        BlockPos targetPos = null;
        double closestDistSq = r * r;
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
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            targetPos = adjacentPos;
                        }
                    }
                }
            }
        }
        if (targetPos == null) {
            int rangeInt = (int) Math.ceil(r);
            for (int x = -rangeInt; x <= rangeInt; x++) {
                for (int y = -rangeInt; y <= rangeInt; y++) {
                    for (int z = -rangeInt; z <= rangeInt; z++) {
                        BlockPos pos = playerPos.offset(x, y, z);
                        double distSq = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        if (distSq < closestDistSq) {
                            if (mc.level.getBlockState(pos).isAir()) {
                                BlockPos below = pos.below();
                                if (!mc.level.getBlockState(below).isAir() && mc.level.getBlockState(below).isCollisionShapeFullBlock(mc.level, below)) {
                                    closestDistSq = distSq;
                                    targetPos = pos;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (targetPos != null) {
            int prevSlot = p.getInventory().getSelectedSlot();
            if (autoSwap.getValue() && chestSlot != prevSlot) {
                p.getInventory().setSelectedSlot(chestSlot);
            }
            BlockPos below = targetPos.below();
            Vec3 hitVec = Vec3.atCenterOf(below).add(0, 0.5, 0); 
            BlockHitResult blockHit = new BlockHitResult(hitVec, Direction.UP, below, false);
            mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, blockHit);
            p.swing(InteractionHand.MAIN_HAND);
            if (autoSwap.getValue() && silent.getValue() && chestSlot != prevSlot) {
                p.getInventory().setSelectedSlot(prevSlot);
            }
            placedChests.add(new PlacedChest(targetPos, now));
            delayTimer = delay.getValue().intValue();
        }
    }
}
