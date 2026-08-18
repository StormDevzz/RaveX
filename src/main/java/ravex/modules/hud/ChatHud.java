package ravex.modules.hud;
import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;

import ravex.modules.client.Hud;

@HudModule("ChatHud")
public class ChatHud extends ravex.modules.Module {
    @Parameter(name = "Scale", min = 0.25, max = 4.0, step = 0.05)
    public double scale = 1.0;
    private ChatHud() {
        super("ChatHud", 2, 30, 200, 100);
        setX(4); setY(4); setWidth(0); setHeight(0);
    }
    public void render(GuiGraphics graphics, float partialTicks) {
    }
}
