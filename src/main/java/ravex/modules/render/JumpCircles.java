package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.utility.render.Render3DUtility;

@Module(name = "JumpCircles", category = "Render")
public class JumpCircles {
    @Parameter(name = "Style", modes = {"Classic", "Sexy"})
    public String style = "Classic";
    @Parameter(name = "Trigger", modes = {"Jump", "Landing", "Both"})
    public String trigger = "Jump";
    @Parameter(name = "Color", color = true)
    public int color = 0xFF40A9F8;
    @Parameter(name = "Color2", color = true, visible = "style=Sexy")
    public int color2 = 0xFFFF4444;
    @Parameter(name = "Radius", min = 0.3, max = 2.0, step = 0.1)
    public double radius = 0.9;
    @Parameter(name = "Life", min = 200.0, max = 2000.0, step = 50.0)
    public double life = 900.0;
    @Parameter(name = "Width", min = 1.0, max = 5.0, step = 0.5, visible = "style=Classic")
    public double width = 2.0;
    @Parameter(name = "Glow", visible = "style=Classic")
    public boolean glow = true;
    @Parameter(name = "Self")
    public boolean self = true;
    @Parameter(name = "Players")
    public boolean players = true;

    private record Circle(net.minecraft.world.phys.Vec3 pos, long birth) {
    }

