package ravex.modules.client;

import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;

public class ClickGui extends Module {
    public final BooleanParameter drawBackground = new BooleanParameter("Background", true);
    public final ModeParameter colorMode = new ModeParameter("ColorMode", "Positive",
            List.of("Positive", "Fade", "Rainbow", "DoubleColor"));
    public final NumberParameter colorSpeed = new NumberParameter("Speed", 18, 2, 54, 1);
    public final ravex.parameter.ColorParameter color1 = new ravex.parameter.ColorParameter("Color1", 0xFF40A9F8);
    public final ravex.parameter.ColorParameter color2 = new ravex.parameter.ColorParameter("Color2", 0xFFE63946);
    public final ModeParameter gradientMode = new ModeParameter("Gradient", "LeftToRight",
            List.of("LeftToRight", "UpsideDown", "Both"));
    public final NumberParameter backgroundOpacity = new NumberParameter("BackgroundOpacity", 40, 0, 200, 1);
    public final NumberParameter panelOpacity = new NumberParameter("PanelOpacity", 70, 0, 255, 1);
    public final NumberParameter buttonOpacity = new NumberParameter("ButtonOpacity", 35, 0, 255, 1);
    public final BooleanParameter blur = new BooleanParameter("Blur", true);
    public final BooleanParameter customFont = new BooleanParameter("CustomFont", true);
    public final BooleanParameter switchless = new BooleanParameter("SwitchlessOptions", true);
    public final BooleanParameter outlines = new BooleanParameter("Outlines", false);
    public final ravex.parameter.ColorParameter outlineColor = new ravex.parameter.ColorParameter("OutlineColor",
            0x30FFFFFF);
    public final BooleanParameter moduleOutlines = new BooleanParameter("ButtonOutlines", true);
    public final ravex.parameter.ColorParameter moduleOutlineColor = new ravex.parameter.ColorParameter("ButtonBorder",
            0xFF2A2A35);
    public final NumberParameter buttonHeight = new NumberParameter("ButtonHeight", 18, 8, 30, 1);
    public final NumberParameter panelWidth = new NumberParameter("PanelWidth", 130, 70, 300, 5);
    public final NumberParameter cornerRadius = new NumberParameter("CornerRadius", 12, 4, 24, 1);
    public final BooleanParameter moduleCounter = new BooleanParameter("ModuleCounter", true);
    public final BooleanParameter companionImage = new BooleanParameter("ShowImage", false);
    public final ModeParameter companionType = new ModeParameter("ImageType", "Femboy",
            List.of("Femboy", "Wypher1", "Boykgun", "Cutie", "Kiss", "Laying", "Licking", "Pillow"));
    public final NumberParameter guiScale = new NumberParameter("GuiScale", 0.85, 0.5, 1.0, 0.05);
    public final NumberParameter gearRotationSpeed = new NumberParameter("GearSpeed", 30, 0, 180, 5);
    public final BooleanParameter smoothScroll = new BooleanParameter("SmoothScroll", true);
    public final NumberParameter scrollSmoothness = new NumberParameter("ScrollSmoothness", 12, 1, 40, 1);
    public final BooleanParameter smoothOption = new BooleanParameter("OptionAnimation", true);
    public final NumberParameter optionSmoothness = new NumberParameter("OptionSmoothness", 12, 1, 40, 1);
    public final BooleanParameter headerGlow = new BooleanParameter("HeaderGlow", true);
    public final NumberParameter headerGlowIntensity = new NumberParameter("GlowIntensity", 20, 5, 60, 5);
    public final BooleanParameter wheelControl = new BooleanParameter("WheelControl", false);
    public final NumberParameter tooltipSpeed = new NumberParameter("TooltipSpeed", 10, 1, 30, 1);
    public final NumberParameter descriptionOpacity = new NumberParameter("DescriptionOpacity", 180, 0, 255, 1);
    public final NumberParameter tooltipOffsetX = new NumberParameter("TooltipOffsetX", 8, 0, 30, 1);
    public final NumberParameter tooltipOffsetY = new NumberParameter("TooltipOffsetY", 8, 0, 30, 1);
    public final BooleanParameter showToolbar = new BooleanParameter("ShowToolbar", false);
    public final BooleanParameter descriptionPanel = new BooleanParameter("DescriptionPanel", false);
    public final BooleanParameter showGear = new BooleanParameter("ShowGear", false);

    private ClickGui() {
        super("ClickGui");
        optionSmoothness.setVisible(() -> smoothOption.getValue());
        color1.setVisible(() -> "Positive".equals(colorMode.getValue()) || "Fade".equals(colorMode.getValue())
                || "DoubleColor".equals(colorMode.getValue()));
        color2.setVisible(() -> "DoubleColor".equals(colorMode.getValue()));
        setEnabled(true);
    }

    public static ClickGui itz() {
        return ModuleManager.get(ClickGui.class);
    }
}
