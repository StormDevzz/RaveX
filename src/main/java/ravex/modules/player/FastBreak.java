package ravex.modules.player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
public class FastBreak extends Module {
    public static final FastBreak INSTANCE = new FastBreak();
    public final NumberParameter delay = new NumberParameter("Delay", 0, 0, 4, 1);

}
