package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;

public class ClickGui extends Module {
    public static final ClickGui INSTANCE = new ClickGui();

    public final BooleanParameter drawBackground = new BooleanParameter("Background", true);
    public final ModeParameter colorPalette = new ModeParameter("Palette", "Red", java.util.List.of("Red", "Blue", "Green", "Gold", "Purple", "Rainbow"));

    private ClickGui() {
        super("ClickGui", Category.RENDER);
        addParameter(drawBackground);
        addParameter(colorPalette);
        setEnabled(true); // Enabled by default
    }
}
