package ravex.modules.world;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.StringParameter;
public class AutoSign extends Module {
    public static final AutoSign INSTANCE = new AutoSign();
    public final StringParameter line1 = new StringParameter("Line1", "RaveX");
    public final StringParameter line2 = new StringParameter("Line2", "Client");
    public final StringParameter line3 = new StringParameter("Line3", "OnTop");
    public final StringParameter line4 = new StringParameter("Line4", "");

}
