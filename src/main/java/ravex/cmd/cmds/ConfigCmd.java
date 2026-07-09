package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.manager.ConfigManager;
import ravex.modules.client.Commands;
import java.util.List;
import java.util.Locale;
import ravex.manager.ModuleManager;
public class ConfigCmd extends Cmd {
    public ConfigCmd() {
        super("config", "Manage configs", "cfg");
    }
    @Override
    public void execute(String[] args) {
        String pref = ModuleManager.get(Commands.class).prefix.getValue();
        if (args.length < 2) {
            CmdReg.print("§5[RaveX] Config commands: §e" + pref + "config save/load/list/delete <name>");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "save":
                if (args.length < 3) { CmdReg.print("§c[RaveX] Usage: " + pref + "config save <name>"); return; }
                if (ConfigManager.INSTANCE.save(args[2]))
                    CmdReg.print("§a[RaveX] Saved config: §e" + args[2]);
                else
                    CmdReg.print("§c[RaveX] Failed to save config: §e" + args[2]);
                break;
            case "load":
                if (args.length < 3) { CmdReg.print("§c[RaveX] Usage: " + pref + "config load <name>"); return; }
                if (ConfigManager.INSTANCE.load(args[2]))
                    CmdReg.print("§a[RaveX] Loaded config: §e" + args[2]);
                else
                    CmdReg.print("§c[RaveX] Failed to load config: §e" + args[2]);
                break;
            case "list":
                List<String> configs = ConfigManager.INSTANCE.list();
                if (configs.isEmpty()) { CmdReg.print("§e[RaveX] No configurations saved."); return; }
                CmdReg.print("§5[RaveX] Configs: §7" + String.join("§r, §e", configs));
                break;
            case "delete":
                if (args.length < 3) { CmdReg.print("§c[RaveX] Usage: " + pref + "config delete <name>"); return; }
                if (ConfigManager.INSTANCE.delete(args[2]))
                    CmdReg.print("§a[RaveX] Deleted config: §e" + args[2]);
                else
                    CmdReg.print("§c[RaveX] Failed to delete config: §e" + args[2]);
                break;
            default:
                CmdReg.print("§5[RaveX] Config commands: §e" + pref + "config save/load/list/delete <name>");
        }
    }
}
