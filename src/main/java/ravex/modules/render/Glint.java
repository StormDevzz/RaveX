package ravex.modules.render;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
public class Glint extends Module {
    public static final Glint INSTANCE = new Glint();
    public final BooleanParameter items = new BooleanParameter("Items", true);
    public final BooleanParameter armor = new BooleanParameter("Armor", true);
    public final ColorParameter color = new ColorParameter("Color", 0xFFFF00FF); 

}
