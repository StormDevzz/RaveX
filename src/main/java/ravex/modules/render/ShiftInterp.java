package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "ShiftInterp", category = "Render")
public class ShiftInterp {
    @Parameter(name = "Target", modes = {"Players", "Self"})
    public String target = "Players";

    public boolean shouldCrouch(net.minecraft.world.entity.Entity entity) {
        if (!Modules.enabled(ShiftInterp.class)) return false;
        if (!(entity instanceof net.minecraft.world.entity.player.Player)) return false;
        var mc = MinecraftWrapper.getWrapper();
        boolean isSelf = (entity == mc.getPlayer());
        if (target.equals("Self")) return isSelf;
        return true;
    }





}