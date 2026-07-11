package ravex.modules.client;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.StringParameter;
public class Commands extends Module {
    public final StringParameter prefix = new StringParameter("Prefix", ".");
    public final BooleanParameter showFeedback = new BooleanParameter("Feedback", true);
    private Commands() {
        super("Commands");
        setEnabled(true);
    }

    public static Commands itz() {
        return ModuleManager.get(Commands.class);
    }
}
