package ravex.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.modules.combat.KillAura;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;

import java.util.ArrayList;
import java.util.List;

public class CityESP extends Module {
    public final NumberParameter range = new NumberParameter("Range", 6.0, 3.0, 10.0, 0.5);
    public final NumberParameter renderRange = new NumberParameter("RenderRange", 64.0, 8.0, 128.0, 8.0);
    public final BooleanParameter filled = new BooleanParameter("Filled", true);
    public final BooleanParameter wireframe = new BooleanParameter("Wireframe", true);
    public final ColorParameter fillColor = new ColorParameter("FillColor", 0x33FF0000);
    public final ColorParameter lineColor = new ColorParameter("LineColor", 0xFFFF0000);

    private BlockPos cityBlock;

    private CityESP() {
        super("CityESP");
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player target = null;
        if (ModuleManager.get(KillAura.class).getEnabled()) {
            var kaTarget = ModuleManager.get(KillAura.class).getCurrentTarget();
            if (kaTarget instanceof Player p && p.isAlive() && mc.player.distanceTo(p) <= range.getValue()) {
                target = p;
            }
        }
        if (target == null) {
            double best = range.getValue() * range.getValue();
            for (var e : mc.level.entitiesForRendering()) {
                if (e instanceof Player p && p != mc.player && p.isAlive()) {
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

    private BlockPos getCityBlock(Player player) {
        double bestDistSq = 6 * 6;
        BlockPos bestPos = null;

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos pos = player.blockPosition().relative(dir);
            BlockBehaviour block = Minecraft.getInstance().level.getBlockState(pos).getBlock();
            if (block != Blocks.OBSIDIAN && block != Blocks.NETHERITE_BLOCK
                && block != Blocks.CRYING_OBSIDIAN && block != Blocks.RESPAWN_ANCHOR
                && block != Blocks.ANCIENT_DEBRIS) continue;

            double distSq = Minecraft.getInstance().player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestPos = pos;
            }
        }

        return bestPos;
    }

    public BlockPos getCityBlock() {
        return cityBlock;
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(CityESP.class);
    }

    public static CityESP itz() {
        return ModuleManager.get(CityESP.class);
    }
}
