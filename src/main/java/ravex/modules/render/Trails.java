package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import ravex.utility.misc.PhysicUtility;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import ravex.utility.render.Render3DUtility;
import java.util.*;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "Trails", category = "Render")
public class Trails {
    @Parameter(name = "Color", color = true)
    public int color = 0xFF33AAFF;
    @Parameter(name = "Width", min = 1.0, max = 6.0, step = 0.5)
    public double width = 2.0;
    @Parameter(name = "Time", min = 0.5, max = 10.0, step = 0.5)
    public double time = 3.0;
    @Parameter(name = "Arrows")
    public boolean arrows = true;
    @Parameter(name = "Pearls")
    public boolean pearls = true;
    @Parameter(name = "Tridents")
    public boolean tridents = true;
    @Parameter(name = "Fireworks")
    public boolean fireworks = true;
    @Parameter(name = "Potions")
    public boolean potions = true;
    @Parameter(name = "Fireballs")
    public boolean fireballs = true;
    @Parameter(name = "WindCharges")
    public boolean windCharges = true;
    @Parameter(name = "OtherProjectiles")
    public boolean other = false;
    @Parameter(name = "Self")
    public boolean self = true;
    @Parameter(name = "Players")
    public boolean playerEnabled = false;
    @Parameter(name = "PlayerColor", color = true, visible = "playerEnabled")
    public int playerColor = 0xFFFF4444;
    @Parameter(name = "PlayerWidth", min = 1.0, max = 6.0, step = 0.5, visible = "playerEnabled")
    public double playerWidth = 2.0;
    @Parameter(name = "PlayerTime", min = 0.5, max = 10.0, step = 0.5, visible = "playerEnabled")
    public double playerTime = 3.0;
    @Parameter(name = "Glow")
    public boolean glow = true;
    @Parameter(name = "GlowLayers", min = 1, max = 8, step = 1, visible = "glow")
    public double glowLayers = 4;
    @Parameter(name = "GlowSpread", min = 0.5, max = 5.0, step = 0.5, visible = "glow")
    public double glowSpread = 1.5;
    @Parameter(name = "Mobs")
    public boolean mobs = false;
    private static final Map<Integer, List<TrailPoint>> entityTrails = new HashMap<>();
    private static final Map<Integer, List<TrailPoint>> playerTrails = new HashMap<>();

    private record TrailPoint(net.minecraft.world.phys.Vec3 pos, long time) {
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null || mc.getPlayer() == null)
            return;
        long now = System.currentTimeMillis();
        purgeOldPoints(entityTrails, (long) (time * 1000.0), now);
        purgeOldPoints(playerTrails, (long) (playerTime * 1000.0), now);
        if (self) {
            addPoint(entityTrails, mc.getPlayer().getId(), mc.getPlayer().position(), now);
        }
        for (net.minecraft.world.entity.Entity entity : mc.getLevel().entitiesForRendering()) {
            if (entity == mc.getPlayer())
                continue;
            if (entity instanceof net.minecraft.world.entity.player.Player && playerEnabled) {
                addPoint(playerTrails, entity.getId(), entity.position(), now);
            } else if (shouldTrack(entity)) {
                addPoint(entityTrails, entity.getId(), entity.position(), now);
            }
        }
    }

    private boolean shouldTrack(net.minecraft.world.entity.Entity entity) {
        if (arrows && entity instanceof Arrow)
            return true;
        if (pearls && entity instanceof ThrownEnderpearl)
            return true;
        if (tridents && entity instanceof ThrownTrident)
            return true;
        if (fireworks && entity instanceof FireworkRocketEntity)
            return true;
        if (potions && entity instanceof AbstractThrownPotion)
            return true;
        if (fireballs && entity instanceof AbstractHurtingProjectile)
            return true;
        if (windCharges && entity instanceof AbstractWindCharge)
            return true;
        if (other && entity instanceof Projectile)
            return true;
        if (mobs && !(entity instanceof Projectile))
            return true;
        return false;
    }

    private static void addPoint(Map<Integer, List<TrailPoint>> map, int id, net.minecraft.world.phys.Vec3 pos, long now) {
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

    private static float toggleAlpha = 0.0f;

    public static boolean shouldRender() {
        return Modules.enabled(Trails.class) || toggleAlpha > 0.001f;
    }

    public static void renderTrails(Matrix4f modelViewMatrix, net.minecraft.world.phys.Vec3 camPos) {
        try {
            long now = System.currentTimeMillis();
            boolean enabled = Modules.enabled(Trails.class);
            
            // Smoothly animate the global module toggle fade-in and fade-out
            float targetToggle = enabled ? 1.0f : 0.0f;
            if (Math.abs(toggleAlpha - targetToggle) > 0.001f) {
                toggleAlpha += (targetToggle - toggleAlpha) * 0.12f;
            } else {
                toggleAlpha = targetToggle;
            }

            // Clean up and skip render if module is disabled and fully faded out
            if (!enabled && toggleAlpha <= 0.001f) {
                entityTrails.clear();
                playerTrails.clear();
                return;
            }

            boolean glowEnabled = Modules.get(Trails.class).glow;
            int glowLayersVal = (int) Modules.get(Trails.class).glowLayers;
            float glowSpreadVal = (float) Modules.get(Trails.class).glowSpread;
            renderFadingTrail(entityTrails, modelViewMatrix, camPos, now,
                    Modules.get(Trails.class).color, (float) Modules.get(Trails.class).width,
                    (long) (Modules.get(Trails.class).time * 1000.0),
                    glowEnabled, glowLayersVal, glowSpreadVal);
            renderFadingTrail(playerTrails, modelViewMatrix, camPos, now,
                    Modules.get(Trails.class).playerColor, (float) Modules.get(Trails.class).playerWidth,
                    (long) (Modules.get(Trails.class).playerTime * 1000.0),
                    glowEnabled, glowLayersVal, glowSpreadVal);
        } catch (Throwable t) {
            System.err.println("[RaveX] Trails render error: " + t.getMessage());
        }
    }

    private static void renderFadingTrail(Map<Integer, List<TrailPoint>> map,
            Matrix4f matrix, net.minecraft.world.phys.Vec3 camPos, long now,
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

                // Unique Animation: Tapered width at the tail + active sine pulse wave along the trail
                float renderAlpha = alpha * toggleAlpha;
                float wave = 1.0f + 0.15f * (float) Math.sin((System.currentTimeMillis() / 150.0) + (p1.time / 400.0));
                float renderWidth = lineWidth * (float) Math.pow(alpha, 0.75) * wave;

                if (renderAlpha <= 0.001f)
                    continue;

                segList.add((float) (p0.pos.x - camPos.x));
                segList.add((float) (p0.pos.y - camPos.y));
                segList.add((float) (p0.pos.z - camPos.z));
                segList.add((float) (p1.pos.x - camPos.x));
                segList.add((float) (p1.pos.y - camPos.y));
                segList.add((float) (p1.pos.z - camPos.z));
                segList.add(renderAlpha);
                segList.add(renderWidth);
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
        Render3DUtility.batchLineAdditive(matrix, List.of(new Vector3f(x1, y1, z1), new Vector3f(x2, y2, z2)), cr, cg, cb,
                alpha, width);
    }

    private static void renderCoreSeg(Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2,
            float cr, float cg, float cb, float alpha, float width) {
        Render3DUtility.batchLineStrip(matrix, List.of(new Vector3f(x1, y1, z1), new Vector3f(x2, y2, z2)), cr, cg, cb,
                alpha, width);
    }
    public void onDisable() {
        // Do nothing to let existing trails smoothly fade out
    }






}