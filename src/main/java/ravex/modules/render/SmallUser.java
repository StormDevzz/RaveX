package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import ravex.utility.misc.MobUtility;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@ModuleInfo(name = "SmallUser", category = "Render")
public class SmallUser extends ravex.modules.Module {
public final ModeParameter target = new ModeParameter("Target", "All", java.util.List.of("All", "Others", "Self"));
    public final NumberParameter scale = new NumberParameter("Scale", 0.5, 0.2, 1.0, 0.05);
    public final Map<Object, Float> stateScaleMap = new ConcurrentHashMap<>();

    public boolean shouldScale(Player player) {
        if (!getEnabled()) return false;
        Minecraft mc = Minecraft.getInstance();
        boolean isSelf = MobUtility.isSelf(player);
        String t = target.getValue();
        if (t.equals("Self")) {
            return isSelf;
        } else if (t.equals("Others")) {
            return !isSelf;
        } else {
            return true;
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("SmallUser").getEnabled();
    }

    public static SmallUser itz() {
        return ravex.manager.ModuleManager.delegate(SmallUser.class);
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