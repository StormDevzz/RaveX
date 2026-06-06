package ravex.modules.client;

import ravex.modules.Category;
import ravex.modules.Module;

public class GuiParticles extends Module {
    public static final GuiParticles INSTANCE = new GuiParticles();

    public GuiParticles() {
        super("GuiParticles", Category.CLIENT);
        setEnabled(false);
    }
}
