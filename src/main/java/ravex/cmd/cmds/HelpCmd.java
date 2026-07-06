package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.modules.misc.Commands;
public class HelpCmd extends Cmd {
    public HelpCmd() {
        super("help", "Show this list");
    }
    @Override
    public void execute(String[] args) {
        String p = Commands.INSTANCE.prefix.getValue();
        CmdReg.print("§5[RaveX] §7════ Command List ════");
        for (Cmd cmd : CmdReg.INSTANCE.getCommands().values()) {
            if (cmd == this) continue;
            CmdReg.print(" §e" + p + cmd.getName() + "§7 - " + cmd.getDescription());
        }
    }
}
