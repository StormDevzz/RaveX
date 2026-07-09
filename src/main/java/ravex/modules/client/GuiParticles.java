package ravex.modules.client;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
public class GuiParticles extends Module {
<<<<<<< HEAD
=======
    public static final GuiParticles INSTANCE = new GuiParticles();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter type = new ModeParameter("Type", "Star",
        java.util.List.of("Star", "Bone", "Fire", "Sun", "Thunder", "Wave"));
    public final ColorParameter color = new ColorParameter("Color", 0xFFFFFFFF);
    public final NumberParameter amount = new NumberParameter("Amount", 55, 10, 150, 5);
    public final NumberParameter size = new NumberParameter("Size", 3, 1, 15, 0.5);
    public final NumberParameter speed = new NumberParameter("Speed", 1.0, 0.1, 5.0, 0.1);
    public GuiParticles() {
        super("GuiParticles");
        setEnabled(false);
    }

    public static GuiParticles itz() {
        return ModuleManager.get(GuiParticles.class);
    }
}
