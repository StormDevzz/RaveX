package ravex.event.combat;

import ravex.event.Event;
import net.minecraft.world.entity.player.Player;

public class TotemPopEvent implements Event {
    private final Player player;
    private final int totemsRemaining;

    public TotemPopEvent(Player player) { this(player, 0); }
    public TotemPopEvent(Player player, int totemsRemaining) {
        this.player = player;
        this.totemsRemaining = totemsRemaining;
    }

    public Player getPlayer() { return player; }
    public int getTotemsRemaining() { return totemsRemaining; }
}
