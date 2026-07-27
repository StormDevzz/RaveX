package ravex.modules.world;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.parameter.StringParameter;
@Module(name = "AutoSign", category = "World")
public class AutoSign {
    @Parameter(name = "Line1")
    public String line1 = "RaveX";
    @Parameter(name = "Line2")
    public String line2 = "Client";
    @Parameter(name = "Line3")
    public String line3 = "OnTop";
    @Parameter(name = "Line4")
    public String line4 = "";





}