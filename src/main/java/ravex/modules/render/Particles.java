package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ravex.event.Subscribe;
import ravex.event.combat.AttackEvent;

import ravex.utility.render.Render3DUtility;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "Particles", category = "Render")
public class Particles implements ModuleAccess {
private static String lastTrigger = "";
    @Parameter(name = "Shape", modes = {"Square", "Circle", "Triangle", "All"})
    public String shape = "All";
    @Parameter(name = "Trigger", modes = {"Always", "Walking", "Attack", "Mine", "Attack&Mine"})
    public String trigger = "Always";
    @Parameter(name = "ThroughWalls")
    public boolean throughWalls = true;
    @Parameter(name = "Amount", min = 5, max = 200, step = 5)
    public double amount = 30;
    @Parameter(name = "Size", min = 0.05, max = 2.0, step = 0.05)
    public double size = 0.5;
    @Parameter(name = "Speed", min = 0.0, max = 5.0, step = 0.1)
    public double speed = 1.5;
    @Parameter(name = "Color", color = true)
    public int color = 0xFFFF9BC4;
    @Parameter(name = "Lifetime", min = 0.5, max = 10.0, step = 0.5)
    public double lifetime = 4.0;
    @Parameter(name = "SpawnRate", min = 1, max = 20, step = 1)
    public double spawnRate = 2;
    @Parameter(name = "Spread", min = 0.5, max = 10.0, step = 0.5)
    public double spread = 2.0;
    @Parameter(name = "Gravity")
    public boolean gravity = false;
    @Parameter(name = "Mode", modes = {"AroundPlayer", "Fountain", "Rising", "Vortex", "Explosion"})
    public String mode = "AroundPlayer";
    @Parameter(name = "Alpha", min = 0.0, max = 1.0, step = 0.05)
    public double alpha = 1.0;
    @Parameter(name = "RotationSpeed", min = 0.0, max = 5.0, step = 0.1)
    public double rotationSpeed = 1.0;
    @Parameter(name = "Glow")
    public boolean glow = true;
    @Parameter(name = "Rainbow")
    public boolean rainbow = false;
    @Parameter(name = "LineWidth", min = 0.5, max = 10.0, step = 0.5)
    public double lineWidth = 3.5;
    @Parameter(name = "Segments", min = 6, max = 32, step = 2)
    public double segments = 16;
    @Parameter(name = "Collision")
    public boolean collide = true;
    public static boolean attackedThisTick = false;
    public static boolean minedThisTick = false;
    public static net.minecraft.world.phys.Vec3 lastAttackPos = null;
    public static net.minecraft.world.phys.Vec3 lastMinePos = null;

    @Subscribe
    public void onAttack(AttackEvent event) {
        attackedThisTick = true;
        lastAttackPos = event.getTarget().position();
    }

