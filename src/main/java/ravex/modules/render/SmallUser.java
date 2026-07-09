package ravex.modules.render;
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import ravex.modules.Module;
import ravex.utility.misc.MobUtility;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class SmallUser extends Module {
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
        return maybeEnabled(SmallUser.class);
    }

    public static SmallUser itz() {
        return ModuleManager.get(SmallUser.class);
    }
}
