package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.manager.FriendManager;
import ravex.modules.client.Commands;
import java.util.Locale;
import java.util.Set;
import ravex.manager.ModuleManager;
public class FriendCmd extends Cmd {
    public FriendCmd() {
        super("friend", "Manage friends", "f");
    }
    @Override
    public void execute(String[] args) {
        String pref = ModuleManager.get(Commands.class).prefix;
        if (args.length < 2) {
            CmdReg.print("§9[§bRaveX§9] §cUsage: " + pref + "friend <add/remove/list> [name]");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add":
                if (args.length < 3) { CmdReg.print("§9[§bRaveX§9] §cUsage: " + pref + "friend add <name>"); return; }
                FriendManager.INSTANCE.addFriend(args[2]);
                CmdReg.print("§9[§bRaveX§9] §aAdded friend: §e" + args[2]);
                break;
            case "remove": case "del": case "fuck":
                if (args.length < 3) { CmdReg.print("§9[§bRaveX§9] §cUsage: " + pref + "friend remove <name>"); return; }
                FriendManager.INSTANCE.removeFriend(args[2]);
                CmdReg.print("§9[§bRaveX§9] §aRemoved friend: §e" + args[2]);
                break;
            case "list":
                Set<String> friends = FriendManager.INSTANCE.getFriends();
                if (friends.isEmpty()) {
                    CmdReg.print("§9[§bRaveX§9] §eYour friend list is empty.");
                } else {
                    CmdReg.print("§9[§bRaveX§9] §7Friends: §e" + String.join("§r, §e", friends));
                }
                break;
            default:
                CmdReg.print("§9[§bRaveX§9] §cUnknown subcommand. Use add, remove, or list.");
        }
    }
}
