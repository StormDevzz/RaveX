package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.item.HoneycombItem;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "WaxAura", category = "Misc")
public class WaxAura {
    @Parameter(name = "Range", min = 2.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "Delay", min = 0.0, max = 20.0, step = 1.0)
    public double delay = 2.0;
    @Parameter(name = "AutoSwap")
    public boolean autoSwap = true;
    @Parameter(name = "Silent")
    public boolean silent = true;
    private int delayTimer = 0;
    public void onEnable() {
        delayTimer = 0;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getLevel() == null) return;
        if (delayTimer > 0) {
            delayTimer--;
            return;
        }
        int honeycombSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(p, i);
            if (InventoryUtility.isItem(stack, "honeycomb")) {
                honeycombSlot = i;
                break;
            }
        }
        if (honeycombSlot == -1) return;
        double r = range;
        var playerPos = p.blockPosition();
        net.minecraft.core.BlockPos targetPos = null;
        double closestDistSq = r * r;
        int rangeInt = (int) Math.ceil(r);
        for (int x = -rangeInt; x <= rangeInt; x++) {
            for (int y = -rangeInt; y <= rangeInt; y++) {
                for (int z = -rangeInt; z <= rangeInt; z++) {
                    var pos = playerPos.offset(x, y, z);
                    double distSq = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (distSq < closestDistSq) {
                        var state = mc.getLevel().getBlockState(pos);
                        if (HoneycombItem.getWaxed(state).isPresent()) {
                            closestDistSq = distSq;
                            targetPos = pos;
                        }
                    }
                }
            }
        }
        if (targetPos != null) {
            int prevSlot = InventoryUtility.getSelectedSlot(p);
            if (autoSwap && honeycombSlot != prevSlot) {
                InventoryUtility.selectSlot(p, honeycombSlot);
            }
            var hitVec = PhysicUtility.centerOf(targetPos);
            var blockHit = new net.minecraft.world.phys.BlockHitResult(hitVec, net.minecraft.core.Direction.UP, targetPos, false);
            var gm = mc.getGameMode();
            if (gm != null) gm.useItemOn(p, net.minecraft.world.InteractionHand.MAIN_HAND, blockHit);
            SwingUtility.swing(p, net.minecraft.world.InteractionHand.MAIN_HAND);
            if (autoSwap && silent && honeycombSlot != prevSlot) {
                InventoryUtility.selectSlot(p, prevSlot);
            }
            delayTimer = (int) delay;
        }
    }
}
