package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.Modules;
@Module(name = "MultiTask", category = "Player")
public class MultiTask {
public static boolean maybeEnabled() {
        return Modules.enabled(MultiTask.class);
    }



}