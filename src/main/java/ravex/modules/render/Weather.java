package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import java.util.List;
public class Weather extends Module {
<<<<<<< HEAD
    public final ModeParameter mode = new ModeParameter("Mode", "Rain", List.of("Clear", "Rain", "Snow", "Thunder"));

    public static boolean maybeEnabled() {
        return maybeEnabled(Weather.class);
    }

    public static Weather itz() {
        return ModuleManager.get(Weather.class);
    }
=======
    public static final Weather INSTANCE = new Weather();
    public final ModeParameter mode = new ModeParameter("Mode", "Rain", List.of("Clear", "Rain", "Snow", "Thunder"));

>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
