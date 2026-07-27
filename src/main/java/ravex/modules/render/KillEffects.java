package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;

import ravex.utility.misc.EntityUtility;
import ravex.event.Subscribe;
import ravex.event.player.DeathEvent;
import ravex.utility.misc.MobUtility;
import net.minecraft.core.particles.ParticleTypes;

import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "KillEffects", category = "Render")
public class KillEffects implements ModuleAccess {
    @Parameter(name = "Effect", modes = {"Lightning", "Fire", "Both"})
    public String effect = "Lightning";
    @Parameter(name = "Players")
    public boolean players = true;
    @Parameter(name = "Monsters")
    public boolean monsters = false;
    @Parameter(name = "Animals")
    public boolean animals = false;

    @Subscribe
    public void onDeath(DeathEvent event) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("KillEffects").getEnabled()) return;
        net.minecraft.world.entity.player.Player victim = event.getPlayer();
        if (victim == MinecraftWrapper.getInstance().player) return;
        net.minecraft.world.entity.LivingEntity living = victim;
        if (!shouldAffect(living)) return;
        ClientLevel level = (ClientLevel) living.level();
        if (level == null) return;
        String eff = effect;
        if (eff.equals("Lightning") || eff.equals("Both")) {
            spawnLightning(level, living.getX(), living.getY(), living.getZ());
        }
        if (eff.equals("Fire") || eff.equals("Both")) {
            spawnFireParticles(level, living.getX(), living.getY(), living.getZ());
        }
    }

    private boolean shouldAffect(net.minecraft.world.entity.LivingEntity e) {
        if (MobUtility.isPlayer(e) && players) return true;
        if (MobUtility.isHostile(e) && monsters) return true;
        if (MobUtility.isPassive(e) && animals) return true;
        return false;
    }
    private void spawnLightning(ClientLevel level, double x, double y, double z) {
        LightningBolt bolt = new LightningBolt(
            net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, level);
        bolt.setPos(x, y, z);
        bolt.setVisualOnly(true);
        level.addEntity(bolt);
        level.playLocalSound(x, y, z,
            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
            10000.0F, 0.8F + level.random.nextFloat() * 0.2F, false);
        level.playLocalSound(x, y, z,
            SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER,
            2.0F, 0.5F + level.random.nextFloat() * 0.2F, false);
    }
    private void spawnFireParticles(ClientLevel level, double x, double y, double z) {
        for (int i = 0; i < 20; i++) {
            double dx = (level.random.nextDouble() - 0.5) * 2.0;
            double dy = level.random.nextDouble() * 2.0;
            double dz = (level.random.nextDouble() - 0.5) * 2.0;
            level.addParticle(ParticleTypes.FLAME,
                x + dx, y + dy, z + dz,
                dx * 0.1, dy * 0.1, dz * 0.1);
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("KillEffects").getEnabled();
    }

    public static KillEffects itz() {
        return ravex.manager.ModuleManager.delegate(KillEffects.class);
    }


}