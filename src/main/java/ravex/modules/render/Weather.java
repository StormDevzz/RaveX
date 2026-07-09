package ravex.modules.render;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import java.util.List;
public class Weather extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Rain", List.of("Clear", "Rain", "Snow", "Thunder"));

    public static boolean maybeEnabled() {
        return maybeEnabled(Weather.class);
    }

    public static Weather itz() {
        return ModuleManager.get(Weather.class);
    }
}
