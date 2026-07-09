package ravex.modules.render;
import ravex.manager.ModuleManager;

import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;

public class ViewModel extends Module {
<<<<<<< HEAD
=======
    public static final ViewModel INSTANCE = new ViewModel();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter mainX = new NumberParameter("MainX", 0.0, -2.0, 2.0, 0.01);
    public final NumberParameter mainY = new NumberParameter("MainY", 0.0, -2.0, 2.0, 0.01);
    public final NumberParameter mainZ = new NumberParameter("MainZ", 0.0, -2.0, 2.0, 0.01);
    public final NumberParameter mainRotX = new NumberParameter("MainRotX", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter mainRotY = new NumberParameter("MainRotY", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter mainRotZ = new NumberParameter("MainRotZ", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter mainScale = new NumberParameter("MainScale", 1.0, 0.1, 3.0, 0.05);
    public final NumberParameter offX = new NumberParameter("OffX", 0.0, -2.0, 2.0, 0.01);
    public final NumberParameter offY = new NumberParameter("OffY", 0.0, -2.0, 2.0, 0.01);
    public final NumberParameter offZ = new NumberParameter("OffZ", 0.0, -2.0, 2.0, 0.01);
    public final NumberParameter offRotX = new NumberParameter("OffRotX", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter offRotY = new NumberParameter("OffRotY", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter offRotZ = new NumberParameter("OffRotZ", 0.0, -180.0, 180.0, 0.5);
    public final NumberParameter offScale = new NumberParameter("OffScale", 1.0, 0.1, 3.0, 0.05);
    public final NumberParameter swingSpeed = new NumberParameter("SwingSpeed", 1.0, 0.1, 3.0, 0.05);
    public final BooleanParameter hideMainHand = new BooleanParameter("HideMain", false);
    public final BooleanParameter hideOffHand = new BooleanParameter("HideOff", false);
    public final BooleanParameter noSwing = new BooleanParameter("NoSwing", false);

<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(ViewModel.class);
    }

    public static ViewModel itz() {
        return ModuleManager.get(ViewModel.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
