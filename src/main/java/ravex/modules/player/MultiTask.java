package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.Modules;
@Module(name = "MultiTask", category = "net.minecraft.world.entity.player.Player")
public class MultiTask {
public static boolean maybeEnabled() {
        return Modules.enabled(MultiTask.class);
    }



}