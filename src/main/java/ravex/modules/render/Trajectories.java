package ravex.modules.render;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.network.NetworkUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.PlayerUtility;
import ravex.utility.render.Render3DUtility;

// predicts projectile landing, previews the held bow shot
@Module(name = "Trajectories", category = "Render")
public class Trajectories {
    @Parameter(name = "FiredProjectiles")
    public boolean firedProjectiles = true;
    @Parameter(name = "OwnThrow")
    public boolean ownThrow = true;
    @Parameter(name = "MaxProjectiles", min = 1, max = 16, step = 1)
    public double maxProjectiles = 6;
    @Parameter(name = "Steps", min = 20, max = 240, step = 10)
    public double steps = 160;
    @Parameter(name = "LineColor", color = true)
    public int lineColor = 0xFF40A9F8;
    @Parameter(name = "Width", min = 0.5, max = 4.0, step = 0.5)
    public double width = 1.5;

    // vanilla projectile constants
    private static final Physics THROWN = new Physics(0.03, 0.99, 0.8, 1.5, 0.0f, true, true, true);
    private static final Physics POTION = new Physics(0.05, 0.99, 0.8, 0.5, -20.0f, true, true, true);
    private static final Physics EXP_BOTTLE = new Physics(0.07, 0.99, 0.8, 0.7, -20.0f, true, true, true);
    private static final Physics ARROW = new Physics(0.05, 0.99, 0.6, 3.0, 0.0f, true, false, false);
    private static final Physics CROSSBOW_ARROW = new Physics(0.05, 0.99, 0.6, 3.15, 0.0f, true, false, false);
    private static final Physics TRIDENT = new Physics(0.05, 0.99, 0.99, 2.5, 0.0f, true, false, false);
    private static final Physics FIREWORK = new Physics(0.0, 1.0, 1.0, 1.6, 0.0f, false, false, false);
    private static final Physics WIND_CHARGE = new Physics(0.0, 1.0, 1.0, 1.5, 0.0f, true, true, false);
    private static final Physics FIREBALL = new Physics(0.0, 0.99, 0.8, 1.5, 0.0f, true, true, false);
    private static final Physics GENERIC = new Physics(0.03, 0.99, 0.8, 1.5, 0.0f, true, true, true);
    // line and marker also show through walls
    private static final boolean THROUGH_WALLS = true;

    private static final class Physics {
        final double gravity;
        final double drag;
        final double waterDrag;
        final double speed;
        final float roll;
        // shoot momentum comes from the shooter
        final boolean copiesPlayerVelocity;
        // gravity and drag apply before the move
        final boolean acceleratesFirst;
        // throwable hit boxes are 0.3 larger
        final boolean inflatesEntityBox;

        Physics(double gravity, double drag, double waterDrag, double speed, float roll,
                boolean copiesPlayerVelocity, boolean acceleratesFirst, boolean inflatesEntityBox) {
            this.gravity = gravity;
            this.drag = drag;
            this.waterDrag = waterDrag;
            this.speed = speed;
            this.roll = roll;
            this.copiesPlayerVelocity = copiesPlayerVelocity;
            this.acceleratesFirst = acceleratesFirst;
            this.inflatesEntityBox = inflatesEntityBox;
        }

        Physics withSpeed(double speed) {
            return new Physics(gravity, drag, waterDrag, speed, roll,
                    copiesPlayerVelocity, acceleratesFirst, inflatesEntityBox);
        }
    }

    // landing geometry is kept in primitives so no minecraft types leak in
    private static final class Path {
        final List<Vector3d> points = new ArrayList<>();
        boolean hasStart;
        double startX, startY, startZ;
        boolean hasLanding;
        double landingX, landingY, landingZ;
        double normalX, normalY = 1.0, normalZ;
        boolean hasBlock;
        int blockX, blockY, blockZ;
        EntityUtility.Box hitBox;
    }

    private final List<Path> paths = new ArrayList<>();

