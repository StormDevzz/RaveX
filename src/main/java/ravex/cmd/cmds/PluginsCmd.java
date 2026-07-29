package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.mcwrapper.MinecraftWrapper;
public class PluginsCmd extends Cmd {
    public PluginsCmd() {
        super("plugins", "Request server plugin list");
    }
    @Override
    public void execute(String[] args) {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;
        mc.player.connection.sendChat("/plugins");
        CmdReg.print("§7[RaveX] Sent §e/plugins §7to server — check server response in chat.");
    }
}
