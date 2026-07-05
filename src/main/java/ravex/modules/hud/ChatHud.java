package ravex.modules.hud;
import net.minecraft.client.gui.GuiGraphics;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.parameter.NumberParameter;
public class ChatHud extends Module {
    public static final ChatHud INSTANCE = new ChatHud();
    public final NumberParameter scale = new NumberParameter("Scale", 1.0, 0.25, 4.0, 0.05);
    private ChatHud() {
        super("Chat", 4, 4, 0, 0);
    }
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
    }
}
