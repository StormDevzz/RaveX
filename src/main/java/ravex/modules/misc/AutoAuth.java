package ravex.modules.misc;
import ravex.utility.network.NetworkUtility;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;

import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "AutoAuth", category = "Misc")
public class AutoAuth {
    @Parameter(name = "Password")
    public String password = "r1v2x";
    private boolean sent = false;
    public void onEnable() { sent = false; }
    public void onDisable() { sent = false; }
    public void onTick() {
        if (sent) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getPlayer().connection == null) return;
        String pw = password;
        if (pw == null || pw.isEmpty()) pw = "r1v2x";
        NetworkUtility.sendCommand("register " + pw + " " + pw);
        NetworkUtility.sendCommand("login " + pw);
        sent = true;
    }
}
