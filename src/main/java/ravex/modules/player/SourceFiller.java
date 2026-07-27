package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.SwingUtility;
import ravex.mcwrapper.MinecraftWrapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;



@ModuleInfo(name = "SourceFiller", category = "net.minecraft.world.entity.player.Player")
public class SourceFiller implements ModuleAccess {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "Mode", modes = {"Normal", "Smart"})
    public String mode = "Smart";
    @Parameter(name = "SilentSwap")
    public boolean silent = true;
    @Parameter(name = "Rotate")
    public boolean rotate = true;
    @Parameter(name = "Delay", min = 0.0, max = 1000.0, step = 10.0)
    public double delay = 200.0;
    private long lastPlaceTime = 0;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getLevel() == null) return;
        if (System.currentTimeMillis() - lastPlaceTime < delay) return;
        int spongeSlot = InventoryUtility.findHotbarSlot(p, "sponge");
        if (spongeSlot == -1) return;
        net.minecraft.core.BlockPos targetPos = findTargetWater(p, mc);
        if (targetPos == null) return;
        int prevSlot = InventoryUtility.getSelectedSlot(p);
        InventoryUtility.selectSlot(p, spongeSlot);
        net.minecraft.world.phys.Vec3 hitVec = net.minecraft.world.phys.Vec3.atCenterOf(targetPos);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(hitVec, net.minecraft.core.Direction.UP, targetPos, false);
        if (rotate) {
            float[] rots = RotationUtility.anglesTo(p.getEyePosition(), net.minecraft.world.phys.Vec3.atCenterOf(targetPos));
            p.setYRot(rots[0]);
            p.setXRot(rots[1]);
        }
        BlockUtility.useItemOn(mc, hit);
        SwingUtility.swingMainHand(p);
        if (silent && spongeSlot != prevSlot)
            InventoryUtility.selectSlot(p, prevSlot);
        lastPlaceTime = System.currentTimeMillis();
    }
    private net.minecraft.core.BlockPos findTargetWater(net.minecraft.client.player.LocalPlayer p, MinecraftWrapper mc) {
        double r = range;
        List<net.minecraft.core.BlockPos> candidates = new ArrayList<>();
        for (int x = (int) Math.floor(p.getX() - r); x <= Math.ceil(p.getX() + r); x++)
            for (int y = (int) Math.floor(p.getY() - r); y <= Math.ceil(p.getY() + r); y++)
                for (int z = (int) Math.floor(p.getZ() - r); z <= Math.ceil(p.getZ() + r); z++) {
                    net.minecraft.core.BlockPos bp = BlockUtility.pos(x, y, z);
                    if (p.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(bp)) > r * r) continue;
                    if (mc.getLevel().getFluidState(bp).is(net.minecraft.tags.FluidTags.WATER)) candidates.add(bp);
                }
        if (candidates.isEmpty()) return null;
        return "Smart".equals(mode)
            ? candidates.stream().max(Comparator.comparingInt(bp -> countAdjacentWater(bp, mc))).orElse(null)
            : candidates.stream().min(Comparator.comparingDouble(bp -> p.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(bp)))).orElse(null);
    }
    private int countAdjacentWater(net.minecraft.core.BlockPos pos, MinecraftWrapper mc) {
        int count = 0;
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values())
            if (mc.getLevel().getFluidState(pos.relative(dir)).is(net.minecraft.tags.FluidTags.WATER)) count++;
        return count;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("SourceFiller").getEnabled();
    }
    public static SourceFiller itz() {
        return ravex.manager.ModuleManager.delegate(SourceFiller.class);
    }


}