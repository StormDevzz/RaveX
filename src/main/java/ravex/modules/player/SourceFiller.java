package ravex.modules.player;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.player.rotation.RotationUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
public class SourceFiller extends Module {
    public final NumberParameter range = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.1);
    public final ModeParameter mode = new ModeParameter("Mode", "Smart", List.of("Normal", "Smart"));
    public final BooleanParameter silent = new BooleanParameter("SilentSwap", true);
    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
    public final NumberParameter delay = new NumberParameter("Delay", 200.0, 0.0, 1000.0, 10.0);
    private long lastPlaceTime = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null) return;
        if (System.currentTimeMillis() - lastPlaceTime < delay.getValue()) return;
        int spongeSlot = InventoryUtility.findHotbarSlot(p, "sponge");
        if (spongeSlot == -1) return;
        BlockPos targetPos = findTargetWater(p, mc);
        if (targetPos == null) return;
        int prevSlot = InventoryUtility.getSelectedSlot(p);
        InventoryUtility.selectSlot(p, spongeSlot);
        Vec3 hitVec = Vec3.atCenterOf(targetPos);
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, targetPos, false);
        if (rotate.getValue()) {
            float[] rots = RotationUtility.anglesTo(p.getEyePosition(), Vec3.atCenterOf(targetPos));
            p.setYRot(rots[0]);
            p.setXRot(rots[1]);
        }
        BlockUtility.useItemOn(mc, hit);
        SwingUtility.swingMainHand(p);
        if (silent.getValue() && spongeSlot != prevSlot)
            InventoryUtility.selectSlot(p, prevSlot);
        lastPlaceTime = System.currentTimeMillis();
    }
    private BlockPos findTargetWater(net.minecraft.client.player.LocalPlayer p, Minecraft mc) {
        double r = range.getValue();
        List<BlockPos> candidates = new ArrayList<>();
        for (int x = (int) Math.floor(p.getX() - r); x <= Math.ceil(p.getX() + r); x++)
            for (int y = (int) Math.floor(p.getY() - r); y <= Math.ceil(p.getY() + r); y++)
                for (int z = (int) Math.floor(p.getZ() - r); z <= Math.ceil(p.getZ() + r); z++) {
                    BlockPos bp = BlockUtility.pos(x, y, z);
                    if (p.getEyePosition().distanceToSqr(Vec3.atCenterOf(bp)) > r * r) continue;
                    if (mc.level.getFluidState(bp).is(net.minecraft.tags.FluidTags.WATER)) candidates.add(bp);
                }
        if (candidates.isEmpty()) return null;
        return "Smart".equals(mode.getValue())
            ? candidates.stream().max(Comparator.comparingInt(bp -> countAdjacentWater(bp, mc))).orElse(null)
            : candidates.stream().min(Comparator.comparingDouble(bp -> p.getEyePosition().distanceToSqr(Vec3.atCenterOf(bp)))).orElse(null);
    }
    private int countAdjacentWater(BlockPos pos, Minecraft mc) {
        int count = 0;
        for (Direction dir : Direction.values())
            if (mc.level.getFluidState(pos.relative(dir)).is(net.minecraft.tags.FluidTags.WATER)) count++;
        return count;
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(SourceFiller.class);
    }
    public static SourceFiller itz() {
        return ModuleManager.get(SourceFiller.class);
    }

}