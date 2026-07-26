package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.item.ItemEntity;

import ravex.parameter.BooleanParameter;
import ravex.utility.misc.MobUtility;
@ModuleInfo(name = "NoPush", category = "Movement")
public class NoPush extends ravex.modules.Module {
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
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoPush").getEnabled();
    }
    public static NoPush itz() {
        return ravex.manager.ModuleManager.delegate(NoPush.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}