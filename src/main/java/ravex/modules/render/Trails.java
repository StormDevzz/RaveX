package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
<<<<<<< HEAD
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.render.Render3DUtils;
import java.util.*;

public class Trails extends Module {
<<<<<<< HEAD
=======
    public static final Trails INSTANCE = new Trails();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ColorParameter color = new ColorParameter("Color", 0xFF33AAFF);
    public final NumberParameter width = new NumberParameter("Width", 2.0, 1.0, 6.0, 0.5);
    public final NumberParameter time = new NumberParameter("Time", 3.0, 0.5, 10.0, 0.5);
    public final BooleanParameter arrows = new BooleanParameter("Arrows", true);
    public final BooleanParameter pearls = new BooleanParameter("Pearls", true);
    public final BooleanParameter tridents = new BooleanParameter("Tridents", true);
    public final BooleanParameter fireworks = new BooleanParameter("Fireworks", true);
    public final BooleanParameter potions = new BooleanParameter("Potions", true);
    public final BooleanParameter fireballs = new BooleanParameter("Fireballs", true);
    public final BooleanParameter windCharges = new BooleanParameter("WindCharges", true);
    public final BooleanParameter other = new BooleanParameter("OtherProjectiles", false);
    public final BooleanParameter self = new BooleanParameter("Self", true);
    public final BooleanParameter playerEnabled = new BooleanParameter("Players", false);
    public final ColorParameter playerColor = new ColorParameter("PlayerColor", 0xFFFF4444);
    public final NumberParameter playerWidth = new NumberParameter("PlayerWidth", 2.0, 1.0, 6.0, 0.5);
    public final NumberParameter playerTime = new NumberParameter("PlayerTime", 3.0, 0.5, 10.0, 0.5);
    public final BooleanParameter glow = new BooleanParameter("Glow", true);
    public final NumberParameter glowLayers = new NumberParameter("GlowLayers", 4, 1, 8, 1);
    public final NumberParameter glowSpread = new NumberParameter("GlowSpread", 1.5, 0.5, 5.0, 0.5);
    public final BooleanParameter mobs = new BooleanParameter("Mobs", false);
    private static final Map<Integer, List<TrailPoint>> entityTrails = new HashMap<>();
    private static final Map<Integer, List<TrailPoint>> playerTrails = new HashMap<>();

    private record TrailPoint(Vec3 pos, long time) {
    }

