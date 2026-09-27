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
    @Parameter(name = "Target", modes = {"Players", "Self"})
    public String target = "Players";
    public final Map<Object, Float> stateScaleMap = new ConcurrentHashMap<>();

    public boolean shouldScale(net.minecraft.world.entity.player.Player player) {
        if (!Modules.enabled(SmallUser.class)) return false;
        boolean isSelf = EntityUtility.isSelf(player);
        if (target.equals("Self")) return isSelf;
        return true;
    }
}
