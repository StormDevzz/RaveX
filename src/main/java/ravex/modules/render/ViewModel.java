package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "ViewModel", category = "Render")
public class ViewModel {
    @Parameter(name = "MainX", min = -2.0, max = 2.0, step = 0.01)
    public double mainX = 0.0;
    @Parameter(name = "MainY", min = -2.0, max = 2.0, step = 0.01)
    public double mainY = 0.0;
    @Parameter(name = "MainZ", min = -2.0, max = 2.0, step = 0.01)
    public double mainZ = 0.0;
    @Parameter(name = "MainRotX", min = -180.0, max = 180.0, step = 0.5)
    public double mainRotX = 0.0;
    @Parameter(name = "MainRotY", min = -180.0, max = 180.0, step = 0.5)
    public double mainRotY = 0.0;
    @Parameter(name = "MainRotZ", min = -180.0, max = 180.0, step = 0.5)
    public double mainRotZ = 0.0;
    @Parameter(name = "MainScale", min = 0.1, max = 3.0, step = 0.05)
    public double mainScale = 1.0;
    @Parameter(name = "OffX", min = -2.0, max = 2.0, step = 0.01)
    public double offX = 0.0;
    @Parameter(name = "OffY", min = -2.0, max = 2.0, step = 0.01)
    public double offY = 0.0;
    @Parameter(name = "OffZ", min = -2.0, max = 2.0, step = 0.01)
    public double offZ = 0.0;
    @Parameter(name = "OffRotX", min = -180.0, max = 180.0, step = 0.5)
    public double offRotX = 0.0;
    @Parameter(name = "OffRotY", min = -180.0, max = 180.0, step = 0.5)
    public double offRotY = 0.0;
    @Parameter(name = "OffRotZ", min = -180.0, max = 180.0, step = 0.5)
    public double offRotZ = 0.0;
    @Parameter(name = "OffScale", min = 0.1, max = 3.0, step = 0.05)
    public double offScale = 1.0;
    @Parameter(name = "SwingSpeed", min = 0.1, max = 3.0, step = 0.05)
    public double swingSpeed = 1.0;
    @Parameter(name = "HideMain")
    public boolean hideMainHand = false;
    @Parameter(name = "HideOff")
    public boolean hideOffHand = false;
    @Parameter(name = "NoSwing")
    public boolean noSwing = false;






}