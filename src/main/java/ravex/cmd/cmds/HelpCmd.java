package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.modules.client.Commands;
import ravex.manager.ModuleManager;
import java.util.HashSet;
import java.util.Set;
public class HelpCmd extends Cmd {
    public HelpCmd() {
        super("help", "Show this list");
    }
    @Override
    public void execute(String[] args) {
        String p = ModuleManager.get(Commands.class).prefix.getValue();
        Set<Cmd> printed = new HashSet<>();
        CmdReg.print("§7Commands:");
        for (Cmd cmd : CmdReg.INSTANCE.getCommands().values()) {
            if (!printed.add(cmd)) continue;
            CmdReg.print(" §e" + p + cmd.getName() + "§7 - " + cmd.getDescription());
        }
    }
}