    private final List<Particle> particles = new ArrayList<>();
    private int spawnTimer = 0;
    private static final String[] SHAPES = {"Square", "Circle", "Triangle"};
    private static class Particle {
        net.minecraft.world.phys.Vec3 pos;
        net.minecraft.world.phys.Vec3 velocity;
        long spawnTime;
        float sizeMod;
        float rotation;
        float rotSpeed;
        String shapeType;
        int colorSeed;
        Particle(net.minecraft.world.phys.Vec3 pos, net.minecraft.world.phys.Vec3 velocity, long spawnTime, float sizeMod, float rotation, float rotSpeed, String shapeType, int colorSeed) {
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
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null || mc.getPlayer() == null) return;
        long now = System.currentTimeMillis();
        long maxAge = (long)(lifetime * 1000);
        String trig = trigger;
        if (!trig.equals(lastTrigger)) {
            particles.clear();
            lastTrigger = trig;
        }
        particles.removeIf(p -> now - p.spawnTime > maxAge);
        int maxParticles = (int) amount * 3;
        while (particles.size() > maxParticles) {
            particles.remove(0);
        }
        for (Particle p : particles) {
            updateParticle(p, mc);
        }
        switch (trig) {
            case "Walking" -> {
                double dx = mc.getPlayer().getX() - mc.getPlayer().xo;
                double dz = mc.getPlayer().getZ() - mc.getPlayer().zo;
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
            if (spawnTimer >= (int) spawnRate) {
                spawnTimer = 0;
                spawnParticles(mc, now, true);
            }
        }
    }
    private void spawnParticles(MinecraftWrapper mc, long now, boolean usePlayerPos) {
        net.minecraft.world.phys.Vec3 center;
        if (usePlayerPos) {
            center = mc.getPlayer().position().add(0, 1.2, 0);
        } else if (lastAttackPos != null && lastMinePos != null) {
            center = lastAttackPos.distanceToSqr(mc.getPlayer().position()) < lastMinePos.distanceToSqr(mc.getPlayer().position())
                ? lastAttackPos : lastMinePos;
        } else if (lastAttackPos != null) {
            center = lastAttackPos;
        } else if (lastMinePos != null) {
            center = lastMinePos;
        } else {
            center = mc.getPlayer().position().add(0, 1.2, 0);
        }
        lastAttackPos = null;
        lastMinePos = null;
        net.minecraft.util.RandomSource rnd = mc.getLevel().random;
        String shapeType = shape;
        String spawnMode = mode;
        double spreadVal = spread;
        float spd = (float) speed;
        for (int i = 0; i < 3; i++) {
            String s = shapeType.equals("All")
                ? SHAPES[rnd.nextInt(SHAPES.length)]
                : shapeType;
            net.minecraft.world.phys.Vec3 pos;
            net.minecraft.world.phys.Vec3 vel;
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
                    vel = new net.minecraft.world.phys.Vec3(
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
                    vel = new net.minecraft.world.phys.Vec3(
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
                    vel = new net.minecraft.world.phys.Vec3(
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
                    vel = new net.minecraft.world.phys.Vec3(
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
                    vel = net.minecraft.world.phys.Vec3.ZERO;
                }
            }
            particles.add(new Particle(
                pos, vel, now,
                0.5f + rnd.nextFloat() * 0.5f,
                rnd.nextFloat() * 360,
                (rnd.nextFloat() - 0.5f) * (float) rotationSpeed,
                s,
                rnd.nextInt()
            ));
        }
    }
    private void updateParticle(Particle p, MinecraftWrapper mc) {
        long age = System.currentTimeMillis() - p.spawnTime;
        float lifeProgress = (float) age / (float) (lifetime * 1000);
        p.rotation += p.rotSpeed;
        if (p.rotation > 360) p.rotation -= 360;
        if (p.rotation < 0) p.rotation += 360;
        net.minecraft.world.phys.Vec3 vel = p.velocity;
        double ax = vel.x, ay = vel.y, az = vel.z;
        if (gravity) {
            ay -= 0.004;
        }
        net.minecraft.world.phys.Vec3 newPos = p.pos.add(ax, ay, az);
        if (collide && mc.getLevel() != null) {
            net.minecraft.core.BlockPos blockPos = net.minecraft.core.BlockPos.containing(newPos);
            if (!mc.getLevel().getBlockState(blockPos).isAir()) {
                return;
            }
        }
        p.velocity = new net.minecraft.world.phys.Vec3(ax * 0.98, ay * 0.98, az * 0.98);
        p.pos = newPos;
    }
    public static void renderParticles(Matrix4f matrix, net.minecraft.world.phys.Vec3 camPos) {
        if (!ravex.manager.ModuleManager.delegate(Particles.class).getEnabled() || ravex.manager.ModuleManager.delegate(Particles.class).particles.isEmpty()) return;
        long now = System.currentTimeMillis();
        long maxAge = (long) (ravex.manager.ModuleManager.delegate(Particles.class).lifetime * 1000);
        float baseAlpha = (float) ravex.manager.ModuleManager.delegate(Particles.class).alpha;
        float baseSize = (float) ravex.manager.ModuleManager.delegate(Particles.class).size;
        boolean glowEnabled = ravex.manager.ModuleManager.delegate(Particles.class).glow;
        float lineW = (float) ravex.manager.ModuleManager.delegate(Particles.class).lineWidth;
        int seg = (int) ravex.manager.ModuleManager.delegate(Particles.class).segments;
        boolean rainbowMode = ravex.manager.ModuleManager.delegate(Particles.class).rainbow;
        int mainColor = ravex.manager.ModuleManager.delegate(Particles.class).color;
        for (Particle p : ravex.manager.ModuleManager.delegate(Particles.class).particles) {
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
            net.minecraft.world.phys.Vec3 toCamera = new net.minecraft.world.phys.Vec3(
                camPos.x - p.pos.x,
                camPos.y - p.pos.y,
                camPos.z - p.pos.z
            );
            double dist = toCamera.length();
            if (dist < 0.01) continue;
            toCamera = toCamera.scale(1.0 / dist);
            net.minecraft.world.phys.Vec3 up = new net.minecraft.world.phys.Vec3(0, 1, 0);
            if (Math.abs(toCamera.dot(up)) > 0.99) {
                up = new net.minecraft.world.phys.Vec3(1, 0, 0);
            }
            net.minecraft.world.phys.Vec3 right = toCamera.cross(up).normalize();
            up = right.cross(toCamera).normalize();
            float rad = particleSize * 0.5f;
            switch (p.shapeType) {
                case "Square" -> renderSquare(matrix, camPos, p, right, up, rad, r, g, b, alpha, lineW, glowEnabled);
                case "Circle" -> renderCircle(matrix, camPos, p, right, up, rad, r, g, b, alpha, lineW, glowEnabled, seg);
                case "Triangle" -> renderTriangle(matrix, camPos, p, right, up, rad, r, g, b, alpha, lineW, glowEnabled);
            }
        }
    }
    private static void renderSquare(Matrix4f matrix, net.minecraft.world.phys.Vec3 camPos, Particle p, net.minecraft.world.phys.Vec3 right, net.minecraft.world.phys.Vec3 up, float rad, float r, float g, float b, float alpha, float lineWidth, boolean glow) {
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
        boolean tw = ravex.manager.ModuleManager.delegate(Particles.class).throughWalls;
        if (glow) {
            Render3DUtility.batchLineAdditive(matrix, pts, r, g, b, alpha * 0.5f, lineWidth * 2, tw);
        }
        Render3DUtility.batchLineStrip(matrix, pts, r, g, b, alpha, lineWidth, tw);
    }
    private static void renderCircle(Matrix4f matrix, net.minecraft.world.phys.Vec3 camPos, Particle p, net.minecraft.world.phys.Vec3 right, net.minecraft.world.phys.Vec3 up, float rad, float r, float g, float b, float alpha, float lineWidth, boolean glow, int segments) {
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
        boolean tw = ravex.manager.ModuleManager.delegate(Particles.class).throughWalls;
        if (glow) {
            Render3DUtility.batchLineAdditive(matrix, pts, r, g, b, alpha * 0.5f, lineWidth * 2, tw);
        }
        Render3DUtility.batchLineStrip(matrix, pts, r, g, b, alpha, lineWidth, tw);
    }
    private static void renderTriangle(Matrix4f matrix, net.minecraft.world.phys.Vec3 camPos, Particle p, net.minecraft.world.phys.Vec3 right, net.minecraft.world.phys.Vec3 up, float rad, float r, float g, float b, float alpha, float lineWidth, boolean glow) {
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
        boolean tw = ravex.manager.ModuleManager.delegate(Particles.class).throughWalls;
        if (glow) {
            Render3DUtility.batchLineAdditive(matrix, pts, r, g, b, alpha * 0.5f, lineWidth * 2, tw);
        }
        Render3DUtility.batchLineStrip(matrix, pts, r, g, b, alpha, lineWidth, tw);
    }
    public void onDisable() {
        particles.clear();
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Particles").getEnabled();
    }

    public static Particles itz() {
        return ravex.manager.ModuleManager.delegate(Particles.class);
    }


}