package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;




@Module(name = "Religion", category = "Misc")
public class Religion {
    @Parameter(name = "Christianity")
    public boolean christianity = false;
    @Parameter(name = "Atheism")
    public boolean atheism = false;
    @Parameter(name = "Islam")
    public boolean islam = false;
    @Parameter(name = "Buddhism")
    public boolean buddhism = false;
    @Parameter(name = "Hinduism")
    public boolean hinduism = false;
    @Parameter(name = "Suka")
    public boolean suka = false;
    public void onEnable() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.level == null) return;
        net.minecraft.client.player.LocalPlayer player = mc.player;
        if (player == null || player.connection == null) return;
        if (christianity) {
            player.connection.sendChat("Amen");
        } else if (atheism) {
            player.connection.sendChat("Nothing");
        } else if (islam) {
            player.connection.sendChat("AllahuAkbar");
        } else if (buddhism) {
            player.connection.sendChat("OmManiPadmeHum");
        } else if (hinduism) {
            player.connection.sendChat("Hare Krishna");
        } else if (suka) {
            player.connection.sendChat("Suka");
        }

    }




}