package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.event.Subscribe;
import ravex.event.EventBusHolder;
import ravex.event.combat.AttackEvent;
import ravex.event.combat.TotemPopEvent;
import ravex.event.player.DeathEvent;
import ravex.utility.misc.CombatUtility;
import ravex.utility.misc.FakePlayerUtility;
import ravex.utility.player.InventoryUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.utility.client.ClientAlertUtility;
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
    private static long lastHitMs = 0;
    private int deathTime = 0;
    public static boolean isFake(Object entity) {
        return FakePlayerUtility.isFake(entity);
    }
    public static void onExplosion(Object center) {
        if (!FakePlayerUtility.exists() || !Modules.enabled(FakePlayer.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null || mc.getPlayer() == null) return;
        mc.execute(() -> {
            if (!FakePlayerUtility.exists() || !Modules.enabled(FakePlayer.class)) return;
            if (FakePlayerUtility.isDeadOrDying() || FakePlayerUtility.isHurt()) return;
            float dmg = FakePlayerUtility.crystalDamage(center);
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
        FakePlayerUtility.spawn(x, y, z, yaw, pitch, headYaw, nickname, copyInventory);
        deathTime = 0;
    }
    public void onDisable() {
        FakePlayerUtility.despawn();
        deathTime = 0;
    }
    public void onTick() {
        if (!FakePlayerUtility.exists()) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null) return;
        if (autoTotem && !InventoryUtility.isTotem(FakePlayerUtility.getOffhand()))
            FakePlayerUtility.equipOffhandTotem();
        if (FakePlayerUtility.isDeadOrDying()) {
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
    public static boolean applyFakeHit(Object target) {
        if (!FakePlayerUtility.isFake(target)) return false;
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return false;
        if (FakePlayerUtility.isDeadOrDying()) return true;
        // one hit per window, several modules can swing
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastHitMs < 450) return true;
        lastHitMs = nowMs;
        FakePlayerUtility.playHurtSound();
        boolean crit = !player.onGround() && player.fallDistance > 0f;
        float charge = player.getAttackStrengthScale(0.5f);
        // vanilla charge curve, no charge means no hit
        if (charge < 0.2f) return true;
        float dmg = (float) CombatUtility.getWeaponDamage(player.getMainHandItem());
        if (dmg <= 0f) dmg = 1f;
        dmg *= 0.2f + charge * charge * 0.8f;
        boolean realCrit = charge >= 0.9f && crit;
        if (realCrit) {
            dmg *= 1.5f;
            FakePlayerUtility.playCritSound();
            FakePlayerUtility.crit(player);
        }
        FakePlayerUtility.knockback(player.getYRot());
        damageFake(dmg);
        return true;
    }
    private static void damageFake(float dmg) {
        if (!FakePlayerUtility.exists()) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null) return;
        FakePlayerUtility.HurtResult result = FakePlayerUtility.hurt(dmg);
        if (result == FakePlayerUtility.HurtResult.TOTEM_POP) {
            EventBusHolder.get().post(new TotemPopEvent(FakePlayerUtility.getFake(), FakePlayerUtility.countTotems()));
            return;
        }
        if (result != FakePlayerUtility.HurtResult.DEAD) return;
        // no vanilla die(), post the event manually
        EventBusHolder.get().post(new DeathEvent(FakePlayerUtility.getFake(), mc.getLevel().damageSources().generic()));
        // ragdoll now, respawn replaces it in this call
        ravex.modules.render.Animations.onVictimDied(FakePlayerUtility.getFake());
        FakePlayer inst = Modules.get(FakePlayer.class);
        boolean re = inst != null && inst.respawn;
        ClientAlertUtility.alert(re ? ravex.utility.misc.LanguageUtility.t("fakeplayer_died_respawn") : ravex.utility.misc.LanguageUtility.t("fakeplayer_died"), 0xFFFF5555);
        if (re && inst != null) {
            double x = FakePlayerUtility.getX(), y = FakePlayerUtility.getY(), z = FakePlayerUtility.getZ();
            float yaw = FakePlayerUtility.getYRot(), pitch = FakePlayerUtility.getXRot(), head = FakePlayerUtility.getHeadRot();
            FakePlayerUtility.despawn();
            inst.spawnAt(x, y, z, yaw, pitch, head);
        }
    }
}
