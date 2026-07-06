package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
public class PanicCmd extends Cmd {
    public PanicCmd() {
        super("panic", "Disable ALL modules");
    }
    @Override
    public void execute(String[] args) {
        int count = 0;
        for (Module m : ModuleManager.INSTANCE.getModules()) {
            if (m.getEnabled()) { m.setEnabled(false); count++; }
        }
        CmdReg.print("§c[RaveX] §ePanic! §cDisabled §e" + count + " §cmodules.");
    }
}
