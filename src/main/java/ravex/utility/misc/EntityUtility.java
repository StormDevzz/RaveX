package ravex.utility.misc;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.player.PlayerUtility;
import ravex.utility.player.InventoryUtility;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class EntityUtility {
    public static boolean isHostile(LivingEntity entity) {
        return entity instanceof Monster || entity instanceof EnderDragon || entity instanceof WitherBoss;
    }

    public static boolean isPassive(LivingEntity entity) {
        return entity instanceof Animal || entity instanceof AbstractVillager || entity instanceof AmbientCreature;
    }

    public static boolean isPlayer(LivingEntity entity) {
        return entity instanceof Player;
    }

    public static boolean isSelf(LivingEntity entity) {
        return entity == PlayerUtility.getPlayer();
    }

    @Nullable
    public static LivingEntity asLivingEntity(Entity entity) {
        return entity instanceof LivingEntity living ? living : null;
    }

    public static boolean isAlive(LivingEntity entity) {
        return entity != null && entity.isAlive();
    }

    public static double getHealth(LivingEntity entity) {
        return entity.getHealth();
    }

    public static double getMaxHealth(LivingEntity entity) {
        return entity.getMaxHealth();
    }

    public static float getHealthPercent(LivingEntity entity) {
        return entity.getHealth() / entity.getMaxHealth();
    }

    public static double distanceTo(LivingEntity from, LivingEntity to) {
        return from.distanceTo(to);
    }

    public static double distanceToPlayer(LivingEntity entity) {
        var p = MinecraftWrapper.getWrapper().getPlayer();
        return p != null ? p.distanceTo(entity) : Double.MAX_VALUE;
    }

    public static boolean hasItem(LivingEntity entity, net.minecraft.world.item.Item item) {
        if (entity instanceof Player p) {
            for (int i = 0; i < 36; i++) {
                if (p.getInventory().getItem(i).is(item)) return true;
            }
        }
        return entity.getMainHandItem().is(item) || entity.getOffhandItem().is(item);
    }

    public static boolean isHoldingSword(LivingEntity entity) {
        String main = entity.getMainHandItem().getItem().toString().toLowerCase();
        String off = entity.getOffhandItem().getItem().toString().toLowerCase();
        return main.contains("sword") || off.contains("sword");
    }

    public static boolean isHoldingTool(LivingEntity entity) {
        String main = entity.getMainHandItem().getItem().toString().toLowerCase();
        return main.contains("pickaxe") || main.contains("axe") || main.contains("shovel") || main.contains("hoe");
    }

    public static boolean isHoldingTotem(LivingEntity entity) {
        return InventoryUtility.isTotem(entity.getOffhandItem())
            || InventoryUtility.isTotem(entity.getMainHandItem());
    }

    public static boolean isArmorStand(LivingEntity entity) {
        return entity instanceof ArmorStand;
    }

    public static boolean isSkeletonTarget(Entity entity) {
        LivingEntity living = asLivingEntity(entity);
        if (living == null || isDead(living) || isArmorStand(living)) return false;
        return isPlayer(living);
    }

    public static boolean isWearingArmor(LivingEntity entity) {
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.HUMANOID_ARMOR) {
                if (!entity.getItemBySlot(slot).isEmpty()) return true;
            }
        }
        return false;
    }

    public static boolean isMountable(Entity entity) {
        if (entity instanceof net.minecraft.world.entity.animal.equine.Horse) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.Donkey) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.Mule) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.SkeletonHorse) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.ZombieHorse) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.Llama) return true;
        if (entity instanceof net.minecraft.world.entity.animal.pig.Pig pig && pig.isSaddled()) return true;
        if (entity instanceof net.minecraft.world.entity.monster.Strider strider && strider.isSaddled()) return true;
        return false;
    }

    public static boolean isVehicle(Entity entity) {
        return entity != null && entity.isVehicle();
    }

    public static void interact(MinecraftWrapper mc, Entity target) {
        var _mc = mc.getRaw();
        if (_mc.player != null && _mc.gameMode != null) {
            _mc.gameMode.interact(_mc.player, target, net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    public static boolean isShearable(net.minecraft.world.entity.animal.sheep.Sheep sheep) {
        return sheep.isAlive() && !sheep.isBaby() && !sheep.isSheared();
    }

    public static boolean isAdultSheep(Entity entity) {
        return entity instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep && isShearable(sheep);
    }

    public static boolean isVillager(Entity entity) {
        return entity instanceof net.minecraft.world.entity.npc.villager.AbstractVillager v && v.isAlive() && !v.isBaby();
    }

    public static boolean isNameable(Entity entity) {
        return entity instanceof LivingEntity living && !(entity instanceof Player)
            && !(entity instanceof net.minecraft.world.entity.decoration.ArmorStand) && living.isAlive();
    }

    public static boolean hasName(Entity entity, String name) {
        var cn = entity.getCustomName();
        return cn != null && cn.getString().equals(name);
    }

    public static boolean isDead(LivingEntity entity) {
        return entity == null || entity.isDeadOrDying();
    }

    public static double getAbsorption(LivingEntity entity) {
        return entity.getAbsorptionAmount();
    }

    public static double getHealthWithAbsorption(LivingEntity entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    public static void attack(MinecraftWrapper mc, Entity target) {
        var _mc = mc.getRaw();
        if (_mc.player != null && _mc.gameMode != null) {
            _mc.gameMode.attack(_mc.player, target);
        }
    }

    public static void swingHand(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        if (_mc.player != null) {
            _mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    public static boolean isWolf(Object entity) {
        return entity instanceof net.minecraft.world.entity.animal.wolf.Wolf;
    }

    public static boolean isCat(Object entity) {
        return entity instanceof net.minecraft.world.entity.animal.feline.Cat;
    }

    public static boolean isLlama(Object entity) {
        return entity instanceof net.minecraft.world.entity.animal.equine.Llama;
    }

    public static boolean isMob(Entity entity) {
        return entity instanceof net.minecraft.world.entity.Mob;
    }

    public static double distanceToPlayer(Entity entity) {
        var p = MinecraftWrapper.getWrapper().getPlayer();
        return p != null ? p.distanceTo(entity) : Double.MAX_VALUE;
    }

    @Nullable
    public static String getOwnerName(Entity entity, boolean displayUuid) {
        if (!(entity instanceof net.minecraft.world.entity.OwnableEntity owned)) return null;
        java.util.UUID uuid = null;
        try {
            for (var m : owned.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType() == java.util.UUID.class) {
                    uuid = (java.util.UUID) m.invoke(owned);
                    if (uuid != null) break;
                }
            }
        } catch (Exception ignored) {}
        if (uuid == null) {
            try {
                var owner = owned.getOwner();
                if (owner != null) uuid = owner.getUUID();
            } catch (Exception ignored) {}
        }
        if (uuid == null) return null;
        if (displayUuid) return uuid.toString();
        try {
            var owner = owned.getOwner();
            if (owner != null) return owner.getScoreboardName();
        } catch (Exception ignored) {}
        try {
            var mc = MinecraftWrapper.getWrapper();
            var conn = mc.getConnection();
            if (conn != null) {
                var info = conn.getClass().getMethod("getPlayerInfo", java.util.UUID.class).invoke(conn, uuid);
                if (info != null) {
                    var profile = info.getClass().getMethod("getProfile").invoke(info);
                    if (profile != null)
                        return (String) profile.getClass().getMethod("getName").invoke(profile);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static final Map<Integer, double[]> smoothPositions = new HashMap<>();

    public static Vec3 smoothPos(Entity entity, Vec3 desired) {
        long now = System.nanoTime();
        double[] entry = smoothPositions.get(entity.getId());
        if (entry == null) {
            smoothPositions.put(entity.getId(), new double[]{desired.x, desired.y, desired.z, now});
            return desired;
        }
        double dt = (now - entry[3]) / 1.0E9;
        entry[3] = now;
        if (dt < 0.0005) dt = 0.0005;
        if (dt > 0.1) dt = 0.1;
        double alpha = 1.0 - Math.exp(-dt * 14.0);
        entry[0] += (desired.x - entry[0]) * alpha;
        entry[1] += (desired.y - entry[1]) * alpha;
        entry[2] += (desired.z - entry[2]) * alpha;
        if (smoothPositions.size() > 256) {
            var it = smoothPositions.entrySet().iterator();
            while (it.hasNext()) {
                if (now - it.next().getValue()[3] > 5.0E9) it.remove();
            }
        }
        return new Vec3(entry[0], entry[1], entry[2]);
    }

    public static boolean isItem(Entity entity) {
        return entity instanceof net.minecraft.world.entity.item.ItemEntity;
    }

    public static boolean isBoat(Entity entity) {
        return entity instanceof net.minecraft.world.entity.vehicle.boat.Boat;
    }

    public enum ProjectileKind {
        NONE, POTION, EXPERIENCE_BOTTLE, THROWABLE, TRIDENT, ARROW, FIREWORK, WIND_CHARGE, FIREBALL, GENERIC
    }

    // order matters, tridents are arrows and wind charges are hurting projectiles
    @NotNull
    public static ProjectileKind projectileKind(@Nullable Object entity) {
        if (!(entity instanceof Entity e)) return ProjectileKind.NONE;
        if (e instanceof AbstractThrownPotion) return ProjectileKind.POTION;
        if (e instanceof ThrownExperienceBottle) return ProjectileKind.EXPERIENCE_BOTTLE;
        if (e instanceof ThrowableProjectile) return ProjectileKind.THROWABLE;
        if (e instanceof ThrownTrident) return ProjectileKind.TRIDENT;
        if (e instanceof AbstractArrow) return ProjectileKind.ARROW;
        if (e instanceof FireworkRocketEntity) return ProjectileKind.FIREWORK;
        if (e instanceof AbstractWindCharge) return ProjectileKind.WIND_CHARGE;
        if (e instanceof AbstractHurtingProjectile) return ProjectileKind.FIREBALL;
        if (e instanceof Projectile) return ProjectileKind.GENERIC;
        return ProjectileKind.NONE;
    }

    @NotNull
    public static Vector3d position(@Nullable Object entity) {
        if (!(entity instanceof Entity e)) return new Vector3d();
        return new Vector3d(e.getX(), e.getY(), e.getZ());
    }

    @NotNull
    public static Vector3d eyePosition(@Nullable Object entity) {
        if (!(entity instanceof Entity e)) return new Vector3d();
        Vec3 eye = e.getEyePosition();
        return new Vector3d(eye.x, eye.y, eye.z);
    }

    @NotNull
    public static Vector3d deltaMovement(@Nullable Object entity) {
        if (!(entity instanceof Entity e)) return new Vector3d();
        Vec3 motion = e.getDeltaMovement();
        return new Vector3d(motion.x, motion.y, motion.z);
    }

    public static Iterable<Entity> entitiesForRendering() {
        var level = MinecraftWrapper.getWrapper().getLevel();
        return level != null ? level.entitiesForRendering() : List.of();
    }

    // axis aligned box that keeps minecraft types out of callers
    public static final class Box {
        public final double minX, minY, minZ, maxX, maxY, maxZ;

        public Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public double sizeX() { return maxX - minX; }
        public double sizeY() { return maxY - minY; }
        public double sizeZ() { return maxZ - minZ; }
    }

    public static final class ProjectileHit {
        public final double x, y, z;
        public final int segment;
        public final Box box;

        private ProjectileHit(double x, double y, double z, int segment, Box box) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.segment = segment;
            this.box = box;
        }
    }

    public static boolean canBeHitByProjectile(Entity entity, @Nullable Entity owner) {
        return entity != owner && entity.isAlive() && entity.isPickable() && entity.canBeHitByProjectile();
    }

    // first entity crossed by the simulated path, landing point included
    @Nullable
    public static ProjectileHit findProjectileHit(@Nullable Object owner, double startX, double startY, double startZ,
            List<Vector3d> points, boolean hasLanding, double landingX, double landingY, double landingZ,
            boolean inflateBox, double inflation) {
        var level = MinecraftWrapper.getWrapper().getLevel();
        if (level == null) return null;

        List<Vector3d> ends = new ArrayList<>(points.size() + 1);
        ends.addAll(points);
        if (hasLanding) ends.add(new Vector3d(landingX, landingY, landingZ));
        if (ends.isEmpty()) return null;

        AABB bounds = new AABB(startX, startY, startZ, startX, startY, startZ);
        Vec3 prev = new Vec3(startX, startY, startZ);
        for (Vector3d end : ends) {
            Vec3 cur = new Vec3(end.x, end.y, end.z);
            bounds = bounds.minmax(new AABB(prev, cur));
            prev = cur;
        }

        Entity ownerEntity = owner instanceof Entity e ? e : null;
        Entity bestEntity = null;
        Vec3 bestHit = null;
        int bestSegment = -1;
        double bestDistance = Double.MAX_VALUE;

        for (Entity entity : level.entitiesForRendering()) {
            if (!canBeHitByProjectile(entity, ownerEntity)) continue;
            // throwables hit 0.3 blocks early
            AABB box = inflateBox ? entity.getBoundingBox().inflate(inflation) : entity.getBoundingBox();
            if (!box.intersects(bounds)) continue;

            prev = new Vec3(startX, startY, startZ);
            double travelled = 0.0;
            for (int i = 0; i < ends.size(); i++) {
                Vec3 cur = new Vec3(ends.get(i).x, ends.get(i).y, ends.get(i).z);
                Optional<Vec3> hit = box.clip(prev, cur);
                if (hit.isPresent()) {
                    double distance = travelled + hit.get().distanceTo(prev);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestEntity = entity;
                        bestHit = hit.get();
                        bestSegment = i;
                    }
                    break;
                }
                travelled += prev.distanceTo(cur);
                prev = cur;
            }
        }

        if (bestEntity == null || bestHit == null) return null;
        AABB box = bestEntity.getBoundingBox();
        return new ProjectileHit(bestHit.x, bestHit.y, bestHit.z, bestSegment,
                new Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));
    }

}
