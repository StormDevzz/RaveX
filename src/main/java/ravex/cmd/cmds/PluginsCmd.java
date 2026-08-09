package ravex.cmd.cmds;
import ravex.utility.network.NetworkUtility;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.mcwrapper.MinecraftWrapper;
public class PluginsCmd extends Cmd {
    public PluginsCmd() {
        super("plugins", "Request server plugin list");
    }
    @Override
    public void execute(String[] args) {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        NetworkUtility.sendChat("/plugins");
        CmdReg.print("§7[RaveX] Sent §e/plugins §7to server — check server response in chat.");
    }
}
