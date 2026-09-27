package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.event.Subscribe;
import ravex.event.EventBusHolder;
import ravex.event.combat.AttackEvent;
import ravex.event.combat.TotemPopEvent;
import ravex.utility.misc.CombatUtility;
import ravex.utility.player.InventoryUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.utility.client.ClientAlertUtility;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
@Module(name = "FakePlayer", category = "World")
public class FakePlayer {
    @Parameter(name = "Nickname")
    public String nickname = "Hausemaster";
    @Parameter(name = "CopyInv")
    public boolean copyInventory = true;
    @Parameter(name = "AutoTotem")
    public boolean autoTotem = false;
    @Parameter(name = "Respawn")
    public boolean respawn = true;
    private static RemotePlayer fake = null;
    private int deathTime = 0;
    public static boolean isFake(Entity entity) {
        return fake != null && entity == fake;
    }
    public static void onExplosion(Vec3 center) {
        if (fake == null || !Modules.enabled(FakePlayer.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null || mc.getPlayer() == null) return;
        mc.execute(() -> {
            if (fake == null || !Modules.enabled(FakePlayer.class)) return;
            if (fake.isDeadOrDying() || fake.hurtTime > 0) return;
            float dmg = crystalDamage(mc.getLevel(), fake, center);
            if (dmg <= 0f) return;
            damageFake(dmg);
        });
    }
    public void onEnable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) {
            Modules.setEnabled(FakePlayer.class, false);
            return;
        }
        deathTime = 0;
        spawnAt(mc.getPlayer().getX(), mc.getPlayer().getY(), mc.getPlayer().getZ(),
            mc.getPlayer().getYRot(), mc.getPlayer().getXRot(), mc.getPlayer().yHeadRot);
    }
    private void spawnAt(double x, double y, double z, float yaw, float pitch, float headYaw) {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        GameProfile profile = new GameProfile(UUID.fromString("c0ffeed0-dec0-4ba5-babe-0123456789ab"), nickname);
        fake = new RemotePlayer(mc.getLevel(), profile);
        fake.setPos(x, y, z);
        fake.setYRot(yaw);
        fake.setXRot(pitch);
        fake.yHeadRot = headYaw;
        fake.setId(-9999);
        if (copyInventory) {
            for (int i = 0; i < InventoryUtility.getContainerSize(mc.getPlayer()); i++) {
                fake.getInventory().setItem(i, InventoryUtility.getItem(mc.getPlayer(), i).copy());
            }
        }
        fake.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 9999, 1));
        mc.getLevel().addEntity(fake);
        deathTime = 0;
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() != null && fake != null) {
            mc.getLevel().removeEntity(fake.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        fake = null;
        deathTime = 0;
    }
    public void onTick() {
        if (fake == null) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null) return;
        if (autoTotem && !InventoryUtility.isTotem(fake.getOffhandItem()))
            fake.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        if (fake.isDeadOrDying()) {
            deathTime++;
            if (deathTime > 10) Modules.setEnabled(FakePlayer.class, false);
        } else {
            deathTime = 0;
        }
    }
    @Subscribe
    public void onAttack(AttackEvent event) {
        if (event.getType() != AttackEvent.AttackType.PRE) return;
        if (!applyFakeHit(event.getTarget())) return;
        event.setCancelled(true);
    }
    public static boolean applyFakeHit(Entity target) {
        if (fake == null || target != fake) return false;
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return false;
        if (fake.isDeadOrDying()) return true;
        ClientLevel level = mc.getLevel();
        level.playLocalSound(fake.getX(), fake.getY(), fake.getZ(),
            SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1f, 1f, false);
        boolean crit = !player.onGround() && player.fallDistance > 0f;
        float charge = player.getAttackStrengthScale(0.5f);
        float dmg;
        if (charge >= 0.85f) {
            dmg = (float) CombatUtility.getWeaponDamage(player.getMainHandItem());
            if (dmg <= 0f) dmg = 1f;
            if (crit) {
                dmg *= 1.5f;
                level.playLocalSound(fake.getX(), fake.getY(), fake.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 1f, false);
                player.crit(fake);
            }
        } else {
            dmg = 1f;
        }
        damageFake(dmg);
        return true;
    }
    private static void damageFake(float dmg) {
        if (fake == null) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null) return;
        fake.hurtTime = 10;
        fake.hurtDuration = 10;
        float hp = fake.getHealth() + fake.getAbsorptionAmount() - dmg;
        if (hp > 0f) {
            fake.setHealth(hp);
            return;
        }
        if (consumeTotem()) {
            fake.setHealth(10f);
            ClientLevel level = mc.getLevel();
            level.playLocalSound(fake.getX(), fake.getY() + 1.0, fake.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1f, 1f, false);
            java.util.concurrent.ThreadLocalRandom rnd = java.util.concurrent.ThreadLocalRandom.current();
            for (int i = 0; i < 30; i++) {
                double dx = (rnd.nextDouble() - 0.5) * 2.0;
                double dy = rnd.nextDouble() * 2.0;
                double dz = (rnd.nextDouble() - 0.5) * 2.0;
                level.addParticle(ParticleTypes.TOTEM_OF_UNDYING,
                    fake.getX() + dx, fake.getY() + 1.0 + dy, fake.getZ() + dz,
                    dx * 0.2, dy * 0.2, dz * 0.2);
            }
            EventBusHolder.get().post(new TotemPopEvent(fake, countTotems()));
        } else {
            fake.setHealth(0f);
            FakePlayer inst = Modules.get(FakePlayer.class);
            boolean re = inst != null && inst.respawn;
            ClientAlertUtility.alert(re ? ravex.utility.misc.LanguageUtility.t("fakeplayer_died_respawn") : ravex.utility.misc.LanguageUtility.t("fakeplayer_died"), 0xFFFF5555);
            if (re && inst != null) {
                double x = fake.getX(), y = fake.getY(), z = fake.getZ();
                float yaw = fake.getYRot(), pitch = fake.getXRot(), head = fake.yHeadRot;
                mc.getLevel().removeEntity(fake.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                fake = null;
                inst.spawnAt(x, y, z, yaw, pitch, head);
            }
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
    private static int countTotems() {
        if (fake == null) return 0;
        int n = 0;
        if (InventoryUtility.isTotem(fake.getOffhandItem())) n += fake.getOffhandItem().getCount();
        if (InventoryUtility.isTotem(fake.getMainHandItem())) n += fake.getMainHandItem().getCount();
        return n;
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
                    double px = net.minecraft.util.Mth.lerp(fx, box.minX, box.maxX) + offX;
                    double py = net.minecraft.util.Mth.lerp(fy, box.minY, box.maxY);
                    double pz = net.minecraft.util.Mth.lerp(fz, box.minZ, box.maxZ) + offZ;
                    Vec3 point = new Vec3(px, py, pz);
                    if (level.clip(new ClipContext(point, center, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, target)).getType() == HitResult.Type.MISS) hits++;
                    total++;
                }
            }
        }
        return total == 0 ? 0.0 : (double) hits / (double) total;
    }
}
