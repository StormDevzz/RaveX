package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.util.List;

public class ClickGui extends Module {
    public static final ClickGui INSTANCE = new ClickGui();

    public final BooleanParameter drawBackground = new BooleanParameter("Background", true);
    public final ModeParameter colorMode = new ModeParameter("Color Mode", "Positive", List.of("Positive", "Fade", "Rainbow", "DoubleColor"));
    public final NumberParameter colorSpeed = new NumberParameter("Speed", 18, 2, 54, 1);
    public final ravex.parameter.ColorParameter color1 = new ravex.parameter.ColorParameter("Color 1", 0xFFFFFFFF);
    public final ravex.parameter.ColorParameter color2 = new ravex.parameter.ColorParameter("Color 2", 0xFFCCCCCC);
    public final ModeParameter gradientMode = new ModeParameter("Gradient", "LeftToRight", List.of("LeftToRight", "UpsideDown", "Both"));
    public final BooleanParameter blur = new BooleanParameter("Blur", false);
    public final BooleanParameter customFont = new BooleanParameter("Custom Font", true);
    public final BooleanParameter switchless = new BooleanParameter("Switchless Options", true);

    public final BooleanParameter outlines = new BooleanParameter("Outlines", false);
    public final ravex.parameter.ColorParameter outlineColor = new ravex.parameter.ColorParameter("Outline Color", 0x44FFFFFF);
    public final BooleanParameter moduleOutlines = new BooleanParameter("Button Outlines", true);
    public final ravex.parameter.ColorParameter moduleOutlineColor = new ravex.parameter.ColorParameter("Button Border", 0xFFFFFFFF);

    
    public final NumberParameter buttonHeight = new NumberParameter("Button Height", 20, 12, 30, 1);
    public final NumberParameter panelWidth   = new NumberParameter("Panel Width",  190, 80, 280, 5);

    
    public final BooleanParameter moduleCounter = new BooleanParameter("Module Counter", true);

    public final BooleanParameter companionImage = new BooleanParameter("Show Image", false);
    public final ModeParameter companionType = new ModeParameter("Image Type", "Femboy", List.of("Femboy", "Wypher1", "Boykgun", "Cutie", "Kiss", "Laying", "Licking", "Pillow"));

    
    public final BooleanParameter separateSettings = new BooleanParameter("Separate Settings", false);

    
    public final NumberParameter guiScale = new NumberParameter("Gui Scale", 0.85, 0.5, 1.0, 0.05);

    
    public final NumberParameter gearRotationSpeed = new NumberParameter("Gear Speed", 30, 0, 180, 5);

    
    public final BooleanParameter smoothScroll = new BooleanParameter("Smooth Scroll", true);
    public final NumberParameter scrollSmoothness = new NumberParameter("Scroll Smoothness", 12, 1, 40, 1);

    
    public final BooleanParameter smoothOption = new BooleanParameter("Option Animation", true);
    public final NumberParameter optionSmoothness = new NumberParameter("Option Smoothness", 12, 1, 40, 1);

    
    public final BooleanParameter headerGlow = new BooleanParameter("Header Glow", true);
    public final NumberParameter headerGlowIntensity = new NumberParameter("Glow Intensity", 20, 5, 60, 5);

    
    public final NumberParameter tooltipSpeed = new NumberParameter("Tooltip Speed", 10, 1, 30, 1);
    public final NumberParameter tooltipOffsetX = new NumberParameter("Tooltip Offset X", 8, 0, 30, 1);
    public final NumberParameter tooltipOffsetY = new NumberParameter("Tooltip Offset Y", 8, 0, 30, 1);

    private ClickGui() {
        super("ClickGui", Category.CLIENT);
        addParameter(drawBackground);
        addParameter(colorMode);
        addParameter(colorSpeed);
        addParameter(color1);
        addParameter(color2);
        addParameter(gradientMode);
        addParameter(blur);
        addParameter(customFont);
        addParameter(switchless);
        addParameter(outlines);
        addParameter(outlineColor);
        addParameter(moduleOutlines);
        addParameter(moduleOutlineColor);
        addParameter(buttonHeight);
        addParameter(panelWidth);
        addParameter(moduleCounter);
        addParameter(companionImage);
        addParameter(companionType);
        addParameter(separateSettings);
        addParameter(guiScale);
        addParameter(gearRotationSpeed);
        addParameter(smoothScroll);
        addParameter(scrollSmoothness);
        addParameter(smoothOption);
        addParameter(optionSmoothness);
        addParameter(headerGlow);
        addParameter(headerGlowIntensity);
        addParameter(tooltipSpeed);
        addParameter(tooltipOffsetX);
        addParameter(tooltipOffsetY);
        optionSmoothness.setVisible(() -> smoothOption.getValue());
        color1.setVisible(() -> "Positive".equals(colorMode.getValue()) || "Fade".equals(colorMode.getValue()) || "DoubleColor".equals(colorMode.getValue()));
        color2.setVisible(() -> "DoubleColor".equals(colorMode.getValue()));
        setEnabled(true);
    }
}

