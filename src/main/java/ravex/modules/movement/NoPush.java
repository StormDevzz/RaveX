package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.item.ItemEntity;

import ravex.utility.misc.MobUtility;
@ModuleInfo(name = "NoPush", category = "Movement")
public class NoPush implements ModuleAccess {
    @Parameter(name = "Players")
    public boolean players = true;
    @Parameter(name = "Mobs")
    public boolean mobs = true;
    @Parameter(name = "Items")
    public boolean items = true;
    @Parameter(name = "Water")
    public boolean water = false;

    public boolean shouldCancelPush(net.minecraft.world.entity.Entity self, net.minecraft.world.entity.Entity other) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("NoPush").getEnabled()) return false;
        boolean otherPlayer = MobUtility.isPlayer(MobUtility.asLivingEntity(other));
        boolean otherMob = other instanceof net.minecraft.world.entity.LivingEntity && !otherPlayer;
        boolean otherItem = other instanceof ItemEntity;
        return (otherPlayer && players) || (otherMob && mobs) || (otherItem && items);
    }
    public boolean shouldCancelPush() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoPush").getEnabled();
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoPush").getEnabled();
    }
    public static NoPush itz() {
        return ravex.manager.ModuleManager.delegate(NoPush.class);
    }


}