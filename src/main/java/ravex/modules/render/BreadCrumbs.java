package ravex.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.render.Render3DUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BreadCrumbs extends Module {
    public static final BreadCrumbs INSTANCE = new BreadCrumbs();
    public static final Map<Integer, List<Vec3>> trails = new HashMap<>();

    public final ColorParameter color = new ColorParameter("Color", 0xFF33AAFF);
    public final NumberParameter width = new NumberParameter("Width", 2.0, 1.0, 6.0, 0.5);
    public final NumberParameter maxPoints = new NumberParameter("Max Points", 200.0, 10.0, 1000.0, 10.0);
    public final BooleanParameter self = new BooleanParameter("Self", true);
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter mobs = new BooleanParameter("Mobs", false);

    private BreadCrumbs() {
        super("BreadCrumbs", Category.RENDER);
        addParameter(color);
        addParameter(width);
        addParameter(maxPoints);
        addParameter(self);
        addParameter(players);
        addParameter(mobs);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        int max = maxPoints.getValue().intValue();

        if (self.getValue()) {
            addPoint(mc.player.getId(), mc.player.position(), max);
        }
        if (players.getValue() || mobs.getValue()) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity == mc.player) continue;
                if (entity instanceof Player && !players.getValue()) continue;
                if (!(entity instanceof Player) && !mobs.getValue()) continue;
                addPoint(entity.getId(), entity.position(), max);
            }
        }
    }

    private void addPoint(int id, Vec3 pos, int max) {
        List<Vec3> trail = trails.computeIfAbsent(id, k -> new ArrayList<>());
        Vec3 last = trail.isEmpty() ? null : trail.get(trail.size() - 1);
        if (last != null && last.distanceToSqr(pos) < 0.01) return;
        trail.add(pos);
        if (trail.size() > max) {
            trail.remove(0);
        }
    }

    public static void renderTrails(Matrix4f modelViewMatrix, Vec3 camPos) {
        int color = INSTANCE.color.getValue();
        float cr = ((color >> 16) & 0xFF) / 255.0f;
        float cg = ((color >> 8) & 0xFF) / 255.0f;
        float cb = (color & 0xFF) / 255.0f;
        float lineWidth = INSTANCE.width.getValue().floatValue();

        for (Map.Entry<Integer, List<Vec3>> entry : trails.entrySet()) {
            List<Vec3> trail = entry.getValue();
            if (trail.size() < 2) continue;
            List<Vector3f> points = new ArrayList<>();
            for (Vec3 p : trail) {
                points.add(new Vector3f(
                    (float)(p.x - camPos.x),
                    (float)(p.y - camPos.y),
                    (float)(p.z - camPos.z)
                ));
            }
            Render3DUtils.renderLineStrip(modelViewMatrix, points, cr, cg, cb, 0.8f, lineWidth);
        }
    }

    @Override
    protected void onDisable() {
        trails.clear();
    }
}
