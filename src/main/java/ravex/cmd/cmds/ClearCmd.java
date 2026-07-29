package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.mcwrapper.MinecraftWrapper;
public class ClearCmd extends Cmd {
    public ClearCmd() {
        super("clear", "Clear chat");
    }
    @Override
    public void execute(String[] args) {
        var mc = MinecraftWrapper.getInstance();
        if (mc.gui != null) {
            mc.gui.getChat().clearMessages(true);
            CmdReg.print(this, "§aChat cleared.");
        }
    }
}
