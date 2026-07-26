package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import java.util.List;
@ModuleInfo(name = "Weather", category = "Render")
public class Weather implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Clear", "Rain", "Snow", "Thunder"})
    public String mode = "Rain";

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Weather").getEnabled();
    }

    public static Weather itz() {
        return ravex.manager.ModuleManager.delegate(Weather.class);
    }


}