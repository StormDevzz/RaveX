package ravex.cmd.cmds;
import net.minecraft.client.Minecraft;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
public class ClearCmd extends Cmd {
    public ClearCmd() {
        super("clear", "Clear chat");
    }
    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) {
            mc.gui.getChat().clearMessages(true);
            CmdReg.print(this, "§aChat cleared.");
        }
    }
}
