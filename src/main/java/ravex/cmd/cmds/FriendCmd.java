package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.manager.FriendManager;
import ravex.modules.misc.Commands;
import java.util.Locale;
import java.util.Set;
public class FriendCmd extends Cmd {
    public FriendCmd() {
        super("friend", "Manage friends", "f");
    }
    @Override
    public void execute(String[] args) {
        String pref = Commands.INSTANCE.prefix.getValue();
        if (args.length < 2) {
            CmdReg.print("§c[RaveX] Usage: " + pref + "friend <add/remove/list> [name]");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add":
                if (args.length < 3) { CmdReg.print("§c[RaveX] Usage: " + pref + "friend add <name>"); return; }
                FriendManager.INSTANCE.addFriend(args[2]);
                CmdReg.print("§a[RaveX] Added friend: §e" + args[2]);
                break;
            case "remove": case "del":
                if (args.length < 3) { CmdReg.print("§c[RaveX] Usage: " + pref + "friend remove <name>"); return; }
                FriendManager.INSTANCE.removeFriend(args[2]);
                CmdReg.print("§a[RaveX] Removed friend: §e" + args[2]);
                break;
            case "list":
                Set<String> friends = FriendManager.INSTANCE.getFriends();
                if (friends.isEmpty()) {
                    CmdReg.print("§e[RaveX] Your friend list is empty.");
                } else {
                    CmdReg.print("§5[RaveX] Friends: §e" + String.join("§r, §e", friends));
                }
                break;
            default:
                CmdReg.print("§c[RaveX] Unknown subcommand. Use add, remove, or list.");
        }
    }
}
