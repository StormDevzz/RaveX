package ravex.utility.misc;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FakePlayerUtility {
    public enum HurtResult { ALIVE, TOTEM_POP, DEAD }
    private static RemotePlayer fake = null;
    public static boolean exists() {
        return fake != null;
    }
    public static boolean isFake(Object entity) {
        return fake != null && entity == fake;
    }
    public static RemotePlayer getFake() {
        return fake;
    }
    public static boolean isDeadOrDying() {
        return fake != null && fake.isDeadOrDying();
    }
    public static boolean isHurt() {
        return fake != null && fake.hurtTime > 0;
    }
    public static ItemStack getOffhand() {
        if (fake == null) return ItemStack.EMPTY;
        return fake.getOffhandItem();
    }
    public static double getX() {
        return fake != null ? fake.getX() : 0.0;
    }
    public static double getY() {
        return fake != null ? fake.getY() : 0.0;
    }
    public static double getZ() {
        return fake != null ? fake.getZ() : 0.0;
    }
    public static float getYRot() {
        return fake != null ? fake.getYRot() : 0f;
    }
    public static float getXRot() {
        return fake != null ? fake.getXRot() : 0f;
    }
    public static float getHeadRot() {
        return fake != null ? fake.yHeadRot : 0f;
    }
    public static void spawn(double x, double y, double z, float yaw, float pitch, float headYaw,
                             String nickname, boolean copyInventory) {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        GameProfile profile = new GameProfile(UUID.fromString("c0ffeed0-dec0-4ba5-babe-0123456789ab"), nickname);
        fake = new RemotePlayer(mc.getLevel(), profile);
        fake.setPos(x, y, z);
        fake.setYRot(yaw);
        fake.setXRot(pitch);
        fake.yHeadRot = headYaw;
        fake.setId(-9999);
        if (copyInventory) {
            for (int i = 0; i < InventoryUtility.getContainerSize(player); i++) {
                fake.getInventory().setItem(i, InventoryUtility.getItem(player, i).copy());
            }
        }
        fake.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 9999, 1));
        mc.getLevel().addEntity(fake);
    }
    public static void despawn() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() != null && fake != null) {
            mc.getLevel().removeEntity(fake.getId(), Entity.RemovalReason.DISCARDED);
        }
        fake = null;
    }
    public static void equipOffhandTotem() {
        if (fake == null) return;
        fake.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
    }
    // runs hurt time plus damage and totem resolution
    public static HurtResult hurt(float dmg) {
        if (fake == null) return HurtResult.ALIVE;
        fake.hurtTime = 10;
        fake.hurtDuration = 10;
        float hp = fake.getHealth() + fake.getAbsorptionAmount() - dmg;
        if (hp > 0f) {
            fake.setHealth(hp);
            return HurtResult.ALIVE;
        }
        if (consumeTotem()) {
            fake.setHealth(10f);
            playSound(SoundEvents.TOTEM_USE, 1.0);
            spawnTotemParticles();
            return HurtResult.TOTEM_POP;
        }
        fake.setHealth(0f);
        return HurtResult.DEAD;
    }
    public static void playHurtSound() {
        playSound(SoundEvents.PLAYER_HURT, 0.0);
    }
    public static void playCritSound() {
        playSound(SoundEvents.PLAYER_ATTACK_CRIT, 0.0);
    }
    public static void crit(LocalPlayer player) {
        if (fake != null) player.crit(fake);
    }
    public static void knockback(float playerYaw) {
        if (fake == null) return;
        double kyaw = Math.toRadians(playerYaw);
        fake.knockback(0.4f, Math.sin(kyaw), -Math.cos(kyaw));
    }
    public static int countTotems() {
        if (fake == null) return 0;
        int n = 0;
        if (InventoryUtility.isTotem(fake.getOffhandItem())) n += fake.getOffhandItem().getCount();
        if (InventoryUtility.isTotem(fake.getMainHandItem())) n += fake.getMainHandItem().getCount();
        return n;
    }
    public static float crystalDamage(Object center) {
        var mc = MinecraftWrapper.getWrapper();
        ClientLevel level = mc.getLevel();
        if (level == null || fake == null || !(center instanceof Vec3 point)) return 0f;
        return crystalDamage(level, fake, point);
    }
    private static void playSound(SoundEvent sound, double yOffset) {
        var mc = MinecraftWrapper.getWrapper();
        ClientLevel level = mc.getLevel();
        if (level == null || fake == null) return;
        level.playLocalSound(fake.getX(), fake.getY() + yOffset, fake.getZ(),
            sound, SoundSource.PLAYERS, 1f, 1f, false);
    }
    private static void spawnTotemParticles() {
        var mc = MinecraftWrapper.getWrapper();
        ClientLevel level = mc.getLevel();
        if (level == null || fake == null) return;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < 30; i++) {
            double dx = (rnd.nextDouble() - 0.5) * 2.0;
            double dy = rnd.nextDouble() * 2.0;
            double dz = (rnd.nextDouble() - 0.5) * 2.0;
            level.addParticle(ParticleTypes.TOTEM_OF_UNDYING,
                fake.getX() + dx, fake.getY() + 1.0 + dy, fake.getZ() + dz,
                dx * 0.2, dy * 0.2, dz * 0.2);
        }
    }
    private static boolean consumeTotem() {
        if (fake == null) return false;
        if (InventoryUtility.isTotem(fake.getOffhandItem())) {
            fake.getOffhandItem().shrink(1);
            return true;
        }
        if (InventoryUtility.isTotem(fake.getMainHandItem())) {
            fake.getMainHandItem().shrink(1);
            return true;
        }
        return false;
    }
    private static float crystalDamage(ClientLevel level, LivingEntity target, Vec3 center) {
        double dist = Math.sqrt(target.distanceToSqr(center));
        if (dist > 12.0) return 0f;
        double exposure = exposure(level, target, center);
        double impact = (1.0 - dist / 12.0) * exposure;
        float raw = (float) (((impact * impact + impact) / 2.0) * 7.0 * 6.0 + 1.0);
        float armor = (float) target.getArmorValue();
        float f = Math.max(armor - raw / 2f, armor * 0.2f);
        f = Math.min(f, 20f);
        float armored = raw * (1f - f / 25f);
        var resistance = target.getEffect(MobEffects.RESISTANCE);
        if (resistance != null) armored *= 1.0f - (resistance.getAmplifier() + 1) * 0.2f;
        switch (level.getDifficulty()) {
            case PEACEFUL -> { return 0f; }
            case EASY -> { return Math.max(Math.min(armored / 2f + 1f, armored), 0f); }
            case HARD -> { return Math.max(armored * 1.5f, 0f); }
            default -> { return Math.max(armored, 0f); }
        }
    }
    private static double exposure(ClientLevel level, LivingEntity target, Vec3 center) {
        AABB box = target.getBoundingBox();
        double stepX = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double stepY = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double stepZ = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double offX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
        double offZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;
        int hits = 0;
        int total = 0;
        for (double fx = 0.0; fx <= 1.0; fx += stepX) {
            for (double fy = 0.0; fy <= 1.0; fy += stepY) {
                for (double fz = 0.0; fz <= 1.0; fz += stepZ) {
                    double px = Mth.lerp(fx, box.minX, box.maxX) + offX;
                    double py = Mth.lerp(fy, box.minY, box.maxY);
                    double pz = Mth.lerp(fz, box.minZ, box.maxZ) + offZ;
                    Vec3 point = new Vec3(px, py, pz);
                    if (level.clip(new ClipContext(point, center, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, target)).getType() == HitResult.Type.MISS) hits++;
                    total++;
                }
            }
        }
        return total == 0 ? 0.0 : (double) hits / (double) total;
    }
}
