package ravex.modules.hud;
import net.minecraft.client.gui.GuiGraphics;
import ravex.modules.Module;
import ravex.modules.client.Hud;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import ravex.parameter.NumberParameter;
public class ChatHud extends Module {
=======
import ravex.parameter.NumberParameter;
public class ChatHud extends Module {
    public static final ChatHud INSTANCE = new ChatHud();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter scale = new NumberParameter("Scale", 1.0, 0.25, 4.0, 0.05);
    private ChatHud() {
        super("Chat", 4, 4, 0, 0);
    }
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
    }
<<<<<<< HEAD

    public static boolean maybeEnabled() {
        return maybeEnabled(ChatHud.class);
    }

    public static ChatHud itz() {
        return ModuleManager.get(ChatHud.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
