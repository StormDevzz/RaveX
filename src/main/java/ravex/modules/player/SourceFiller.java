package ravex.modules.player;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.ModeParameter;
<<<<<<< HEAD
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.player.rotation.RotationUtility;
=======
import ravex.utility.player.InventoryUtility;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
<<<<<<< HEAD
=======
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
public class SourceFiller extends Module {
<<<<<<< HEAD
=======
    public static final SourceFiller INSTANCE = new SourceFiller();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter range = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.1);
    public final ModeParameter mode = new ModeParameter("Mode", "Smart", List.of("Normal", "Smart"));
    public final BooleanParameter silent = new BooleanParameter("SilentSwap", true);
    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
<<<<<<< HEAD
    public final NumberParameter delay = new NumberParameter("Delay", 200.0, 0.0, 1000.0, 10.0);
=======
    public final NumberParameter delay = new NumberParameter("Delay(ms)", 200.0, 0.0, 1000.0, 10.0);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private long lastPlaceTime = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null) return;
        if (System.currentTimeMillis() - lastPlaceTime < delay.getValue()) return;
<<<<<<< HEAD
        int spongeSlot = InventoryUtility.findHotbarSlot(p, "sponge");
        if (spongeSlot == -1) return;
        BlockPos targetPos = findTargetWater(p, mc);
        if (targetPos == null) return;
        int prevSlot = InventoryUtility.getSelectedSlot(p);
        InventoryUtility.selectSlot(p, spongeSlot);
=======
        int spongeSlot = InventoryUtility.findHotbarSlot(p, Items.SPONGE);
        if (spongeSlot == -1) return;
        BlockPos targetPos = findTargetWater(p, mc);
        if (targetPos == null) return;
        int prevSlot = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(spongeSlot);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        Vec3 hitVec = Vec3.atCenterOf(targetPos);
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, targetPos, false);
        if (rotate.getValue()) {
            float[] rots = RotationUtility.anglesTo(p.getEyePosition(), Vec3.atCenterOf(targetPos));
            p.setYRot(rots[0]);
            p.setXRot(rots[1]);
        }
<<<<<<< HEAD
        BlockUtility.useItemOn(mc, hit);
        SwingUtility.swingMainHand(p);
        if (silent.getValue() && spongeSlot != prevSlot)
            InventoryUtility.selectSlot(p, prevSlot);
        lastPlaceTime = System.currentTimeMillis();
    }
    private BlockPos findTargetWater(net.minecraft.client.player.LocalPlayer p, Minecraft mc) {
=======
        mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hit);
        p.swing(InteractionHand.MAIN_HAND);
        if (silent.getValue() && spongeSlot != prevSlot)
            p.getInventory().setSelectedSlot(prevSlot);
        lastPlaceTime = System.currentTimeMillis();
    }
    private BlockPos findTargetWater(LocalPlayer p, Minecraft mc) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        double r = range.getValue();
        List<BlockPos> candidates = new ArrayList<>();
        for (int x = (int) Math.floor(p.getX() - r); x <= Math.ceil(p.getX() + r); x++)
            for (int y = (int) Math.floor(p.getY() - r); y <= Math.ceil(p.getY() + r); y++)
                for (int z = (int) Math.floor(p.getZ() - r); z <= Math.ceil(p.getZ() + r); z++) {
<<<<<<< HEAD
                    BlockPos bp = BlockUtility.pos(x, y, z);
                    if (p.getEyePosition().distanceToSqr(Vec3.atCenterOf(bp)) > r * r) continue;
                    if (mc.level.getFluidState(bp).is(net.minecraft.tags.FluidTags.WATER)) candidates.add(bp);
=======
                    BlockPos bp = new BlockPos(x, y, z);
                    if (p.getEyePosition().distanceToSqr(Vec3.atCenterOf(bp)) > r * r) continue;
                    if (mc.level.getFluidState(bp).is(FluidTags.WATER)) candidates.add(bp);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                }
        if (candidates.isEmpty()) return null;
        return "Smart".equals(mode.getValue())
            ? candidates.stream().max(Comparator.comparingInt(bp -> countAdjacentWater(bp, mc))).orElse(null)
            : candidates.stream().min(Comparator.comparingDouble(bp -> p.getEyePosition().distanceToSqr(Vec3.atCenterOf(bp)))).orElse(null);
    }
    private int countAdjacentWater(BlockPos pos, Minecraft mc) {
        int count = 0;
        for (Direction dir : Direction.values())
<<<<<<< HEAD
            if (mc.level.getFluidState(pos.relative(dir)).is(net.minecraft.tags.FluidTags.WATER)) count++;
        return count;
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(SourceFiller.class);
=======
            if (mc.level.getFluidState(pos.relative(dir)).is(FluidTags.WATER)) count++;
        return count;
    }
    private float[] rotationsTo(BlockPos pos, LocalPlayer p) {
        Vec3 target = Vec3.atCenterOf(pos);
        double dx = target.x - p.getX(), dy = (target.y + 0.5) - (p.getY() + p.getEyeHeight()), dz = target.z - p.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        return new float[]{(float) Math.toDegrees(Math.atan2(-dx, dz)), (float) -Math.toDegrees(Math.atan2(dy, dist))};
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }
    public static SourceFiller itz() {
        return ModuleManager.get(SourceFiller.class);
    }

}