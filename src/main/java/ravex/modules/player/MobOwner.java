package ravex.modules.player;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.utility.misc.MobUtility;
public class MobOwner extends Module {
=======
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
public class MobOwner extends Module {
    public static final MobOwner INSTANCE = new MobOwner();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final BooleanParameter animals = new BooleanParameter("Animals", true);
    public final BooleanParameter displayUUID = new BooleanParameter("ShowUUID", false);
    public final BooleanParameter background = new BooleanParameter("Background", false);
    public final ColorParameter textColor = new ColorParameter("TextColor", 0xFFFFAA00);

<<<<<<< HEAD
    public static String getOwnerName(net.minecraft.world.entity.LivingEntity entity) {
        return MobUtility.getOwnerName(entity, ModuleManager.get(MobOwner.class).displayUUID.getValue());
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(MobOwner.class);
    }
    public static MobOwner itz() {
        return ModuleManager.get(MobOwner.class);
    }

}
=======
    private static String resolveName(java.util.UUID uuid) {
        if (uuid == null) return null;
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            Object connection = mc.getConnection();
            if (connection != null) {
                java.lang.reflect.Method getPlayerInfo = connection.getClass().getMethod("getPlayerInfo", java.util.UUID.class);
                Object info = getPlayerInfo.invoke(connection, uuid);
                if (info != null) {
                    java.lang.reflect.Method getProfile = info.getClass().getMethod("getProfile");
                    Object profile = getProfile.invoke(info);
                    if (profile != null) {
                        java.lang.reflect.Method getName = profile.getClass().getMethod("getName");
                        return (String) getName.invoke(profile);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
    public static String getOwnerName(LivingEntity entity) {
        if (!(entity instanceof OwnableEntity owned)) return null;
        java.util.UUID uuid = null;
        try {
            for (java.lang.reflect.Method m : owned.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType() == java.util.UUID.class) {
                    uuid = (java.util.UUID) m.invoke(owned);
                    if (uuid != null) break;
                }
            }
        } catch (Exception ignored) {}
        if (uuid == null) {
            try {
                net.minecraft.world.entity.Entity owner = owned.getOwner();
                if (owner != null) uuid = owner.getUUID();
            } catch (Exception ignored) {}
        }
        if (uuid == null) return null;
        if (INSTANCE.displayUUID.getValue()) {
            return uuid.toString();
        }
        try {
            net.minecraft.world.entity.Entity owner = owned.getOwner();
            if (owner != null) {
                return owner.getScoreboardName();
            }
        } catch (Exception ignored) {}
        String resolved = resolveName(uuid);
        if (resolved != null) {
            return resolved;
        }
        return uuid.toString();
    }
}
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
