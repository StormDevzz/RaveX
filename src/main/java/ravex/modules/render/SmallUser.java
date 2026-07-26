package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.EntityUtility;

import ravex.utility.misc.MobUtility;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@ModuleInfo(name = "SmallUser", category = "Render")
public class SmallUser implements ModuleAccess {
    @Parameter(name = "Target", modes = {"All", "Others", "Self"})
    public String target = "All";
    @Parameter(name = "Scale", min = 0.2, max = 1.0, step = 0.05)
    public double scale = 0.5;
    public final Map<Object, Float> stateScaleMap = new ConcurrentHashMap<>();

    public boolean shouldScale(net.minecraft.world.entity.player.Player player) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("SmallUser").getEnabled()) return false;
        Minecraft mc = Minecraft.getInstance();
        boolean isSelf = MobUtility.isSelf(player);
        String t = target;
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


}