package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.manager.ModuleManager;
import ravex.mcwrapper.MinecraftWrapper;
public class UUIDCmd extends Cmd {
    public UUIDCmd() {
        super("uuid", "Get UUID of a player", "uuid");
    }
    @Override
    public void execute(String[] args) {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getConnection() == null) return;
        String pref = ModuleManager.get(ravex.modules.client.Commands.class).prefix;

        if (args.length < 2) {
            CmdReg.print(this, "§eYour UUID: §7" + mc.getPlayer().getUUID().toString());
            return;
        }

        String target = args[1];
        if (target.equalsIgnoreCase(mc.getPlayer().getGameProfile().name())) {
            CmdReg.print(this, "§eYour UUID: §7" + mc.getPlayer().getUUID().toString());
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