    private static final List<Circle> circles = new ArrayList<>();
    private static float toggleAlpha = 0f;
    private final Map<Integer, Boolean> wasGround = new HashMap<>();
    private final Map<Integer, Double> lastY = new HashMap<>();

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null || mc.getPlayer() == null) return;
        long now = System.currentTimeMillis();
        long lifeMs = Math.max(50L, (long) life);
        circles.removeIf(c -> now - c.birth > lifeMs);
        if (circles.size() > 80) circles.subList(0, circles.size() - 80).clear();
        Set<Integer> seen = new HashSet<>();
        for (var e : mc.getLevel().entitiesForRendering()) {
            if (!(e instanceof net.minecraft.world.entity.player.Player p)) continue;
            boolean isSelf = p == mc.getPlayer();
            if (isSelf && !self) continue;
            if (!isSelf && !players) continue;
            int id = p.getId();
            seen.add(id);
            boolean g = p.onGround();
            double y = p.getY();
            Boolean pg = wasGround.get(id);
            Double py = lastY.get(id);
            if (pg != null && py != null) {
                boolean jumped = pg && !g && y > py + 0.01;
                boolean landed = !pg && g && y < py - 0.05;
                if ((jumped && !"Landing".equals(trigger)) || (landed && !"Jump".equals(trigger))) {
                    circles.add(new Circle(new net.minecraft.world.phys.Vec3(p.getX(), y, p.getZ()), now));
                }
            }
            wasGround.put(id, g);
            lastY.put(id, y);
        }
        wasGround.keySet().retainAll(seen);
        lastY.keySet().retainAll(seen);
    }

    public void onDisable() {
        circles.clear();
        wasGround.clear();
        lastY.clear();
    }

    public static boolean shouldRender() {
        return Modules.enabled(JumpCircles.class) || toggleAlpha > 0.001f;
    }

    public static void renderCircles(Matrix4f matrix, net.minecraft.world.phys.Vec3 camPos) {
        try {
            JumpCircles m = Modules.get(JumpCircles.class);
            boolean enabled = Modules.enabled(JumpCircles.class);
            float target = enabled ? 1f : 0f;
            if (Math.abs(toggleAlpha - target) > 0.001f) toggleAlpha += (target - toggleAlpha) * 0.12f;
            else toggleAlpha = target;
            if ((!enabled && toggleAlpha <= 0.001f) || m == null) {
                if (!enabled) circles.clear();
                return;
            }
            long now = System.currentTimeMillis();
            long lifeMs = Math.max(50L, (long) m.life);
            float maxR = (float) m.radius;
            float lw = (float) m.width;
            circles.removeIf(c -> now - c.birth > lifeMs);
            boolean sexy = "Sexy".equals(m.style);
            int baseColor = m.color;
            for (Circle c : circles) {
                float t = (now - c.birth) / (float) lifeMs;
                if (t < 0f || t >= 1f) continue;
                double cx = c.pos.x - camPos.x;
                double cy = c.pos.y + 0.12 - camPos.y;
                double cz = c.pos.z - camPos.z;
                if (sexy) drawSexyBand(matrix, cx, cy, cz, t, now, maxR, m.color, m.color2);
                else drawClassicRing(matrix, cx, cy, cz, t, maxR, lw, baseColor, m.glow);
            }
        } catch (Throwable t) {
            System.err.println("[RaveX] JumpCircles render error: " + t.getMessage());
        }
    }

    private static void bandQuad(Matrix4f matrix, double cx, double cy, double cz,
            double a0, double a1, float r0, float r1, float[] c, float a) {
        float y = (float) cy;
        Render3DUtility.batchFlatBandQuad(matrix,
            (float) (cx + Math.cos(a0) * r0), y, (float) (cz + Math.sin(a0) * r0),
            (float) (cx + Math.cos(a0) * r1), y, (float) (cz + Math.sin(a0) * r1),
            (float) (cx + Math.cos(a1) * r1), y, (float) (cz + Math.sin(a1) * r1),
            (float) (cx + Math.cos(a1) * r0), y, (float) (cz + Math.sin(a1) * r0),
            c[0], c[1], c[2], a);
    }

    private static float[] avgColor(float[] c0, float[] c1) {
        return new float[]{(c0[0] + c1[0]) * 0.5f, (c0[1] + c1[1]) * 0.5f, (c0[2] + c1[2]) * 0.5f};
    }

    private static void drawClassicRing(Matrix4f matrix, double cx, double cy, double cz,
            float t, float maxR, float lw, int baseColor, boolean glow) {
        float r = maxR * (1f - (1f - t) * (1f - t));
        float a = (1f - t) * ((baseColor >> 24) & 0xFF) / 255f * toggleAlpha;
        if (r <= 0.02f) return;
        float half = (float) (lw * 0.02);
        float[] c = new float[]{
            ((baseColor >> 16) & 0xFF) / 255f,
            ((baseColor >> 8) & 0xFF) / 255f,
            (baseColor & 0xFF) / 255f};
        int segs = 64;
        double step = Math.PI * 2.0 / segs;
        if (glow && a > 0.004f) {
            float gh = half * 2.2f;
            for (int i = 0; i < segs; i++) {
                bandQuad(matrix, cx, cy, cz, i * step, (i + 1) * step,
                    Math.max(0.001f, r - gh), r + gh, c, a * 0.22f);
            }
        }
        if (a <= 0.003f) return;
        for (int i = 0; i < segs; i++) {
            bandQuad(matrix, cx, cy, cz, i * step, (i + 1) * step,
                Math.max(0.001f, r - half), r + half, c, a);
        }
    }

    private static float[] mixColors(int c1, int c2, float f) {
        float r = (((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * f) / 255f;
        float g = (((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * f) / 255f;
        float b = ((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * f) / 255f;
        return new float[]{r, g, b};
    }

    private static void drawSexyBand(Matrix4f matrix, double cx, double cy, double cz,
            float t, long now, float maxR, int color1, int color2) {
        float end = maxR * 2.2f * t;
        float start = end / 3f;
        float middle = (start + end) / 2f;
        float fade = (1f - t) * toggleAlpha;
        if (end <= 0.03f || fade <= 0.004f) return;
        int segs = 64;
        int sub = 5;
        double timeSweep = now / 1400.0;
        double step = Math.PI * 2.0 / segs;
        for (int i = 0; i < segs; i++) {
            double f0 = i / (double) segs;
            double f1 = (i + 1) / (double) segs;
            double a0 = i * step;
            double a1 = (i + 1) * step;
            float[] base0 = mixColors(color1, color2, 0.5f + 0.5f * (float) Math.sin((f0 + timeSweep) * Math.PI * 2.0));
            float[] base1 = mixColors(color1, color2, 0.5f + 0.5f * (float) Math.sin((f1 + timeSweep) * Math.PI * 2.0));
            float[] cm = avgColor(base0, base1);
            for (int s = 0; s < sub; s++) {
                float inR = start + (middle - start) * s / (float) sub;
                float outR = start + (middle - start) * (s + 1) / (float) sub;
                float aMid = (s + 0.5f) / (float) sub * fade;
                bandQuad(matrix, cx, cy, cz, a0, a1, inR, outR, cm, aMid);
            }
            for (int s = 0; s < sub; s++) {
                float inR = middle + (end - middle) * s / (float) sub;
                float outR = middle + (end - middle) * (s + 1) / (float) sub;
                float aMid = (sub - s - 0.5f) / (float) sub * fade;
                bandQuad(matrix, cx, cy, cz, a0, a1, inR, outR, cm, aMid);
            }
        }
    }
}
