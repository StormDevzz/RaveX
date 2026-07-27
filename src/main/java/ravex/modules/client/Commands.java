package ravex.modules.client;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.parameter.StringParameter;
@Module(name = "Commands", category = "Client", enabled = true)
public class Commands {
    @Parameter(name = "Prefix")
    public String prefix = ".";
    @Parameter(name = "Feedback")
    public boolean showFeedback = true;
}