package ravex.modules.misc;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.utility.player.InventoryUtility;
public class WaxAura extends Module {
    public static final WaxAura INSTANCE = new WaxAura();
    public final NumberParameter range = new NumberParameter("Range", 4.5, 2.0, 6.0, 0.1);
    public final NumberParameter delay = new NumberParameter("Delay", 2.0, 0.0, 20.0, 1.0);
    public final BooleanParameter autoSwap = new BooleanParameter("AutoSwap", true);
    public final BooleanParameter silent = new BooleanParameter("Silent", true);
    private int delayTimer = 0;

    @Override
    protected void onEnable() {
        delayTimer = 0;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
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
        double r = range.getValue();
        BlockPos playerPos = p.blockPosition();
        BlockPos targetPos = null;
        double closestDistSq = r * r;
        int rangeInt = (int) Math.ceil(r);
        for (int x = -rangeInt; x <= rangeInt; x++) {
            for (int y = -rangeInt; y <= rangeInt; y++) {
                for (int z = -rangeInt; z <= rangeInt; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    double distSq = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (distSq < closestDistSq) {
                        BlockState state = mc.level.getBlockState(pos);
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
            if (autoSwap.getValue() && honeycombSlot != prevSlot) {
                InventoryUtility.selectSlot(p, honeycombSlot);
            }
            Vec3 hitVec = Vec3.atCenterOf(targetPos);
            BlockHitResult blockHit = new BlockHitResult(hitVec, Direction.UP, targetPos, false);
            mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, blockHit);
            p.swing(InteractionHand.MAIN_HAND);
            if (autoSwap.getValue() && silent.getValue() && honeycombSlot != prevSlot) {
                InventoryUtility.selectSlot(p, prevSlot);
            }
            delayTimer = delay.getValue().intValue();
        }
    }
}
