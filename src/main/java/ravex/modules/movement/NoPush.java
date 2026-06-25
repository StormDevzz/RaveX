package ravex.modules.movement;

import net.minecraft.world.entity.player.Player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;

public class NoPush extends Module {
    public static final NoPush INSTANCE = new NoPush();

    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter mobs = new BooleanParameter("Mobs", true);
    public final BooleanParameter items = new BooleanParameter("Items", true);
    public final BooleanParameter water = new BooleanParameter("Water", false);

    private NoPush() {
        super("NoPush", Category.MOVEMENT);
        addParameter(players);
        addParameter(mobs);
        addParameter(items);
        addParameter(water);
    }

    public boolean shouldCancelPush(net.minecraft.world.entity.Entity self, net.minecraft.world.entity.Entity other) {
        if (!getEnabled()) return false;

        boolean otherPlayer = other instanceof Player;
        boolean otherMob = other instanceof net.minecraft.world.entity.monster.Monster;
        boolean otherItem = other instanceof net.minecraft.world.entity.item.ItemEntity;

        return (otherPlayer && players.getValue()) || (otherMob && mobs.getValue()) || (otherItem && items.getValue());
    }

    public boolean shouldCancelPush() {
        return getEnabled();
    }
}
