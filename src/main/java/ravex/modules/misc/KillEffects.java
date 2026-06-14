package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class KillEffects extends Module {
    public static final KillEffects INSTANCE = new KillEffects();

    public final ModeParameter effect = new ModeParameter("Effect", "Lightning",
        List.of("Lightning", "Fire", "Both"));
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter monsters = new BooleanParameter("Monsters", false);
    public final BooleanParameter animals = new BooleanParameter("Animals", false);

    private final Set<Entity> processed = new HashSet<>();

    private KillEffects() {
        super("KillEffects", Category.MISC);
        addParameter(effect);
        addParameter(players);
        addParameter(monsters);
        addParameter(animals);
    }

    @Override
    protected void onEnable() {
        processed.clear();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        String eff = effect.getValue();

        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isDeadOrDying()) continue;
            if (processed.contains(e)) continue;
            if (!shouldAffect(living)) continue;

            processed.add(e);

            if (eff.equals("Lightning") || eff.equals("Both")) {
                spawnLightning(level, living.getX(), living.getY(), living.getZ());
            }
            if (eff.equals("Fire") || eff.equals("Both")) {
                spawnFireParticles(level, living.getX(), living.getY(), living.getZ());
            }
        }

        processed.removeIf(e -> !e.isAlive());
    }

    private boolean shouldAffect(LivingEntity e) {
        if (e instanceof Player && players.getValue()) return true;
        if (e instanceof Monster && monsters.getValue()) return true;
        if (e instanceof Animal && animals.getValue()) return true;
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
}
