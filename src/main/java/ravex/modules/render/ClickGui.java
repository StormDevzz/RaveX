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
    public final ModeParameter colorMode = new ModeParameter("Color Mode", "Static", List.of("Static", "Sky", "LightRainbow", "Rainbow", "Fade", "DoubleColor", "Analogous"));
    public final NumberParameter colorSpeed = new NumberParameter("Speed", 18, 2, 54, 1);
    public final ravex.parameter.ColorParameter color1 = new ravex.parameter.ColorParameter("Color 1", 0xFF1E88E5);
    public final ravex.parameter.ColorParameter color2 = new ravex.parameter.ColorParameter("Color 2", 0xFFE63946);
    public final ModeParameter gradientMode = new ModeParameter("Gradient", "LeftToRight", List.of("LeftToRight", "UpsideDown", "Both"));
    public final BooleanParameter blur = new BooleanParameter("Blur", false);
    public final BooleanParameter customFont = new BooleanParameter("Custom Font", true);

    public final BooleanParameter outlines = new BooleanParameter("Outlines", true);
    public final ravex.parameter.ColorParameter outlineColor = new ravex.parameter.ColorParameter("Outline Color", 0x44FFFFFF);
    public final BooleanParameter moduleOutlines = new BooleanParameter("Button Outlines", true);
    public final ravex.parameter.ColorParameter moduleOutlineColor = new ravex.parameter.ColorParameter("Button Border", 0xFF2A2A35);

    // Live-adjustable layout
    public final NumberParameter buttonHeight = new NumberParameter("Button Height", 15, 12, 24, 1);
    public final NumberParameter panelWidth   = new NumberParameter("Panel Width",  120, 80, 200, 5);

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
        addParameter(outlines);
        addParameter(outlineColor);
        addParameter(moduleOutlines);
        addParameter(moduleOutlineColor);
        addParameter(buttonHeight);
        addParameter(panelWidth);
        color1.setVisible(() -> "Static".equals(colorMode.getValue()) || "Fade".equals(colorMode.getValue()) || "DoubleColor".equals(colorMode.getValue()) || "Analogous".equals(colorMode.getValue()));
        color2.setVisible(() -> "DoubleColor".equals(colorMode.getValue()) || "Analogous".equals(colorMode.getValue()));
        setEnabled(true);
    }
}

