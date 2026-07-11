package ravex.modules.movement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.item.ItemEntity;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.misc.MobUtility;
public class NoPush extends Module {
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter mobs = new BooleanParameter("Mobs", true);
    public final BooleanParameter items = new BooleanParameter("Items", true);
    public final BooleanParameter water = new BooleanParameter("Water", false);

    public boolean shouldCancelPush(net.minecraft.world.entity.Entity self, net.minecraft.world.entity.Entity other) {
        if (!getEnabled()) return false;
        boolean otherPlayer = MobUtility.isPlayer(MobUtility.asLivingEntity(other));
        boolean otherMob = other instanceof LivingEntity && !otherPlayer;
        boolean otherItem = other instanceof ItemEntity;
        return (otherPlayer && players.getValue()) || (otherMob && mobs.getValue()) || (otherItem && items.getValue());
    }
    public boolean shouldCancelPush() {
        return getEnabled();
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(NoPush.class);
    }
    public static NoPush itz() {
        return ModuleManager.get(NoPush.class);
    }
}
