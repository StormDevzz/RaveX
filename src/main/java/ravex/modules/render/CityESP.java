package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.EntityUtility;

import ravex.modules.combat.KillAura;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "CityESP", category = "Render")
public class CityESP extends ravex.modules.Module {
public final NumberParameter range = new NumberParameter("Range", 6.0, 3.0, 10.0, 0.5);
    public final NumberParameter renderRange = new NumberParameter("RenderRange", 64.0, 8.0, 128.0, 8.0);
    public final BooleanParameter filled = new BooleanParameter("Filled", true);
    public final BooleanParameter wireframe = new BooleanParameter("Wireframe", true);
    public final ColorParameter fillColor = new ColorParameter("FillColor", 0x33FF0000);
    public final ColorParameter lineColor = new ColorParameter("LineColor", 0xFFFF0000);

    private net.minecraft.core.BlockPos cityBlock;

    private CityESP() {
        
    }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        net.minecraft.world.entity.player.Player target = null;
        if (ravex.manager.ModuleManager.delegate(KillAura.class).getEnabled()) {
            var kaTarget = ravex.manager.ModuleManager.delegate(KillAura.class).getCurrentTarget();
            if (kaTarget instanceof net.minecraft.world.entity.player.Player p && p.isAlive() && mc.player.distanceTo(p) <= range.getValue()) {
                target = p;
            }
        }
        if (target == null) {
            double best = range.getValue() * range.getValue();
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
            net.minecraft.world.level.block.state.BlockBehaviour block = Minecraft.getInstance().level.getBlockState(pos).getBlock();
            if (block != net.minecraft.world.level.block.Blocks.OBSIDIAN && block != net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK
                && block != net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN && block != net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR
                && block != net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS) continue;

            double distSq = Minecraft.getInstance().player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
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

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("CityESP").getEnabled();
    }

    public static CityESP itz() {
        return ravex.manager.ModuleManager.delegate(CityESP.class);
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