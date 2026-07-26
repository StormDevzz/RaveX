package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.misc.MobUtility;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.render.Render3DUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@ModuleInfo(name = "BreadCrumbs", category = "Render")
public class BreadCrumbs extends ravex.modules.Module {
public static final Map<Integer, List<net.minecraft.world.phys.Vec3>> trails = new HashMap<>();
    public final ColorParameter color = new ColorParameter("Color", 0xFF33AAFF);
    public final NumberParameter width = new NumberParameter("Width", 2.0, 1.0, 6.0, 0.5);
    public final NumberParameter maxPoints = new NumberParameter("MaxPoints", 200.0, 10.0, 1000.0, 10.0);
    public final BooleanParameter self = new BooleanParameter("Self", true);
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter mobs = new BooleanParameter("Mobs", false);
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        int max = maxPoints.getValue().intValue();
        if (self.getValue()) {
            addPoint(mc.player.getId(), mc.player.position(), max);
        }
        if (players.getValue() || mobs.getValue()) {
            for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
                if (entity == mc.player) continue;
                if (MobUtility.isPlayer(MobUtility.asLivingEntity(entity)) && !players.getValue()) continue;
                if (!MobUtility.isPlayer(MobUtility.asLivingEntity(entity)) && !mobs.getValue()) continue;
                addPoint(entity.getId(), entity.position(), max);
            }
        }
    }
    private void addPoint(int id, net.minecraft.world.phys.Vec3 pos, int max) {
        List<net.minecraft.world.phys.Vec3> trail = trails.computeIfAbsent(id, k -> new ArrayList<>());
        net.minecraft.world.phys.Vec3 last = trail.isEmpty() ? null : trail.get(trail.size() - 1);
        if (last != null && last.distanceToSqr(pos) < 0.01) return;
        trail.add(pos);
        if (trail.size() > max) {
            trail.remove(0);
        }
    }
    public static void renderTrails(Matrix4f modelViewMatrix, net.minecraft.world.phys.Vec3 camPos) {
        int color = ravex.manager.ModuleManager.delegate(BreadCrumbs.class).color.getValue();
        float cr = ((color >> 16) & 0xFF) / 255.0f;
        float cg = ((color >> 8) & 0xFF) / 255.0f;
        float cb = (color & 0xFF) / 255.0f;
        float lineWidth = ravex.manager.ModuleManager.delegate(BreadCrumbs.class).width.getValue().floatValue();
        for (Map.Entry<Integer, List<net.minecraft.world.phys.Vec3>> entry : trails.entrySet()) {
            List<net.minecraft.world.phys.Vec3> trail = entry.getValue();
            if (trail.size() < 2) continue;
            List<Vector3f> points = new ArrayList<>();
            for (net.minecraft.world.phys.Vec3 p : trail) {
                points.add(new Vector3f(
                    (float)(p.x - camPos.x),
                    (float)(p.y - camPos.y),
                    (float)(p.z - camPos.z)
                ));
            }
            Render3DUtility.batchLineStrip(modelViewMatrix, points, cr, cg, cb, 0.8f, lineWidth);
        }
    }
    protected void onDisable() {
        trails.clear();
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("BreadCrumbs").getEnabled();
    }

    public static BreadCrumbs itz() {
        return ravex.manager.ModuleManager.delegate(BreadCrumbs.class);
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