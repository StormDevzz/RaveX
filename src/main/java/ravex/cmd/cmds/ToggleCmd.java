package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
public class ToggleCmd extends Cmd {
    public ToggleCmd() {
        super("toggle", "Toggle a module on/off");
    }
    @Override
    public void execute(String[] args) {
        if (args.length < 2) { CmdReg.print("§c[RaveX] Usage: .toggle <module>"); return; }
        Module m = ModuleManager.INSTANCE.getByName(args[1]);
        if (m == null) { CmdReg.print("§c[RaveX] Module not found: §e" + args[1]); return; }
        m.toggle();
        CmdReg.print("§a[RaveX] §e" + m.getName() + " §7→ " + (m.getEnabled() ? "§aON" : "§cOFF"));
    }
}
