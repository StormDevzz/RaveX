package ravex.modules.hud;

import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.modules.client.Hud;
import ravex.gui.hudeditor.HudEditorScreen;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.ColorUtility;

@HudModule("ChatHud")
public class ChatHud extends ravex.modules.Module {
    public ChatHud() {
        super("ChatHud", 4, 250, 320, 120);
        setX(4);
        setY(250);
        setWidth(320);
        setHeight(120);
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        setWidth(320);
        setHeight(120);
        var mc = MinecraftWrapper.getWrapper();
        if (!(mc.getScreen() instanceof HudEditorScreen)) return;

        int bx = getX(), by = getY();
        int pw = getWidth(), ph = getHeight();
        int activeColor = ColorUtility.getActiveColor();

        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);

        HudRendererUtility.drawLabel(graphics, "Chat", bx + 6, by + 4, activeColor);
        var chat = mc.getRaw().gui.getChat();
        if (chat != null) {
            chat.render(graphics, mc.getFont(), 0, 0, 0, true, false);
        }
    }
}
