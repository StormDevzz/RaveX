package ravex.modules.movement;
<<<<<<< HEAD
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.item.ItemEntity;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.misc.MobUtility;
public class NoPush extends Module {
=======
import net.minecraft.world.entity.player.Player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
public class NoPush extends Module {
    public static final NoPush INSTANCE = new NoPush();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter mobs = new BooleanParameter("Mobs", true);
    public final BooleanParameter items = new BooleanParameter("Items", true);
    public final BooleanParameter water = new BooleanParameter("Water", false);

    public boolean shouldCancelPush(net.minecraft.world.entity.Entity self, net.minecraft.world.entity.Entity other) {
        if (!getEnabled()) return false;
<<<<<<< HEAD
        boolean otherPlayer = MobUtility.isPlayer(MobUtility.asLivingEntity(other));
        boolean otherMob = other instanceof LivingEntity && !otherPlayer;
        boolean otherItem = other instanceof ItemEntity;
=======
        boolean otherPlayer = other instanceof Player;
        boolean otherMob = other instanceof net.minecraft.world.entity.monster.Monster;
        boolean otherItem = other instanceof net.minecraft.world.entity.item.ItemEntity;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
