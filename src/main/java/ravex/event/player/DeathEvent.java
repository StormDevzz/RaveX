package ravex.event.player;

import ravex.event.Event;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

public class DeathEvent implements Event {
    private final Player player;
    private final DamageSource source;
    private final boolean self;

    public DeathEvent(Player player, DamageSource source) {
        this.player = player;
        this.source = source;
        this.self = player == net.minecraft.client.Minecraft.getInstance().player;
    }

    public Player getPlayer() { return player; }
    public DamageSource getSource() { return source; }
    public boolean isSelf() { return self; }
}
