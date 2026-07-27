package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.item.ItemEntity;

import ravex.utility.misc.MobUtility;
import ravex.modules.Modules;
@Module(name = "NoPush", category = "Movement")
public class NoPush {
    @Parameter(name = "Players")
    public boolean players = true;
    @Parameter(name = "Mobs")
    public boolean mobs = true;
    @Parameter(name = "Items")
    public boolean items = true;
    @Parameter(name = "Water")
    public boolean water = false;

    public boolean shouldCancelPush(net.minecraft.world.entity.Entity self, net.minecraft.world.entity.Entity other) {
        if (!Modules.enabled(NoPush.class)) return false;
        boolean otherPlayer = MobUtility.isPlayer(MobUtility.asLivingEntity(other));
        boolean otherMob = other instanceof net.minecraft.world.entity.LivingEntity && !otherPlayer;
        boolean otherItem = other instanceof ItemEntity;
        return (otherPlayer && players) || (otherMob && mobs) || (otherItem && items);
    }
    public boolean shouldCancelPush() {
        return Modules.enabled(NoPush.class);
    }




}