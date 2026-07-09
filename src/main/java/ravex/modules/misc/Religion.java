package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
public class Religion extends Module {
=======
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
public class Religion extends Module {
    public static final Religion INSTANCE = new Religion();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final BooleanParameter christianity = new BooleanParameter("Christianity", false);
    public final BooleanParameter atheism = new BooleanParameter("Atheism", false);
    public final BooleanParameter islam = new BooleanParameter("Islam", false);
    public final BooleanParameter buddhism = new BooleanParameter("Buddhism", false);
    public final BooleanParameter hinduism = new BooleanParameter("Hinduism", false);

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        LocalPlayer player = mc.player;
        if (player == null || player.connection == null) return;
        if (christianity.getValue()) {
            player.connection.sendChat("Amen!");
        } else if (atheism.getValue()) {
            player.connection.sendChat("Nothing?");
        } else if (islam.getValue()) {
            player.connection.sendChat("AllahuAkbar");
        } else if (buddhism.getValue()) {
            player.connection.sendChat("OmManiPadmeHum");
        } else if (hinduism.getValue()) {
            player.connection.sendChat("Hare Krishna!");
        }
    }
<<<<<<< HEAD

    public static Religion itz() {
        return ModuleManager.get(Religion.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
