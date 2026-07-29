package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.network.NetworkUtility;
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
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null) return;
        var player = mc.getPlayer();
        if (player == null) return;
        if (christianity) {
            NetworkUtility.sendChat("Amen");
        } else if (atheism) {
            NetworkUtility.sendChat("Nothing");
        } else if (islam) {
            NetworkUtility.sendChat("AllahuAkbar");
        } else if (buddhism) {
            NetworkUtility.sendChat("OmManiPadmeHum");
        } else if (hinduism) {
            NetworkUtility.sendChat("Hare Krishna");
        } else if (suka) {
            NetworkUtility.sendChat("Suka");
        }
    }
}
