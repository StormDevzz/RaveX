package ravex.modules.hud;

import net.minecraft.client.gui.GuiGraphics;
import ravex.modules.HudModule;
import ravex.modules.render.Hud;
import ravex.parameter.NumberParameter;

public class ChatHud extends HudModule {
    public static final ChatHud INSTANCE = new ChatHud();

    public final NumberParameter scale = new NumberParameter("Scale", 1.0, 0.25, 4.0, 0.05);

    private ChatHud() {
        super("Chat", 4, 4, 0, 0);
        addParameter(scale);
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        // Chat is repositioned via MixinChatHud
    }
}
