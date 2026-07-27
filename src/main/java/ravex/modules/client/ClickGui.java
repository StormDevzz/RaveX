package ravex.modules.client;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.List;

@Module(name = "ClickGui", category = "Client", enabled = true)
public class ClickGui {
    @Parameter(name = "Background")
    public boolean drawBackground = true;
    @Parameter(name = "ColorMode", modes = {"Positive", "Fade", "Rainbow", "DoubleColor"})
    public String colorMode = "Positive";
    @Parameter(name = "Speed", min = 2, max = 54, step = 1)
    public double colorSpeed = 18;
    @Parameter(name = "Color1", color = true)
    public int color1 = 0xFF40A9F8;
    @Parameter(name = "Color2", color = true)
    public int color2 = 0xFFE63946;
    @Parameter(name = "Gradient", modes = {"LeftToRight", "UpsideDown", "Both"})
    public String gradientMode = "LeftToRight";
    @Parameter(name = "BackgroundOpacity", min = 0, max = 200, step = 1)
    public double backgroundOpacity = 40;
    @Parameter(name = "PanelOpacity", min = 0, max = 255, step = 1)
    public double panelOpacity = 70;
    @Parameter(name = "ButtonOpacity", min = 0, max = 255, step = 1)
    public double buttonOpacity = 35;
    @Parameter(name = "Blur")
    public boolean blur = true;
    @Parameter(name = "CustomFont")
    public boolean customFont = true;
    @Parameter(name = "SwitchlessOptions")
    public boolean switchless = true;
    @Parameter(name = "Outlines")
    public boolean outlines = false;
    @Parameter(name = "OutlineColor", color = true)
    public int outlineColor = 0x30FFFFFF;
    @Parameter(name = "ButtonOutlines")
    public boolean moduleOutlines = true;
    @Parameter(name = "ButtonBorder", color = true)
    public int moduleOutlineColor = 0xFFFFFFFF;
    @Parameter(name = "ButtonHeight", min = 8, max = 30, step = 1)
    public double buttonHeight = 18;
    @Parameter(name = "PanelWidth", min = 70, max = 300, step = 5)
    public double panelWidth = 130;
    @Parameter(name = "CornerRadius", min = 4, max = 24, step = 1)
    public double cornerRadius = 12;
    @Parameter(name = "ModuleCounter")
    public boolean moduleCounter = true;
    @Parameter(name = "ShowImage")
    public boolean companionImage = false;
    @Parameter(name = "ImageType", modes = {"Femboy", "Wypher1", "Boykgun", "Cutie", "Kiss", "Laying", "Licking", "Pillow", "Cutieeee", "Cutiemonster", "Furik", "Godofcoding", "Terrydavis"})
    public String companionType = "Femboy";
    @Parameter(name = "GuiScale", min = 0.5, max = 1.0, step = 0.05)
    public double guiScale = 0.85;
    @Parameter(name = "GearSpeed", min = 0, max = 180, step = 5)
    public double gearRotationSpeed = 30;
    @Parameter(name = "SmoothScroll")
    public boolean smoothScroll = true;
    @Parameter(name = "ScrollSmoothness", min = 1, max = 40, step = 1)
    public double scrollSmoothness = 12;
    @Parameter(name = "OptionAnimation")
    public boolean smoothOption = true;
    @Parameter(name = "OptionSmoothness", min = 1, max = 40, step = 1)
    public double optionSmoothness = 12;
    @Parameter(name = "HeaderGlow")
    public boolean headerGlow = true;
    @Parameter(name = "GlowIntensity", min = 5, max = 60, step = 5)
    public double headerGlowIntensity = 20;
    @Parameter(name = "WheelControl")
    public boolean wheelControl = false;
    @Parameter(name = "TooltipSpeed", min = 1, max = 30, step = 1)
    public double tooltipSpeed = 10;
    @Parameter(name = "DescriptionOpacity", min = 0, max = 255, step = 1)
    public double descriptionOpacity = 180;
    @Parameter(name = "TooltipOffsetX", min = 0, max = 30, step = 1)
    public double tooltipOffsetX = 8;
    @Parameter(name = "TooltipOffsetY", min = 0, max = 30, step = 1)
    public double tooltipOffsetY = 8;
    @Parameter(name = "ShowToolbar")
    public boolean showToolbar = false;
    @Parameter(name = "DescriptionPanel")
    public boolean descriptionPanel = false;
    @Parameter(name = "ShowGear")
    public boolean showGear = false;

}