package ravex.cmd.cmds;
import net.minecraft.client.Minecraft;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
public class PluginsCmd extends Cmd {
    public PluginsCmd() {
        super("plugins", "Request server plugin list");
    }
    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.connection.sendChat("/plugins");
        CmdReg.print("§7[RaveX] Sent §e/plugins §7to server — check server response in chat.");
    }
}