    public void onTick() {
        paths.clear();
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        int maxSteps = (int) steps;

        if (ownThrow) {
            Path own = simulateOwnThrow(maxSteps);
            if (own != null) paths.add(own);
        }

        if (firedProjectiles) {
            int limit = (int) maxProjectiles;
            int fired = 0;
            for (var entity : EntityUtility.entitiesForRendering()) {
                Physics ph = physicsOf(entity);
                if (ph == null) continue;
                Vector3d velocity = EntityUtility.deltaMovement(entity);
                if (velocity.lengthSquared() < 1.0E-5) continue;
                Vector3d position = EntityUtility.position(entity);
                Path path = new Path();
                simulate(ph, entity, position.x, position.y, position.z, velocity, maxSteps, path);
                if (!path.points.isEmpty() || path.hasLanding) {
                    paths.add(path);
                    if (++fired >= limit) break;
                }
            }
        }
    }

    private Path simulateOwnThrow(int maxSteps) {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        var stack = InventoryUtility.getMainHand(player);
        if (stack.isEmpty()) stack = InventoryUtility.getOffhand(player);
        if (stack.isEmpty()) return null;

        Physics ph = physicsOfItem(player, stack);
        if (ph == null) return null;

        // potions and bottles use a -20 roll
        Vector3d velocity = directionFromRot(PlayerUtility.getYaw(), PlayerUtility.getPitch(), ph.roll).mul(ph.speed);
        if (ph.copiesPlayerVelocity) {
            // same movement the server knows, y only in air
            Vector3d motion = NetworkUtility.knownMovement(player);
            velocity.add(motion.x, PlayerUtility.isOnGround(player) ? 0.0 : motion.y, motion.z);
        }
        Vector3d eye = EntityUtility.eyePosition(player);
        Path path = new Path();
        simulate(ph, player, eye.x, eye.y - 0.1, eye.z, velocity, maxSteps, path);
        if (path.points.isEmpty() && !path.hasLanding) return null;
        return path;
    }

    private Physics physicsOfItem(Object player, Object stack) {
        switch (InventoryUtility.projectileItemKind(stack)) {
            case BOW: {
                // real charge while drawing, full charge otherwise
                float power = InventoryUtility.getBowDrawPower(player, stack);
                if (power < 0.1f) return null;
                return ARROW.withSpeed(power * ARROW.speed);
            }
            case CROSSBOW: {
                if (!InventoryUtility.isCrossbowCharged(stack)) return null;
                return CROSSBOW_ARROW;
            }
            case THROWABLE: {
                return THROWN;
            }
            case POTION: {
                return POTION;
            }
            case EXPERIENCE_BOTTLE: {
                return EXP_BOTTLE;
            }
            case TRIDENT: {
                return TRIDENT;
            }
            case WIND_CHARGE: {
                return WIND_CHARGE;
            }
            case FIREWORK: {
                return FIREWORK;
            }
            default: {
                return null;
            }
        }
    }

    private Physics physicsOf(Object entity) {
        switch (EntityUtility.projectileKind(entity)) {
            case POTION: {
                return POTION;
            }
            case EXPERIENCE_BOTTLE: {
                return EXP_BOTTLE;
            }
            case THROWABLE: {
                return THROWN;
            }
            case TRIDENT: {
                return TRIDENT;
            }
            case ARROW: {
                return ARROW;
            }
            case FIREWORK: {
                return FIREWORK;
            }
            case WIND_CHARGE: {
                return WIND_CHARGE;
            }
            case FIREBALL: {
                return FIREBALL;
            }
            case GENERIC: {
                return GENERIC;
            }
            default: {
                return null;
            }
        }
    }

