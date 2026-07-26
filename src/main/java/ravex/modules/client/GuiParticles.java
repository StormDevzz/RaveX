package ravex.modules.client;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "GuiParticles", category = "Client")
public class GuiParticles implements ModuleAccess {
    @Parameter(name = "Type", modes = {"Star", "Bone", "Fire", "Sun", "Thunder", "Wave"})
    public String type = "Star";
    @Parameter(name = "Color", color = true)
    public int color = 0xFFFFFFFF;
    @Parameter(name = "Amount", min = 10, max = 150, step = 5)
    public double amount = 55;
    @Parameter(name = "Size", min = 1, max = 15, step = 0.5)
    public double size = 3;
    @Parameter(name = "Speed", min = 0.1, max = 5.0, step = 0.1)
    public double speed = 1.0;
    public GuiParticles() {
        
        ravex.manager.ModuleManager.INSTANCE.getByName("GuiParticles").setEnabled(false);
    }

    public static GuiParticles itz() {
        return ravex.manager.ModuleManager.delegate(GuiParticles.class);
    }


}