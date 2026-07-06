package ravex.cmd.cmds;
import net.minecraft.client.Minecraft;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.modules.misc.Commands;
import java.util.Locale;
public class WaypointCmd extends Cmd {
    public WaypointCmd() {
        super("waypoint", "Manage waypoints", "wp");
    }
    @Override
    public void execute(String[] args) {
        String pref = Commands.INSTANCE.prefix.getValue();
        if (args.length < 2) {
            CmdReg.print("§c[RaveX] Usage: " + pref + "waypoint <add/remove/list/clear> [name]");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        Minecraft mc = Minecraft.getInstance();
        switch (sub) {
            case "add": {
                if (args.length < 3) { CmdReg.print("§c[RaveX] Usage: " + pref + "waypoint add <name>"); return; }
                String name = args[2];
                if (mc.player == null || mc.level == null) return;
                double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
                String dim = mc.level.dimension().identifier().toString();
                ravex.modules.render.Waypoint.INSTANCE.addWaypoint(name, x, y, z, dim);
                CmdReg.print("§a[RaveX] Added waypoint: §e" + name + " §7(" + String.format("%.1f", x) + ", " + String.format("%.1f", y) + ", " + String.format("%.1f", z) + ")");
                break;
            }
            case "remove": case "del": {
                if (args.length < 3) { CmdReg.print("§c[RaveX] Usage: " + pref + "waypoint remove <name>"); return; }
                if (ravex.modules.render.Waypoint.INSTANCE.removeWaypoint(args[2]))
                    CmdReg.print("§a[RaveX] Removed waypoint: §e" + args[2]);
                else
                    CmdReg.print("§c[RaveX] Waypoint not found: §e" + args[2]);
                break;
            }
            case "list": {
                var waypoints = ravex.modules.render.Waypoint.getWaypoints();
                if (waypoints.isEmpty()) { CmdReg.print("§e[RaveX] No waypoints saved."); return; }
                CmdReg.print("§5[RaveX] Waypoints: §7(" + waypoints.size() + ")");
                for (var wp : waypoints) {
                    String dim = wp.dimension();
                    if (dim.startsWith("minecraft:")) dim = dim.substring(10);
                    double dist = 0;
                    if (mc.player != null) {
                        dist = Math.sqrt((wp.x() + 0.5 - mc.player.getX()) * (wp.x() + 0.5 - mc.player.getX()) + (wp.y() + 0.5 - mc.player.getY()) * (wp.y() + 0.5 - mc.player.getY()) + (wp.z() + 0.5 - mc.player.getZ()) * (wp.z() + 0.5 - mc.player.getZ()));
                    }
                    CmdReg.print(" §e" + wp.name() + " §7- " + String.format("%.1f", wp.x()) + " " + String.format("%.1f", wp.y()) + " " + String.format("%.1f", wp.z()) + " §8[" + dim + "] §7(" + String.format("%.0f", dist) + "m)");
                }
                break;
            }
            case "clear": {
                int count = ravex.modules.render.Waypoint.INSTANCE.waypoints.size();
                ravex.modules.render.Waypoint.INSTANCE.clearWaypoints();
                CmdReg.print("§a[RaveX] Cleared §e" + count + " §awaypoints.");
                break;
            }
            default:
                CmdReg.print("§c[RaveX] Unknown subcommand. Use add, remove, list, or clear.");
        }
    }
}
