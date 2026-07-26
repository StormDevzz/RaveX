package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.player.rotation.RotationUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import ravex.utility.misc.PhysicUtility;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
@ModuleInfo(name = "SourceFiller", category = "net.minecraft.world.entity.player.Player")
public class SourceFiller extends ravex.modules.Module {
public final NumberParameter range = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.1);
    public final ModeParameter mode = new ModeParameter("Mode", "Smart", List.of("Normal", "Smart"));
    public final BooleanParameter silent = new BooleanParameter("SilentSwap", true);
    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
    public final NumberParameter delay = new NumberParameter("Delay", 200.0, 0.0, 1000.0, 10.0);
    private long lastPlaceTime = 0;
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null) return;
        if (System.currentTimeMillis() - lastPlaceTime < delay.getValue()) return;
        int spongeSlot = InventoryUtility.findHotbarSlot(p, "sponge");
        if (spongeSlot == -1) return;
        net.minecraft.core.BlockPos targetPos = findTargetWater(p, mc);
        if (targetPos == null) return;
        int prevSlot = InventoryUtility.getSelectedSlot(p);
        InventoryUtility.selectSlot(p, spongeSlot);
        net.minecraft.world.phys.Vec3 hitVec = net.minecraft.world.phys.Vec3.atCenterOf(targetPos);
        BlockHitResult hit = new BlockHitResult(hitVec, net.minecraft.core.Direction.UP, targetPos, false);
        if (rotate.getValue()) {
            float[] rots = RotationUtility.anglesTo(p.getEyePosition(), net.minecraft.world.phys.Vec3.atCenterOf(targetPos));
            p.setYRot(rots[0]);
            p.setXRot(rots[1]);
        }
        BlockUtility.useItemOn(mc, hit);
        SwingUtility.swingMainHand(p);
        if (silent.getValue() && spongeSlot != prevSlot)
            InventoryUtility.selectSlot(p, prevSlot);
        lastPlaceTime = System.currentTimeMillis();
    }
    private net.minecraft.core.BlockPos findTargetWater(net.minecraft.client.player.LocalPlayer p, Minecraft mc) {
        double r = range.getValue();
        List<net.minecraft.core.BlockPos> candidates = new ArrayList<>();
        for (int x = (int) Math.floor(p.getX() - r); x <= Math.ceil(p.getX() + r); x++)
            for (int y = (int) Math.floor(p.getY() - r); y <= Math.ceil(p.getY() + r); y++)
                for (int z = (int) Math.floor(p.getZ() - r); z <= Math.ceil(p.getZ() + r); z++) {
                    net.minecraft.core.BlockPos bp = BlockUtility.pos(x, y, z);
                    if (p.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(bp)) > r * r) continue;
                    if (mc.level.getFluidState(bp).is(net.minecraft.tags.FluidTags.WATER)) candidates.add(bp);
                }
        if (candidates.isEmpty()) return null;
        return "Smart".equals(mode.getValue())
            ? candidates.stream().max(Comparator.comparingInt(bp -> countAdjacentWater(bp, mc))).orElse(null)
            : candidates.stream().min(Comparator.comparingDouble(bp -> p.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(bp)))).orElse(null);
    }
    private int countAdjacentWater(net.minecraft.core.BlockPos pos, Minecraft mc) {
        int count = 0;
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values())
            if (mc.level.getFluidState(pos.relative(dir)).is(net.minecraft.tags.FluidTags.WATER)) count++;
        return count;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("SourceFiller").getEnabled();
    }
    public static SourceFiller itz() {
        return ravex.manager.ModuleManager.delegate(SourceFiller.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}