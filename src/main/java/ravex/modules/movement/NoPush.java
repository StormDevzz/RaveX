package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;

import ravex.utility.misc.EntityUtility;
import ravex.modules.Modules;
@Module(name = "NoPush", category = "Movement")
public class NoPush {
    @Parameter(name = "Players")
    public boolean players = true;
    @Parameter(name = "Mobs")
    public boolean mobs = true;
    @Parameter(name = "Items")
    public boolean items = true;
    @Parameter(name = "Liquid")
    public boolean liquid = false;

    public boolean shouldCancelPush(net.minecraft.world.entity.Entity self, net.minecraft.world.entity.Entity other) {
        if (!Modules.enabled(NoPush.class)) return false;
        boolean otherPlayer = EntityUtility.isPlayer(EntityUtility.asLivingEntity(other));
        boolean otherMob = EntityUtility.asLivingEntity(other) != null && !otherPlayer;
        boolean otherItem = EntityUtility.isItem(other);
        return (otherPlayer && players) || (otherMob && mobs) || (otherItem && items);
    }
    public boolean shouldCancelPush() {
        return Modules.enabled(NoPush.class);
    }




}