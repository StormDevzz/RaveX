<<<<<<< HEAD:src/main/java/ravex/modules/client/Commands.java
package ravex.modules.client;
import ravex.manager.ModuleManager;
=======
package ravex.modules.misc;
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3:src/main/java/ravex/modules/misc/Commands.java
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.StringParameter;
public class Commands extends Module {
<<<<<<< HEAD:src/main/java/ravex/modules/client/Commands.java
=======
    public static final Commands INSTANCE = new Commands();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3:src/main/java/ravex/modules/misc/Commands.java
    public final StringParameter prefix = new StringParameter("Prefix", ".");
    public final BooleanParameter showFeedback = new BooleanParameter("Feedback", true);
    private Commands() {
        super("Commands");
        setEnabled(true); 
<<<<<<< HEAD:src/main/java/ravex/modules/client/Commands.java
    }

    public static Commands itz() {
        return ModuleManager.get(Commands.class);
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3:src/main/java/ravex/modules/misc/Commands.java
    }
}
