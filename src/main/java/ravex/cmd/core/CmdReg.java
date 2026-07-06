package ravex.cmd.core;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ravex.modules.misc.Commands;
import java.util.LinkedHashMap;
import java.util.Map;
public class CmdReg {
    public static final CmdReg INSTANCE = new CmdReg();
    private final Map<String, Cmd> commands = new LinkedHashMap<>();

    private CmdReg() {}

    public void register(Cmd cmd) {
        commands.put(cmd.getName().toLowerCase(), cmd);
        for (String alias : cmd.getAliases()) {
            commands.put(alias.toLowerCase(), cmd);
        }
    }

    public boolean process(String message) {
        if (!Commands.INSTANCE.getEnabled()) return false;
        String pref = Commands.INSTANCE.prefix.getValue();
        if (!message.startsWith(pref)) return false;
        String raw = message.substring(pref.length()).trim();
        if (raw.isEmpty()) return true;
        String[] args = raw.split("\\s+");
        String name = args[0].toLowerCase();
        Cmd cmd = commands.get(name);
        if (cmd == null) {
            print("§c[RaveX] Unknown command. Type §e" + pref + "help §cfor a list of commands.");
            return true;
        }
        cmd.execute(args);
        return true;
    }

    public Map<String, Cmd> getCommands() { return commands; }

    public static void print(String text) {
        if (!Commands.INSTANCE.showFeedback.getValue()) {
            if (!text.contains("§c") && !text.contains("§5") && !text.contains("Help")) return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(text), false);
        }
    }

    static {
        INSTANCE.register(new ravex.cmd.cmds.HelpCmd());
        INSTANCE.register(new ravex.cmd.cmds.ConfigCmd());
        INSTANCE.register(new ravex.cmd.cmds.CalcCmd());
        INSTANCE.register(new ravex.cmd.cmds.ToggleCmd());
        INSTANCE.register(new ravex.cmd.cmds.BindCmd());
        INSTANCE.register(new ravex.cmd.cmds.PanicCmd());
        INSTANCE.register(new ravex.cmd.cmds.SayCmd());
        INSTANCE.register(new ravex.cmd.cmds.TimeCmd());
        INSTANCE.register(new ravex.cmd.cmds.ModulesCmd());
        INSTANCE.register(new ravex.cmd.cmds.CoordsCmd());
        INSTANCE.register(new ravex.cmd.cmds.WaypointCmd());
        INSTANCE.register(new ravex.cmd.cmds.BenchmarkCmd());
        INSTANCE.register(new ravex.cmd.cmds.FriendCmd());
        INSTANCE.register(new ravex.cmd.cmds.PluginsCmd());
    }
}
