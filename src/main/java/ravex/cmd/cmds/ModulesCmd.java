package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
public class ModulesCmd extends Cmd {
    public ModulesCmd() {
        super("modules", "List all enabled modules");
    }
    @Override
    public void execute(String[] args) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Module m : ModuleManager.INSTANCE.getModules()) {
            if (m.getEnabled()) {
                if (count > 0) sb.append("§7, ");
                sb.append("§e").append(m.getName());
                count++;
            }
        }
        if (count == 0) {
            CmdReg.print("§7[RaveX] No modules are enabled.");
        } else {
            CmdReg.print("§5[RaveX] §7Active (§e" + count + "§7): " + sb);
        }
    }
}
