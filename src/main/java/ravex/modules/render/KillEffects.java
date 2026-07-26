package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ravex.event.Subscribe;
import ravex.event.player.DeathEvent;
import ravex.utility.misc.MobUtility;
import net.minecraft.core.particles.ParticleTypes;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import java.util.List;
@ModuleInfo(name = "KillEffects", category = "Render")
public class KillEffects extends ravex.modules.Module {
public final ModeParameter effect = new ModeParameter("Effect", "Lightning",
        List.of("Lightning", "Fire", "Both"));
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter monsters = new BooleanParameter("Monsters", false);
    public final BooleanParameter animals = new BooleanParameter("Animals", false);

    @Subscribe
    public void onDeath(DeathEvent event) {
        if (!getEnabled()) return;
        Player victim = event.getPlayer();
        if (victim == Minecraft.getInstance().player) return;
        LivingEntity living = victim;
        if (!shouldAffect(living)) return;
        ClientLevel level = (ClientLevel) living.level();
        if (level == null) return;
        String eff = effect.getValue();
        if (eff.equals("Lightning") || eff.equals("Both")) {
            spawnLightning(level, living.getX(), living.getY(), living.getZ());
        }
        if (eff.equals("Fire") || eff.equals("Both")) {
            spawnFireParticles(level, living.getX(), living.getY(), living.getZ());
        }
    }

    private boolean shouldAffect(LivingEntity e) {
        if (MobUtility.isPlayer(e) && players.getValue()) return true;
        if (MobUtility.isHostile(e) && monsters.getValue()) return true;
        if (MobUtility.isPassive(e) && animals.getValue()) return true;
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

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}