    // vanilla shot direction, roll tilts it up
    private static Vector3d directionFromRot(float yawDeg, float pitchDeg, float rollDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double roll = Math.toRadians(rollDeg);
        return new Vector3d(
                -Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch + roll),
                Math.cos(yaw) * Math.cos(pitch)).normalize();
    }

    // steps the projectile tick by tick, landing comes from a block raycast
    private void simulate(Physics ph, Object owner, double startX, double startY, double startZ,
            Vector3d velocity, int maxSteps, Path path) {
        var level = MinecraftWrapper.getWrapper().getLevel();
        if (level == null) return;
        double minY = BlockUtility.getMinY(level);
        path.hasStart = true;
        path.startX = startX;
        path.startY = startY;
        path.startZ = startZ;

        double posX = startX, posY = startY, posZ = startZ;
        Vector3d vel = new Vector3d(velocity);

        for (int i = 0; i < maxSteps; i++) {
            // thrown items accelerate before they move
            if (ph.acceleratesFirst) damp(ph, vel, BlockUtility.hasFluidAt(posX, posY, posZ));

            if (posY < minY) {
                path.hasLanding = true;
                path.landingX = posX;
                path.landingY = posY;
                path.landingZ = posZ;
                break;
            }

            double prevX = posX, prevY = posY, prevZ = posZ;
            double nextX = posX + vel.x;
            double nextY = posY + vel.y;
            double nextZ = posZ + vel.z;

            var hit = BlockUtility.clipSegment(owner, prevX, prevY, prevZ, nextX, nextY, nextZ);
            if (hit != null) {
                path.hasLanding = true;
                path.landingX = hit.x;
                path.landingY = hit.y;
                path.landingZ = hit.z;
                path.normalX = hit.normalX;
                path.normalY = hit.normalY;
                path.normalZ = hit.normalZ;
                path.hasBlock = true;
                path.blockX = hit.blockX;
                path.blockY = hit.blockY;
                path.blockZ = hit.blockZ;
                break;
            }

            posX = nextX;
            posY = nextY;
            posZ = nextZ;
            path.points.add(new Vector3d(posX, posY, posZ));

            if (!ph.acceleratesFirst) damp(ph, vel, BlockUtility.hasFluidAt(posX, posY, posZ));
        }

        checkEntityHit(path, owner, ph);
    }

    private static void damp(Physics ph, Vector3d vel, boolean inWater) {
        double drag = inWater ? ph.waterDrag : ph.drag;
        vel.set(vel.x * drag, vel.y * drag - ph.gravity, vel.z * drag);
    }

    // finds the nearest entity on the path, the path ends on it
    private void checkEntityHit(Path path, Object owner, Physics ph) {
        var hit = EntityUtility.findProjectileHit(
                owner, path.startX, path.startY, path.startZ,
                path.points, path.hasLanding, path.landingX, path.landingY, path.landingZ,
                ph.inflatesEntityBox, 0.3);
        if (hit == null) return;

        path.hitBox = hit.box;
        path.hasLanding = true;
        path.landingX = hit.x;
        path.landingY = hit.y;
        path.landingZ = hit.z;
        path.hasBlock = false;

        double normalX = hit.x - path.startX;
        double normalY = hit.y - path.startY;
        double normalZ = hit.z - path.startZ;
        double lengthSq = normalX * normalX + normalY * normalY + normalZ * normalZ;
        if (lengthSq > 1.0E-8) {
            double length = Math.sqrt(lengthSq);
            path.normalX = normalX / length;
            path.normalY = normalY / length;
            path.normalZ = normalZ / length;
        } else {
            path.normalX = 0.0;
            path.normalY = 0.0;
            path.normalZ = 0.0;
        }

        if (hit.segment < path.points.size()) {
            path.points.subList(hit.segment, path.points.size()).clear();
        }
        if (path.points.isEmpty()) {
            path.points.add(new Vector3d(hit.x, hit.y, hit.z));
        }
    }

    public void onDisable() {
        paths.clear();
    }

    public List<Path> getPaths() {
        return paths;
    }

    private static final Matrix4f REUSABLE_MATRIX = new Matrix4f();
    private static final List<Vector3f> RELATIVE = new ArrayList<>();

    // a vector that lies flat on the given normal
    private static Vector3d perpendicular(Vector3d normal) {
        Vector3d axis = Math.abs(normal.y) > 0.9 ? new Vector3d(1.0, 0.0, 0.0) : new Vector3d(0.0, 1.0, 0.0);
        Vector3d flat = new Vector3d(normal).cross(axis);
        if (flat.lengthSquared() < 1.0E-8) return flat.set(0.0, 0.0, 0.0);
        return flat.normalize();
    }

    // the block outline module already draws a box around that block
    private static boolean outlinedByBlockOutline(int x, int y, int z) {
        return Modules.enabled(BlockOutline.class) && BlockUtility.isTargetedBlock(x, y, z);
    }

    public static void renderTrajectories(Matrix4f matrix, double camX, double camY, double camZ) {
        try {
            if (!Modules.enabled(Trajectories.class)) return;
            Trajectories m = Modules.get(Trajectories.class);
            if (m == null || m.getPaths().isEmpty()) return;

            int lc = m.lineColor;
            float lr = ((lc >> 16) & 0xFF) / 255.0f;
            float lg = ((lc >> 8) & 0xFF) / 255.0f;
            float lb = (lc & 0xFF) / 255.0f;
            // line stays translucent over the world
            float la = ((lc >> 24) & 0xFF) / 255.0f * 0.6f;

            // marker uses the line color
            float mr = lr;
            float mg = lg;
            float mb = lb;
            float ma = la;

            float lw = (float) m.width;

            for (Trajectories.Path path : m.getPaths()) {
                if (!path.points.isEmpty() && path.hasStart) {
                    RELATIVE.clear();
                    // the line starts where the shot is spawned
                    RELATIVE.add(new Vector3f(
                            (float) (path.startX - camX),
                            (float) (path.startY - camY),
                            (float) (path.startZ - camZ)));
                    for (Vector3d v : path.points) {
                        RELATIVE.add(new Vector3f(
                                (float) (v.x - camX),
                                (float) (v.y - camY),
                                (float) (v.z - camZ)));
                    }
                    // the ray has to reach the impact point
                    if (path.hasLanding) {
                        RELATIVE.add(new Vector3f(
                                (float) (path.landingX - camX),
                                (float) (path.landingY - camY),
                                (float) (path.landingZ - camZ)));
                    }
                    Render3DUtility.batchLineStrip(matrix, RELATIVE, lr, lg, lb, la, lw, THROUGH_WALLS);
                }

                // highlight the whole landing block
                if (path.hasBlock && !outlinedByBlockOutline(path.blockX, path.blockY, path.blockZ)) {
                    REUSABLE_MATRIX.set(matrix);
                    REUSABLE_MATRIX.translate(
                            (float) (path.blockX - camX),
                            (float) (path.blockY - camY),
                            (float) (path.blockZ - camZ));
                    Render3DUtility.batchFilledBox(REUSABLE_MATRIX, 1.0, lr, lg, lb, la * 0.16f, THROUGH_WALLS);
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, 1.0, lr, lg, lb, la * 0.95f, lw, THROUGH_WALLS);
                }

                // highlight the entity on the path
                if (path.hitBox != null) {
                    var box = path.hitBox;
                    REUSABLE_MATRIX.set(matrix);
                    REUSABLE_MATRIX.translate(
                            (float) (box.minX - camX),
                            (float) (box.minY - camY),
                            (float) (box.minZ - camZ));
                    REUSABLE_MATRIX.scale((float) box.sizeX(), (float) box.sizeY(), (float) box.sizeZ());
                    Render3DUtility.batchFilledBox(REUSABLE_MATRIX, 1.0, lr, lg, lb, la * 0.2f, THROUGH_WALLS);
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, 1.0, lr, lg, lb, la * 0.9f, lw, THROUGH_WALLS);
                }

                if (path.hasLanding) {
                    // one small square flat on the hit face
                    Vector3d normal = new Vector3d(path.normalX, path.normalY, path.normalZ);
                    Vector3d right = perpendicular(normal);
                    Vector3d forward = new Vector3d(normal).cross(right);
                    double centerX = path.landingX + normal.x * 0.01;
                    double centerY = path.landingY + normal.y * 0.01;
                    double centerZ = path.landingZ + normal.z * 0.01;
                    float half = 0.125f;
                    float rightX = (float) (right.x * half);
                    float rightY = (float) (right.y * half);
                    float rightZ = (float) (right.z * half);
                    float forwardX = (float) (forward.x * half);
                    float forwardY = (float) (forward.y * half);
                    float forwardZ = (float) (forward.z * half);
                    float baseX = (float) (centerX - camX);
                    float baseY = (float) (centerY - camY);
                    float baseZ = (float) (centerZ - camZ);
                    Render3DUtility.batchFlatBandQuad(matrix,
                            baseX - rightX - forwardX, baseY - rightY - forwardY, baseZ - rightZ - forwardZ,
                            baseX + rightX - forwardX, baseY + rightY - forwardY, baseZ + rightZ - forwardZ,
                            baseX + rightX + forwardX, baseY + rightY + forwardY, baseZ + rightZ + forwardZ,
                            baseX - rightX + forwardX, baseY - rightY + forwardY, baseZ - rightZ + forwardZ,
                            mr, mg, mb, ma);
                }
            }
        } catch (Throwable t) {
            System.err.println("[RaveX] Trajectories render error: " + t.getMessage());
        }
    }
}
