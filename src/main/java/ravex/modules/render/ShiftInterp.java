package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "ShiftInterp", category = "Render")
public class ShiftInterp {
    @Parameter(name = "Target", modes = {"All", "Others", "Self"})
    public String target = "All";

    public boolean shouldCrouch(net.minecraft.world.entity.Entity entity) {
        if (!Modules.enabled(ShiftInterp.class)) return false;
        if (!(entity instanceof net.minecraft.world.entity.player.Player)) return false;
        var mc = MinecraftWrapper.getInstance();
        boolean isSelf = (entity == mc.player);
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