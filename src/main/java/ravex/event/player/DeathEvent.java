package ravex.event.player;

import ravex.event.Event;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ravex.mcwrapper.MinecraftWrapper;

public class DeathEvent implements Event {
    private final LivingEntity entity;
    private final DamageSource source;
    private final boolean self;

    public DeathEvent(LivingEntity entity, DamageSource source) {
        this.entity = entity;
        this.source = source;
        this.self = entity == MinecraftWrapper.getWrapper().getPlayer();
    }

    public LivingEntity getEntity() { return entity; }
    // null for non player victims.
    public Player getPlayer() { return entity instanceof Player player ? player : null; }
    public DamageSource getSource() { return source; }
    public boolean isSelf() { return self; }
}
