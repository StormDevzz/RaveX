package ravex.modules.misc;
import ravex.modules.ModuleAccess;
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
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        if (hasRegistered) return;
        tickCounter++;
        if (tickCounter >= (int) delay * 20) {
            String pw = password;
            if (pw.isEmpty()) pw = "r1v2x";
            mc.player.connection.sendCommand("register " + pw + " " + pw);
            mc.player.connection.sendCommand("login " + pw);
            hasRegistered = true;
        }
    }




}