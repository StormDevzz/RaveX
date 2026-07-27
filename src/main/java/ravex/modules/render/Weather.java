package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.List;
@Module(name = "Weather", category = "Render")
public class Weather {
    @Parameter(name = "Mode", modes = {"Clear", "Rain", "Snow", "Thunder"})
    public String mode = "Rain";






}