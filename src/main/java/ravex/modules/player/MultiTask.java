package ravex.modules.player;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
public class MultiTask extends Module {

    public static boolean maybeEnabled() {
        return maybeEnabled(MultiTask.class);
    }
    public static MultiTask itz() {
        return ModuleManager.get(MultiTask.class);
    }

}