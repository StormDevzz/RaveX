package ravex.modules.render;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import java.util.List;
public class Weather extends Module {
    public static final Weather INSTANCE = new Weather();
    public final ModeParameter mode = new ModeParameter("Mode", "Rain", List.of("Clear", "Rain", "Snow", "Thunder"));

}
