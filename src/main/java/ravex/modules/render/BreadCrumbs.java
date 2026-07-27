package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.misc.MobUtility;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import ravex.utility.render.Render3DUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "BreadCrumbs", category = "Render")
public class BreadCrumbs {
public static final Map<Integer, List<net.minecraft.world.phys.Vec3>> trails = new HashMap<>();
    @Parameter(name = "Color", color = true)
    public int color = 0xFF33AAFF;
    @Parameter(name = "Width", min = 1.0, max = 6.0, step = 0.5)
    public double width = 2.0;
    @Parameter(name = "MaxPoints", min = 10.0, max = 1000.0, step = 10.0)
    public double maxPoints = 200.0;
    @Parameter(name = "Self")
    public boolean self = true;
    @Parameter(name = "Players")
    public boolean players = true;
    @Parameter(name = "Mobs")
    public boolean mobs = false;
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.level == null || mc.player == null) return;
        int max = (int) maxPoints;
        if (self) {
            addPoint(mc.player.getId(), mc.player.position(), max);
        }
        if (players || mobs) {
            for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
                if (entity == mc.player) continue;
                if (MobUtility.isPlayer(MobUtility.asLivingEntity(entity)) && !players) continue;
                if (!MobUtility.isPlayer(MobUtility.asLivingEntity(entity)) && !mobs) continue;
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
        int color = Modules.get(BreadCrumbs.class).color;
        float cr = ((color >> 16) & 0xFF) / 255.0f;
        float cg = ((color >> 8) & 0xFF) / 255.0f;
        float cb = (color & 0xFF) / 255.0f;
        float lineWidth = (float) Modules.get(BreadCrumbs.class).width;
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
    public void onDisable() {
        trails.clear();
    }





}