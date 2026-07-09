package ravex.modules.misc;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.StringParameter;

public class Commands extends Module {
    public static final Commands INSTANCE = new Commands();

    public final StringParameter prefix = new StringParameter("Prefix", ".");
    public final BooleanParameter showFeedback = new BooleanParameter("Feedback", true);

    private Commands() {
        super("Commands", Category.CLIENT);
        addParameter(prefix);
        addParameter(showFeedback);
        setEnabled(true); 
    }
}
