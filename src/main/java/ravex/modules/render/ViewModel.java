package ravex.modules.render;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
public class ViewModel extends Module {
    public static final ViewModel INSTANCE = new ViewModel();
    public final NumberParameter mainX    = new NumberParameter("Main X",     0.0, -2.0, 2.0, 0.01);
    public final NumberParameter mainY    = new NumberParameter("Main Y",     0.0, -2.0, 2.0, 0.01);
    public final NumberParameter mainZ    = new NumberParameter("Main Z",     0.0, -2.0, 2.0, 0.01);
    public final NumberParameter mainRotX = new NumberParameter("Main Rot X", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter mainRotY = new NumberParameter("Main Rot Y", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter mainRotZ = new NumberParameter("Main Rot Z", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter mainScale = new NumberParameter("Main Scale", 1.0, 0.1, 3.0, 0.05);
    public final NumberParameter offX    = new NumberParameter("Off X",     0.0, -2.0, 2.0, 0.01);
    public final NumberParameter offY    = new NumberParameter("Off Y",     0.0, -2.0, 2.0, 0.01);
    public final NumberParameter offZ    = new NumberParameter("Off Z",     0.0, -2.0, 2.0, 0.01);
    public final NumberParameter offRotX = new NumberParameter("Off Rot X", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter offRotY = new NumberParameter("Off Rot Y", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter offRotZ = new NumberParameter("Off Rot Z", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter offScale = new NumberParameter("Off Scale", 1.0, 0.1, 3.0, 0.05);
    public final NumberParameter swingSpeed = new NumberParameter("Swing Speed", 1.0, 0.1, 3.0, 0.05);
    public final BooleanParameter hideMainHand = new BooleanParameter("Hide Main", false);
    public final BooleanParameter hideOffHand  = new BooleanParameter("Hide Off",  false);
    public final BooleanParameter noSwing      = new BooleanParameter("No Swing",  false);

}
