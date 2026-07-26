package ravex.modules.client;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.parameter.StringParameter;
@ModuleInfo(name = "Commands", category = "Client")
public class Commands implements ModuleAccess {
    @Parameter(name = "Prefix")
    public String prefix = ".";
    @Parameter(name = "Feedback")
    public boolean showFeedback = true;
    private Commands() {
        
        ravex.manager.ModuleManager.INSTANCE.getByName("Commands").setEnabled(true);
    }

    public static Commands itz() {
        return ravex.manager.ModuleManager.delegate(Commands.class);
    }


}