package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.EntityUtility;

import ravex.modules.combat.KillAura;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "CityESP", category = "Render")
public class CityESP {
    @Parameter(name = "Range", min = 3.0, max = 10.0, step = 0.5)
    public double range = 6.0;
    @Parameter(name = "RenderRange", min = 8.0, max = 128.0, step = 8.0)
    public double renderRange = 64.0;
    @Parameter(name = "Filled")
    public boolean filled = true;
    @Parameter(name = "Wireframe")
    public boolean wireframe = true;
    @Parameter(name = "FillColor", color = true)
    public int fillColor = 0x33FF0000;
    @Parameter(name = "LineColor", color = true)
    public int lineColor = 0xFFFF0000;

    private net.minecraft.core.BlockPos cityBlock;

    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;

        net.minecraft.world.entity.player.Player target = null;
        if (Modules.enabled(KillAura.class)) {
            var kaTarget = Modules.get(KillAura.class).getCurrentTarget();
            if (kaTarget instanceof net.minecraft.world.entity.player.Player p && p.isAlive() && mc.player.distanceTo(p) <= range) {
                target = p;
            }
        }
        if (target == null) {
            double best = range * range;
            for (var e : mc.level.entitiesForRendering()) {
                if (e instanceof net.minecraft.world.entity.player.Player p && p != mc.player && p.isAlive()) {
                    double dist = mc.player.distanceToSqr(p);
                    if (dist < best) {
                        best = dist;
                        target = p;
                    }
                }
            }
        }

        if (target == null) {
            cityBlock = null;
            return;
        }

        cityBlock = getCityBlock(target);
    }

    private net.minecraft.core.BlockPos getCityBlock(net.minecraft.world.entity.player.Player player) {
        double bestDistSq = 6 * 6;
        net.minecraft.core.BlockPos bestPos = null;

        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            net.minecraft.core.BlockPos pos = player.blockPosition().relative(dir);
            net.minecraft.world.level.block.state.BlockBehaviour block = MinecraftWrapper.getInstance().level.getBlockState(pos).getBlock();
            if (block != net.minecraft.world.level.block.Blocks.OBSIDIAN && block != net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK
                && block != net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN && block != net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR
                && block != net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS) continue;

            double distSq = MinecraftWrapper.getInstance().player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestPos = pos;
            }
        }

        return bestPos;
    }

    public net.minecraft.core.BlockPos getCityBlock() {
        return cityBlock;
    }






}