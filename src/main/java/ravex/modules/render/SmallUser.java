package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "SmallUser", category = "Render")
public class SmallUser {
    @Parameter(name = "Target", modes = {"All", "Others", "Self"})
    public String target = "All";
    @Parameter(name = "Scale", min = 0.2, max = 1.0, step = 0.05)
    public double scale = 0.5;
    public final Map<Object, Float> stateScaleMap = new ConcurrentHashMap<>();

    public boolean shouldScale(net.minecraft.world.entity.player.Player player) {
        if (!Modules.enabled(SmallUser.class)) return false;
        boolean isSelf = EntityUtility.isSelf(player);
        String t = target;
        if (t.equals("Self")) {
            return isSelf;
        } else if (t.equals("Others")) {
            return !isSelf;
        } else {
            return true;
        }
    }
}
