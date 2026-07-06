package ravex.modules.movement;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.misc.MobUtility;
public class NoPush extends Module {
    public static final NoPush INSTANCE = new NoPush();
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter mobs = new BooleanParameter("Mobs", true);
    public final BooleanParameter items = new BooleanParameter("Items", true);
    public final BooleanParameter water = new BooleanParameter("Water", false);

    public boolean shouldCancelPush(net.minecraft.world.entity.Entity self, net.minecraft.world.entity.Entity other) {
        if (!getEnabled()) return false;
        boolean otherPlayer = MobUtility.isPlayer(MobUtility.asLivingEntity(other));
        boolean otherMob = other instanceof net.minecraft.world.entity.monster.Monster;
        boolean otherItem = other instanceof net.minecraft.world.entity.item.ItemEntity;
        return (otherPlayer && players.getValue()) || (otherMob && mobs.getValue()) || (otherItem && items.getValue());
    }
    public boolean shouldCancelPush() {
        return getEnabled();
    }
}
