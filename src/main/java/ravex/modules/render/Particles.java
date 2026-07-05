package ravex.modules.render;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.render.Render3DUtils;
import java.util.ArrayList;
import java.util.List;
public class Particles extends Module {
    public static final Particles INSTANCE = new Particles();
    private static String lastTrigger = "";
    public final ModeParameter shape = new ModeParameter("Shape", "All",
        List.of("Square", "Circle", "Triangle", "All"));
    public final ModeParameter trigger = new ModeParameter("Trigger", "Always",
        List.of("Always", "Walking", "Attack", "Mine", "Attack&Mine"));
    public final BooleanParameter throughWalls = new BooleanParameter("Through Walls", true);
    public final NumberParameter amount = new NumberParameter("Amount", 30, 5, 200, 5);
    public final NumberParameter size = new NumberParameter("Size", 0.5, 0.05, 2.0, 0.05);
    public final NumberParameter speed = new NumberParameter("Speed", 1.5, 0.0, 5.0, 0.1);
    public final ColorParameter color = new ColorParameter("Color", 0xFFFF9BC4);
    public final NumberParameter lifetime = new NumberParameter("Lifetime", 4.0, 0.5, 10.0, 0.5);
    public final NumberParameter spawnRate = new NumberParameter("Spawn Rate", 2, 1, 20, 1);
    public final NumberParameter spread = new NumberParameter("Spread", 2.0, 0.5, 10.0, 0.5);
    public final BooleanParameter gravity = new BooleanParameter("Gravity", false);
    public final ModeParameter mode = new ModeParameter("Mode", "AroundPlayer",
        List.of("AroundPlayer", "Fountain", "Rising", "Vortex", "Explosion"));
    public final NumberParameter alpha = new NumberParameter("Alpha", 1.0, 0.0, 1.0, 0.05);
    public final NumberParameter rotationSpeed = new NumberParameter("Rotation Speed", 1.0, 0.0, 5.0, 0.1);
    public final BooleanParameter glow = new BooleanParameter("Glow", true);
    public final BooleanParameter rainbow = new BooleanParameter("Rainbow", false);
    public final NumberParameter lineWidth = new NumberParameter("Line Width", 3.5, 0.5, 10.0, 0.5);
    public final NumberParameter segments = new NumberParameter("Segments", 16, 6, 32, 2);
    public final BooleanParameter collide = new BooleanParameter("Collision", true);
    public static boolean attackedThisTick = false;
    public static boolean minedThisTick = false;
    public static Vec3 lastAttackPos = null;
    public static Vec3 lastMinePos = null;
    private final List<Particle> particles = new ArrayList<>();
    private int spawnTimer = 0;
    private static final String[] SHAPES = {"Square", "Circle", "Triangle"};
    private static class Particle {
        Vec3 pos;
        Vec3 velocity;
        long spawnTime;
        float sizeMod;
        float rotation;
        float rotSpeed;
        String shapeType;
        int colorSeed;
        Particle(Vec3 pos, Vec3 velocity, long spawnTime, float sizeMod, float rotation, float rotSpeed, String shapeType, int colorSeed) {
            this.pos = pos;
            this.velocity = velocity;
            this.spawnTime = spawnTime;
            this.sizeMod = sizeMod;
            this.rotation = rotation;
            this.rotSpeed = rotSpeed;
            this.shapeType = shapeType;
            this.colorSeed = colorSeed;
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        long now = System.currentTimeMillis();
        long maxAge = (long)(lifetime.getValue() * 1000);
        String trig = trigger.getValue();
        if (!trig.equals(lastTrigger)) {
            particles.clear();
            lastTrigger = trig;
        }
        particles.removeIf(p -> now - p.spawnTime > maxAge);
        int maxParticles = amount.getValue().intValue() * 3;
        while (particles.size() > maxParticles) {
            particles.remove(0);
        }
        for (Particle p : particles) {
            updateParticle(p, mc);
        }
        switch (trig) {
            case "Walking" -> {
                double dx = mc.player.getX() - mc.player.xo;
                double dz = mc.player.getZ() - mc.player.zo;
                boolean moving = dx * dx + dz * dz > 0.0001;
                if (!moving) {
                    attackedThisTick = false;
                    minedThisTick = false;
                    return;
                }
            }
            case "Attack" -> {
                if (!attackedThisTick) {
                    attackedThisTick = false;
                    minedThisTick = false;
                    return;
                }
            }
            case "Mine" -> {
                if (!minedThisTick) {
                    attackedThisTick = false;
                    minedThisTick = false;
                    return;
                }
            }
            case "Attack&Mine" -> {
                if (!attackedThisTick && !minedThisTick) {
                    attackedThisTick = false;
                    minedThisTick = false;
                    return;
                }
            }
        }
        boolean isEventTrigger = trig.equals("Attack") || trig.equals("Mine") || trig.equals("Attack&Mine");
        boolean usePlayerPos = !trig.equals("Attack") && !trig.equals("Mine") && !trig.equals("Attack&Mine");
        attackedThisTick = false;
        minedThisTick = false;
        if (isEventTrigger) {
            spawnParticles(mc, now, usePlayerPos);
        } else {
            spawnTimer++;
            if (spawnTimer >= spawnRate.getValue().intValue()) {
                spawnTimer = 0;
                spawnParticles(mc, now, true);
            }
        }
    }
    private void spawnParticles(Minecraft mc, long now, boolean usePlayerPos) {
        Vec3 center;
        if (usePlayerPos) {
            center = mc.player.position().add(0, 1.2, 0);
        } else if (lastAttackPos != null && lastMinePos != null) {
            center = lastAttackPos.distanceToSqr(mc.player.position()) < lastMinePos.distanceToSqr(mc.player.position())
                ? lastAttackPos : lastMinePos;
        } else if (lastAttackPos != null) {
            center = lastAttackPos;
        } else if (lastMinePos != null) {
            center = lastMinePos;
        } else {
            center = mc.player.position().add(0, 1.2, 0);
        }
        lastAttackPos = null;
        lastMinePos = null;
        net.minecraft.util.RandomSource rnd = mc.level.random;
        String shapeType = shape.getValue();
        String spawnMode = mode.getValue();
        double spreadVal = spread.getValue();
        float spd = speed.getValue().floatValue();
        for (int i = 0; i < 3; i++) {
            String s = shapeType.equals("All")
                ? SHAPES[rnd.nextInt(SHAPES.length)]
                : shapeType;
            Vec3 pos;
            Vec3 vel;
            switch (spawnMode) {
                case "AroundPlayer" -> {
                    double theta = rnd.nextDouble() * Math.PI * 2;
                    double phi = rnd.nextDouble() * Math.PI;
                    double rad = spreadVal * (0.3 + rnd.nextDouble() * 0.7);
                    pos = center.add(
                        rad * Math.sin(phi) * Math.cos(theta),
                        rad * Math.cos(phi),
                        rad * Math.sin(phi) * Math.sin(theta)
                    );
                    vel = center.subtract(pos).scale(0.03 * spd);
                }
                case "Fountain" -> {
                    pos = center.add(
                        (rnd.nextDouble() - 0.5) * spreadVal * 0.5,
                        0,
                        (rnd.nextDouble() - 0.5) * spreadVal * 0.5
                    );
                    vel = new Vec3(
                        (rnd.nextDouble() - 0.5) * 0.1 * spd,
                        0.15 + rnd.nextDouble() * 0.2 * spd,
                        (rnd.nextDouble() - 0.5) * 0.1 * spd
                    );
                }
                case "Rising" -> {
                    pos = center.add(
                        (rnd.nextDouble() - 0.5) * spreadVal,
                        (rnd.nextDouble() - 0.5) * spreadVal * 0.3,
                        (rnd.nextDouble() - 0.5) * spreadVal
                    );
                    vel = new Vec3(
                        (rnd.nextDouble() - 0.5) * 0.02 * spd,
                        0.03 + rnd.nextDouble() * 0.05 * spd,
                        (rnd.nextDouble() - 0.5) * 0.02 * spd
                    );
                }
                case "Vortex" -> {
                    double angle = rnd.nextDouble() * Math.PI * 2;
                    double rad = rnd.nextDouble() * spreadVal;
                    pos = center.add(
                        rad * Math.cos(angle),
                        (rnd.nextDouble() - 0.5) * spreadVal * 0.3,
                        rad * Math.sin(angle)
                    );
                    vel = new Vec3(
                        -Math.sin(angle) * 0.05 * spd,
                        (rnd.nextDouble() - 0.5) * 0.01 * spd,
                        Math.cos(angle) * 0.05 * spd
                    );
                }
                case "Explosion" -> {
                    pos = center;
                    double theta = rnd.nextDouble() * Math.PI * 2;
                    double phi = rnd.nextDouble() * Math.PI;
                    double force = 0.1 + rnd.nextDouble() * 0.25 * spd;
                    vel = new Vec3(
                        Math.sin(phi) * Math.cos(theta) * force,
                        Math.cos(phi) * force,
                        Math.sin(phi) * Math.sin(theta) * force
                    );
                }
                default -> {
                    pos = center.add(
                        (rnd.nextDouble() - 0.5) * spreadVal,
                        (rnd.nextDouble() - 0.5) * spreadVal,
                        (rnd.nextDouble() - 0.5) * spreadVal
                    );
                    vel = Vec3.ZERO;
                }
            }
            particles.add(new Particle(
                pos, vel, now,
                0.5f + rnd.nextFloat() * 0.5f,
                rnd.nextFloat() * 360,
                (rnd.nextFloat() - 0.5f) * rotationSpeed.getValue().floatValue(),
                s,
                rnd.nextInt()
            ));
        }
    }
    private void updateParticle(Particle p, Minecraft mc) {
        long age = System.currentTimeMillis() - p.spawnTime;
        float lifeProgress = (float) age / (float) (lifetime.getValue() * 1000);
        p.rotation += p.rotSpeed;
        if (p.rotation > 360) p.rotation -= 360;
        if (p.rotation < 0) p.rotation += 360;
        Vec3 vel = p.velocity;
        double ax = vel.x, ay = vel.y, az = vel.z;
        if (gravity.getValue()) {
            ay -= 0.004;
        }
        Vec3 newPos = p.pos.add(ax, ay, az);
        if (collide.getValue() && mc.level != null) {
            BlockPos blockPos = BlockPos.containing(newPos);
            if (!mc.level.getBlockState(blockPos).isAir()) {
                return;
            }
        }
        p.velocity = new Vec3(ax * 0.98, ay * 0.98, az * 0.98);
        p.pos = newPos;
    }
    public static void renderParticles(Matrix4f matrix, Vec3 camPos) {
        if (!INSTANCE.getEnabled() || INSTANCE.particles.isEmpty()) return;
        long now = System.currentTimeMillis();
        long maxAge = (long) (INSTANCE.lifetime.getValue() * 1000);
        float baseAlpha = INSTANCE.alpha.getValue().floatValue();
        float baseSize = INSTANCE.size.getValue().floatValue();
        boolean glowEnabled = INSTANCE.glow.getValue();
        float lineW = INSTANCE.lineWidth.getValue().floatValue();
        int seg = INSTANCE.segments.getValue().intValue();
        boolean rainbowMode = INSTANCE.rainbow.getValue();
        int mainColor = INSTANCE.color.getValue();
        for (Particle p : INSTANCE.particles) {
            long age = now - p.spawnTime;
            float lifeProgress = (float) age / (float) maxAge;
            if (lifeProgress >= 1.0f) continue;
            float alpha = baseAlpha * Math.max(0.0f, 1.0f - lifeProgress);
            if (alpha <= 0.01f) continue;
            float particleSize = baseSize * p.sizeMod;
            int color;
            if (rainbowMode) {
                float hue = (System.currentTimeMillis() % 3600) / 3600f;
                color = java.awt.Color.HSBtoRGB(hue + p.colorSeed * 0.01f, 0.8f, 1.0f);
            } else {
                color = mainColor;
            }
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            Vec3 toCamera = new Vec3(
                camPos.x - p.pos.x,
                camPos.y - p.pos.y,
                camPos.z - p.pos.z
            );
            double dist = toCamera.length();
            if (dist < 0.01) continue;
            toCamera = toCamera.scale(1.0 / dist);
            Vec3 up = new Vec3(0, 1, 0);
            if (Math.abs(toCamera.dot(up)) > 0.99) {
                up = new Vec3(1, 0, 0);
            }
            Vec3 right = toCamera.cross(up).normalize();
            up = right.cross(toCamera).normalize();
            float rad = particleSize * 0.5f;
            switch (p.shapeType) {
                case "Square" -> renderSquare(matrix, camPos, p, right, up, rad, r, g, b, alpha, lineW, glowEnabled);
                case "Circle" -> renderCircle(matrix, camPos, p, right, up, rad, r, g, b, alpha, lineW, glowEnabled, seg);
                case "Triangle" -> renderTriangle(matrix, camPos, p, right, up, rad, r, g, b, alpha, lineW, glowEnabled);
            }
        }
    }
    private static void renderSquare(Matrix4f matrix, Vec3 camPos, Particle p, Vec3 right, Vec3 up, float rad, float r, float g, float b, float alpha, float lineWidth, boolean glow) {
        double angle = Math.toRadians(p.rotation);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        double rx = right.x * cos + up.x * sin;
        double ry = right.y * cos + up.y * sin;
        double rz = right.z * cos + up.z * sin;
        double ux = right.x * -sin + up.x * cos;
        double uy = right.y * -sin + up.y * cos;
        double uz = right.z * -sin + up.z * cos;
        List<Vector3f> pts = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            float a = (float) (i * Math.PI / 2);
            float c = (float) Math.cos(a);
            float s = (float) Math.sin(a);
            pts.add(new Vector3f(
                (float) (p.pos.x + rx * rad * c + ux * rad * s - camPos.x),
                (float) (p.pos.y + ry * rad * c + uy * rad * s - camPos.y),
                (float) (p.pos.z + rz * rad * c + uz * rad * s - camPos.z)
            ));
        }
        pts.add(pts.get(0));
        boolean tw = INSTANCE.throughWalls.getValue();
        if (glow) {
            Render3DUtils.batchLineAdditive(matrix, pts, r, g, b, alpha * 0.5f, lineWidth * 2, tw);
        }
        Render3DUtils.batchLineStrip(matrix, pts, r, g, b, alpha, lineWidth, tw);
    }
    private static void renderCircle(Matrix4f matrix, Vec3 camPos, Particle p, Vec3 right, Vec3 up, float rad, float r, float g, float b, float alpha, float lineWidth, boolean glow, int segments) {
        double angle = Math.toRadians(p.rotation);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        double rx = right.x * cos + up.x * sin;
        double ry = right.y * cos + up.y * sin;
        double rz = right.z * cos + up.z * sin;
        double ux = right.x * -sin + up.x * cos;
        double uy = right.y * -sin + up.y * cos;
        double uz = right.z * -sin + up.z * cos;
        List<Vector3f> pts = new ArrayList<>();
        for (int i = 0; i <= segments; i++) {
            float a = (float) (i * 2 * Math.PI / segments);
            float c = (float) Math.cos(a);
            float s = (float) Math.sin(a);
            pts.add(new Vector3f(
                (float) (p.pos.x + rx * rad * c + ux * rad * s - camPos.x),
                (float) (p.pos.y + ry * rad * c + uy * rad * s - camPos.y),
                (float) (p.pos.z + rz * rad * c + uz * rad * s - camPos.z)
            ));
        }
        boolean tw = INSTANCE.throughWalls.getValue();
        if (glow) {
            Render3DUtils.batchLineAdditive(matrix, pts, r, g, b, alpha * 0.5f, lineWidth * 2, tw);
        }
        Render3DUtils.batchLineStrip(matrix, pts, r, g, b, alpha, lineWidth, tw);
    }
    private static void renderTriangle(Matrix4f matrix, Vec3 camPos, Particle p, Vec3 right, Vec3 up, float rad, float r, float g, float b, float alpha, float lineWidth, boolean glow) {
        double angle = Math.toRadians(p.rotation);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        double rx = right.x * cos + up.x * sin;
        double ry = right.y * cos + up.y * sin;
        double rz = right.z * cos + up.z * sin;
        double ux = right.x * -sin + up.x * cos;
        double uy = right.y * -sin + up.y * cos;
        double uz = right.z * -sin + up.z * cos;
        List<Vector3f> pts = new ArrayList<>();
        for (int i = 0; i <= 3; i++) {
            float a = (float) (i * 2 * Math.PI / 3 + Math.toRadians(270));
            float c = (float) Math.cos(a);
            float s = (float) Math.sin(a);
            pts.add(new Vector3f(
                (float) (p.pos.x + rx * rad * c + ux * rad * s - camPos.x),
                (float) (p.pos.y + ry * rad * c + uy * rad * s - camPos.y),
                (float) (p.pos.z + rz * rad * c + uz * rad * s - camPos.z)
            ));
        }
        boolean tw = INSTANCE.throughWalls.getValue();
        if (glow) {
            Render3DUtils.batchLineAdditive(matrix, pts, r, g, b, alpha * 0.5f, lineWidth * 2, tw);
        }
        Render3DUtils.batchLineStrip(matrix, pts, r, g, b, alpha, lineWidth, tw);
    }
    @Override
    protected void onDisable() {
        particles.clear();
    }
}
