package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "ShiftInterp", category = "Render")
public class ShiftInterp {
    @Parameter(name = "Players")
    public boolean players = false;
    @Parameter(name = "Self")
    public boolean self = true;

    public boolean shouldCrouch(net.minecraft.world.entity.Entity entity) {
        if (!Modules.enabled(ShiftInterp.class)) return false;
        if (!(entity instanceof net.minecraft.world.entity.player.Player)) return false;
        var mc = MinecraftWrapper.getWrapper();
        boolean isSelf = (entity == mc.getPlayer());
        return isSelf ? self : players;
    }
}