    private Trails() {
        super("Trails");
        playerColor.setVisible(() -> playerEnabled.getValue());
        playerWidth.setVisible(() -> playerEnabled.getValue());
        playerTime.setVisible(() -> playerEnabled.getValue());
        glowLayers.setVisible(() -> glow.getValue());
        glowSpread.setVisible(() -> glow.getValue());
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;
        long now = System.currentTimeMillis();
        purgeOldPoints(entityTrails, (long) (time.getValue() * 1000.0), now);
        purgeOldPoints(playerTrails, (long) (playerTime.getValue() * 1000.0), now);
        if (self.getValue()) {
            addPoint(entityTrails, mc.player.getId(), mc.player.position(), now);
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player)
                continue;
            if (entity instanceof Player && playerEnabled.getValue()) {
                addPoint(playerTrails, entity.getId(), entity.position(), now);
            } else if (shouldTrack(entity)) {
                addPoint(entityTrails, entity.getId(), entity.position(), now);
            }
        }
    }

    private boolean shouldTrack(Entity entity) {
        if (arrows.getValue() && entity instanceof Arrow)
            return true;
        if (pearls.getValue() && entity instanceof ThrownEnderpearl)
            return true;
        if (tridents.getValue() && entity instanceof ThrownTrident)
            return true;
        if (fireworks.getValue() && entity instanceof FireworkRocketEntity)
            return true;
        if (potions.getValue() && entity instanceof AbstractThrownPotion)
            return true;
        if (fireballs.getValue() && entity instanceof AbstractHurtingProjectile)
            return true;
        if (windCharges.getValue() && entity instanceof AbstractWindCharge)
            return true;
        if (other.getValue() && entity instanceof Projectile)
            return true;
        if (mobs.getValue() && !(entity instanceof Projectile))
            return true;
        return false;
    }

    private static void addPoint(Map<Integer, List<TrailPoint>> map, int id, Vec3 pos, long now) {
        List<TrailPoint> trail = map.computeIfAbsent(id, k -> new ArrayList<>());
        TrailPoint last = trail.isEmpty() ? null : trail.get(trail.size() - 1);
        if (last != null && last.pos.distanceToSqr(pos) < 0.01)
            return;
        trail.add(new TrailPoint(pos, now));
    }

    private static void purgeOldPoints(Map<Integer, List<TrailPoint>> map, long maxAge, long now) {
        Iterator<Map.Entry<Integer, List<TrailPoint>>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            List<TrailPoint> points = it.next().getValue();
            points.removeIf(p -> now - p.time > maxAge);
            if (points.isEmpty())
                it.remove();
        }
    }

    public static void renderTrails(Matrix4f modelViewMatrix, Vec3 camPos) {
        try {
            long now = System.currentTimeMillis();
<<<<<<< HEAD
            boolean glowEnabled = ModuleManager.get(Trails.class).glow.getValue();
            int glowLayersVal = ModuleManager.get(Trails.class).glowLayers.getValue().intValue();
            float glowSpreadVal = ModuleManager.get(Trails.class).glowSpread.getValue().floatValue();
            renderFadingTrail(entityTrails, modelViewMatrix, camPos, now,
                    ModuleManager.get(Trails.class).color.getValue(), ModuleManager.get(Trails.class).width.getValue().floatValue(),
                    (long) (ModuleManager.get(Trails.class).time.getValue() * 1000.0),
                    glowEnabled, glowLayersVal, glowSpreadVal);
            renderFadingTrail(playerTrails, modelViewMatrix, camPos, now,
                    ModuleManager.get(Trails.class).playerColor.getValue(), ModuleManager.get(Trails.class).playerWidth.getValue().floatValue(),
                    (long) (ModuleManager.get(Trails.class).playerTime.getValue() * 1000.0),
=======
            boolean glowEnabled = INSTANCE.glow.getValue();
            int glowLayersVal = INSTANCE.glowLayers.getValue().intValue();
            float glowSpreadVal = INSTANCE.glowSpread.getValue().floatValue();
            renderFadingTrail(entityTrails, modelViewMatrix, camPos, now,
                    INSTANCE.color.getValue(), INSTANCE.width.getValue().floatValue(),
                    (long) (INSTANCE.time.getValue() * 1000.0),
                    glowEnabled, glowLayersVal, glowSpreadVal);
            renderFadingTrail(playerTrails, modelViewMatrix, camPos, now,
                    INSTANCE.playerColor.getValue(), INSTANCE.playerWidth.getValue().floatValue(),
                    (long) (INSTANCE.playerTime.getValue() * 1000.0),
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                    glowEnabled, glowLayersVal, glowSpreadVal);
        } catch (Throwable t) {
            System.err.println("[RaveX] Trails render error: " + t.getMessage());
        }
    }

    private static void renderFadingTrail(Map<Integer, List<TrailPoint>> map,
            Matrix4f matrix, Vec3 camPos, long now,
            int colorARGB, float lineWidth, long maxAge,
            boolean glowEnabled, int glowLayersVal, float glowSpreadVal) {
        float cr = ((colorARGB >> 16) & 0xFF) / 255.0f;
        float cg = ((colorARGB >> 8) & 0xFF) / 255.0f;
        float cb = (colorARGB & 0xFF) / 255.0f;
        List<Float> segList = new ArrayList<>();
        int segCount = 0;
        for (List<TrailPoint> trail : map.values()) {
            if (trail.size() < 2)
                continue;
            for (int i = 1; i < trail.size(); i++) {
                TrailPoint p0 = trail.get(i - 1);
                TrailPoint p1 = trail.get(i);
                float age = (float) (now - p1.time) / maxAge;
                float alpha = Math.max(0.0f, 1.0f - age);
                if (alpha <= 0.001f)
                    continue;
                segList.add((float) (p0.pos.x - camPos.x));
                segList.add((float) (p0.pos.y - camPos.y));
                segList.add((float) (p0.pos.z - camPos.z));
                segList.add((float) (p1.pos.x - camPos.x));
                segList.add((float) (p1.pos.y - camPos.y));
                segList.add((float) (p1.pos.z - camPos.z));
                segList.add(alpha);
                segList.add(lineWidth);
                segCount++;
            }
        }
        if (segCount == 0)
            return;
        if (glowEnabled) {
            int layers = Math.max(1, glowLayersVal);
            float spread = Math.max(0.5f, glowSpreadVal);
            for (int i = 0; i < segCount; i++) {
                int off = i * 8;
                float x1 = segList.get(off);
                float y1 = segList.get(off + 1);
                float z1 = segList.get(off + 2);
                float x2 = segList.get(off + 3);
                float y2 = segList.get(off + 4);
                float z2 = segList.get(off + 5);
                float alpha = segList.get(off + 6);
                float w = segList.get(off + 7);
                if (alpha <= 0.001f)
                    continue;
                float glowAlpha = Math.min(alpha * 2.0f, 1.0f);
                for (int l = layers - 1; l >= 0; l--) {
                    float t = (float) (l + 1) / (float) layers;
                    float bloomWidth = w * (1.0f + spread * (1.0f - t) * 3.0f);
                    renderGlowSeg(matrix, x1, y1, z1, x2, y2, z2, cr, cg, cb, glowAlpha, bloomWidth);
                }
                float coreAlpha = Math.min(alpha * 2.5f, 1.0f);
                renderCoreSeg(matrix, x1, y1, z1, x2, y2, z2, cr, cg, cb, coreAlpha, w);
            }
        } else {
            for (int i = 0; i < segCount; i++) {
                int off = i * 8;
                float a = segList.get(off + 6);
                if (a <= 0.001f)
                    continue;
                renderGlowSeg(matrix,
                        segList.get(off), segList.get(off + 1), segList.get(off + 2),
                        segList.get(off + 3), segList.get(off + 4), segList.get(off + 5),
                        cr, cg, cb, a, segList.get(off + 7));
            }
        }
    }

    private static void renderGlowSeg(Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2,
            float cr, float cg, float cb, float alpha, float width) {
        Render3DUtils.batchLineAdditive(matrix, List.of(new Vector3f(x1, y1, z1), new Vector3f(x2, y2, z2)), cr, cg, cb,
                alpha, width);
    }

    private static void renderCoreSeg(Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2,
            float cr, float cg, float cb, float alpha, float width) {
        Render3DUtils.batchLineStrip(matrix, List.of(new Vector3f(x1, y1, z1), new Vector3f(x2, y2, z2)), cr, cg, cb,
                alpha, width);
    }

    @Override
    protected void onDisable() {
        entityTrails.clear();
        playerTrails.clear();
    }
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(Trails.class);
    }

    public static Trails itz() {
        return ModuleManager.get(Trails.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
