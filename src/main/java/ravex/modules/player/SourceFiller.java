package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.SwingUtility;
import ravex.mcwrapper.MinecraftWrapper;
import org.jetbrains.annotations.Nullable;



@Module(name = "SourceFiller", category = "Player")
public class SourceFiller {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "Swap", modes = {"Silent", "Normal"})
    public String swap = "Normal";
    @Parameter(name = "AirPlace")
    public boolean airPlace = false;
    @Parameter(name = "Rotate")
    public boolean rotate = true;
    @Parameter(name = "Delay", min = 0.0, max = 1000.0, step = 10.0)
    public double delay = 200.0;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true, visible = "render")
    public int color = 0x5540A9F8;

    private long lastPlaceTime = 0;
    private net.minecraft.core.BlockPos currentTarget;

    public net.minecraft.core.BlockPos getRenderTarget() {
        return currentTarget;
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getLevel() == null) return;
        net.minecraft.core.BlockPos targetPos = findTargetWater(p, mc);
        currentTarget = targetPos;
        if (targetPos == null) return;
        if (System.currentTimeMillis() - lastPlaceTime < delay) return;
        int spongeSlot = InventoryUtility.findHotbarSlot(p, "sponge");
        int selectedSlot = InventoryUtility.getSelectedSlot(p);
        int slotToUse;
        boolean swapped;
        if (spongeSlot != -1) {
            slotToUse = spongeSlot;
            swapped = (spongeSlot != selectedSlot);
        } else {
            slotToUse = selectedSlot;
            swapped = false;
        }
        if (p.getInventory().getItem(slotToUse).isEmpty()) return;
        if (rotate) {
            float[] rots = RotationUtility.anglesTo(p.getEyePosition(), PhysicUtility.centerOf(targetPos));
            p.setYBodyRot(rots[0]);
            p.setYHeadRot(rots[0]);
        }
        if (airPlace) {
            if (swapped) InventoryUtility.swapToSlot(p, slotToUse, "Silent".equals(swap) ? "Silent" : "Normal");
            BlockUtility.grimAirPlace(mc, targetPos, net.minecraft.world.InteractionHand.MAIN_HAND);
            ravex.utility.player.SwingUtility.swingMainHand(p);
            if (swapped && "Silent".equals(swap)) InventoryUtility.swapBackSlot(p, selectedSlot, "Silent");
            if (swapped && "Normal".equals(swap)) InventoryUtility.swapBackSlot(p, selectedSlot, "Normal");
        } else {
            var cfg = new BlockUtility.PlaceConfig();
            cfg.swing = true;
            cfg.restoreSlot = swapped;
            cfg.swapMode = "Silent".equals(swap) ? "Silent" : "Normal";
            BlockUtility.placeBlock(mc, targetPos, slotToUse, cfg);
        }
        lastPlaceTime = System.currentTimeMillis();
    }
    @Nullable
    private net.minecraft.core.BlockPos findTargetWater(net.minecraft.client.player.LocalPlayer p, MinecraftWrapper mc) {
        double r = range;
        net.minecraft.core.BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = (int) Math.floor(p.getX() - r); x <= Math.ceil(p.getX() + r); x++)
            for (int y = (int) Math.floor(p.getY() - r); y <= Math.ceil(p.getY() + r); y++)
                for (int z = (int) Math.floor(p.getZ() - r); z <= Math.ceil(p.getZ() + r); z++) {
                    net.minecraft.core.BlockPos bp = BlockUtility.pos(x, y, z);
                    double dist = p.getEyePosition().distanceToSqr(PhysicUtility.centerOf(bp));
                    if (dist > r * r) continue;
                    if (mc.getLevel().getFluidState(bp).is(net.minecraft.tags.FluidTags.WATER) && dist < bestDist) {
                        bestDist = dist;
                        best = bp;
                    }
                }
        return best;
    }



}
