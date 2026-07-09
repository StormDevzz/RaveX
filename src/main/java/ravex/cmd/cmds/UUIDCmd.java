package ravex.cmd.cmds;
import net.minecraft.client.Minecraft;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import java.util.Locale;
import ravex.manager.ModuleManager;
public class UUIDCmd extends Cmd {
    public UUIDCmd() {
        super("uuid", "Get UUID of a player", "uuid");
    }
    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        String pref = ModuleManager.get(ravex.modules.client.Commands.class).prefix.getValue();

        if (args.length < 2) {
            CmdReg.print(this, "§eYour UUID: §7" + mc.player.getUUID().toString());
            return;
        }

        String target = args[1];
        if (target.equalsIgnoreCase(mc.player.getGameProfile().name())) {
            CmdReg.print(this, "§eYour UUID: §7" + mc.player.getUUID().toString());
            return;
        }

        var playerInfo = mc.getConnection().getPlayerInfo(target);
        if (playerInfo != null && playerInfo.getProfile() != null) {
            CmdReg.print(this, "§e" + target + "'s UUID: §7" + playerInfo.getProfile().id().toString());
        } else {
            CmdReg.print(this, "§cPlayer §e" + target + " §cnot found on tab list.");
        }
    }
}
