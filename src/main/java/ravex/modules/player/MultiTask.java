package ravex.modules.player;
<<<<<<< HEAD
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
=======
import ravex.modules.Category;
import ravex.modules.Module;
public class MultiTask extends Module {
    public static final MultiTask INSTANCE = new MultiTask();

}
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
