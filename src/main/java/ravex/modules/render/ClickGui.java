package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;

public class ClickGui extends Module {
    public static final ClickGui INSTANCE = new ClickGui();

    public final BooleanParameter drawBackground = new BooleanParameter("Background", true);
    public final ModeParameter colorPalette = new ModeParameter("Palette", "Red", java.util.List.of("Red", "Blue", "Green", "Gold", "Purple", "Rainbow", "Custom"));
    public final ravex.parameter.ColorParameter accentColor = new ravex.parameter.ColorParameter("Accent Color", 0xFFE63946);
    public final BooleanParameter customFont = new BooleanParameter("Custom Font", true);
    
    // Aesthetic customizations
    public final BooleanParameter outlines = new BooleanParameter("Outlines", true);
    public final ravex.parameter.ColorParameter outlineColor = new ravex.parameter.ColorParameter("Outline Color", 0x44FFFFFF);
    public final BooleanParameter moduleOutlines = new BooleanParameter("Button Outlines", true);
    public final ravex.parameter.ColorParameter moduleOutlineColor = new ravex.parameter.ColorParameter("Button Border", 0xFF2A2A35);

    private ClickGui() {
        super("ClickGui", Category.CLIENT);
        addParameter(drawBackground);
        addParameter(colorPalette);
        addParameter(accentColor);
        addParameter(customFont);
        addParameter(outlines);
        addParameter(outlineColor);
        addParameter(moduleOutlines);
        addParameter(moduleOutlineColor);
        accentColor.setVisible(() -> "Custom".equals(colorPalette.getValue()));
        setEnabled(true); // Enabled by default
    }
}
