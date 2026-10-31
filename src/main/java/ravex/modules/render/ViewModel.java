package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "ViewModel", category = "Render")
public class ViewModel {
    @Parameter(name = "Main", group = true)
    public boolean mainGroup = false;
    @Parameter(name = "MainX", min = -2.0, max = 2.0, step = 0.01, visible = "mainGroup")
    public double mainX = 0.0;
    @Parameter(name = "MainY", min = -2.0, max = 2.0, step = 0.01, visible = "mainGroup")
    public double mainY = 0.0;
    @Parameter(name = "MainZ", min = -2.0, max = 2.0, step = 0.01, visible = "mainGroup")
    public double mainZ = 0.0;
    @Parameter(name = "MainRotX", min = -180.0, max = 180.0, step = 0.5, visible = "mainGroup")
    public double mainRotX = 0.0;
    @Parameter(name = "MainRotY", min = -180.0, max = 180.0, step = 0.5, visible = "mainGroup")
    public double mainRotY = 0.0;
    @Parameter(name = "MainRotZ", min = -180.0, max = 180.0, step = 0.5, visible = "mainGroup")
    public double mainRotZ = 0.0;
    @Parameter(name = "MainScale", min = 0.1, max = 3.0, step = 0.05, visible = "mainGroup")
    public double mainScale = 1.0;
    @Parameter(name = "Off", group = true)
    public boolean offGroup = false;
    @Parameter(name = "OffX", min = -2.0, max = 2.0, step = 0.01, visible = "offGroup")
    public double offX = 0.0;
    @Parameter(name = "OffY", min = -2.0, max = 2.0, step = 0.01, visible = "offGroup")
    public double offY = 0.0;
    @Parameter(name = "OffZ", min = -2.0, max = 2.0, step = 0.01, visible = "offGroup")
    public double offZ = 0.0;
    @Parameter(name = "OffRotX", min = -180.0, max = 180.0, step = 0.5, visible = "offGroup")
    public double offRotX = 0.0;
    @Parameter(name = "OffRotY", min = -180.0, max = 180.0, step = 0.5, visible = "offGroup")
    public double offRotY = 0.0;
    @Parameter(name = "OffRotZ", min = -180.0, max = 180.0, step = 0.5, visible = "offGroup")
    public double offRotZ = 0.0;
    @Parameter(name = "OffScale", min = 0.1, max = 3.0, step = 0.05, visible = "offGroup")
    public double offScale = 1.0;
    @Parameter(name = "HideMain")
    public boolean hideMainHand = false;
    @Parameter(name = "HideOff")
    public boolean hideOffHand = false;
}
