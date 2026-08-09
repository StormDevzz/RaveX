package ravex.modules.misc;
import ravex.utility.network.NetworkUtility;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;

import ravex.parameter.StringParameter;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "AutoAuth", category = "Misc")
public class AutoAuth {
    @Parameter(name = "Password")
    public String password = "r1v2x";
    @Parameter(name = "Delay", min = 0.0, max = 20.0, step = 1.0)
    public double delay = 3.0;
    private int tickCounter = 0;
    private boolean hasRegistered = false;
    public void onEnable() { tickCounter = 0; hasRegistered = false; }
    public void onDisable() { hasRegistered = false; }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getPlayer().connection == null) return;
        if (hasRegistered) return;
        tickCounter++;
        if (tickCounter >= (int) delay * 20) {
            String pw = password;
            if (pw.isEmpty()) pw = "r1v2x";
            NetworkUtility.sendCommand("register " + pw + " " + pw);
            NetworkUtility.sendCommand("login " + pw);
            hasRegistered = true;
        }
    }




}