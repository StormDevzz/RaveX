package ravex.modules.world;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.parameter.StringParameter;
@ModuleInfo(name = "AutoSign", category = "World")
public class AutoSign implements ModuleAccess {
    @Parameter(name = "Line1")
    public String line1 = "RaveX";
    @Parameter(name = "Line2")
    public String line2 = "Client";
    @Parameter(name = "Line3")
    public String line3 = "OnTop";
    @Parameter(name = "Line4")
    public String line4 = "";

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoSign").getEnabled();
    }
    public static AutoSign itz() {
        return ravex.manager.ModuleManager.delegate(AutoSign.class);
    }


}