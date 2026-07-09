package ravex.cmd.cmds;
import net.minecraft.client.Minecraft;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.modules.client.Commands;
import java.util.Locale;
import ravex.manager.ModuleManager;
public class WaypointCmd extends Cmd {
    public WaypointCmd() {
        super("waypoint", "Manage waypoints", "wp");
    }
    @Override
    public void execute(String[] args) {
        String pref = ModuleManager.get(Commands.class).prefix.getValue();
        if (args.length < 2) {
            CmdReg.print(this, "§cUsage: " + pref + "waypoint <add/list/remove/delete/clear>");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        Minecraft mc = Minecraft.getInstance();
        switch (sub) {
            case "add": {
                if (mc.player == null || mc.level == null) return;

                if (args.length >= 6) {
                    try {
                        double x = Double.parseDouble(args[2]);
                        double y = Double.parseDouble(args[3]);
                        double z = Double.parseDouble(args[4]);
                        StringBuilder nameBuilder = new StringBuilder();
                        for (int i = 5; i < args.length; i++) {
                            if (nameBuilder.length() > 0) nameBuilder.append(" ");
                            nameBuilder.append(args[i]);
                        }
                        String name = nameBuilder.toString();
                        String dim = mc.level.dimension().identifier().toString();
                        ModuleManager.get(ravex.modules.render.Waypoint.class).addWaypoint(name, x, y, z, dim);
                        CmdReg.print(this, "§aAdded waypoint: §e" + name + " §7(" + String.format("%.1f", x) + ", " + String.format("%.1f", y) + ", " + String.format("%.1f", z) + ")");
                    } catch (NumberFormatException e) {
                        CmdReg.print(this, "§cInvalid coordinates. Usage: " + pref + "waypoint add <x> <y> <z> <name>");
                    }
                } else if (args.length >= 3) {
                    String name = args[2];
                    double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
                    String dim = mc.level.dimension().identifier().toString();
                    ModuleManager.get(ravex.modules.render.Waypoint.class).addWaypoint(name, x, y, z, dim);
                    CmdReg.print(this, "§aAdded waypoint: §e" + name + " §7(" + String.format("%.1f", x) + ", " + String.format("%.1f", y) + ", " + String.format("%.1f", z) + ")");
                } else {
                    CmdReg.print(this, "§cUsage: " + pref + "waypoint add <name> or " + pref + "waypoint add <x> <y> <z> <name>");
                }
                break;
            }
            case "remove": case "del": case "delete": {
                if (args.length < 3) { CmdReg.print(this, "§cUsage: " + pref + "waypoint remove <name>"); return; }
                StringBuilder nameBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    if (nameBuilder.length() > 0) nameBuilder.append(" ");
                    nameBuilder.append(args[i]);
                }
                if (ModuleManager.get(ravex.modules.render.Waypoint.class).removeWaypoint(nameBuilder.toString()))
                    CmdReg.print(this, "§aRemoved waypoint: §e" + nameBuilder);
                else
                    CmdReg.print(this, "§cWaypoint not found: §e" + nameBuilder);
                break;
            }
            case "list": {
                var waypoints = ravex.modules.render.Waypoint.getWaypoints();
                if (waypoints.isEmpty()) { CmdReg.print(this, "§eNo waypoints saved."); return; }
                CmdReg.print(this, "§5Waypoints: §7(" + waypoints.size() + ")");
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
                int count = ModuleManager.get(ravex.modules.render.Waypoint.class).waypoints.size();
                ModuleManager.get(ravex.modules.render.Waypoint.class).clearWaypoints();
                CmdReg.print(this, "§aCleared §e" + count + " §awaypoints.");
                break;
            }
            default:
                CmdReg.print(this, "§cUnknown subcommand. Use add, list, remove, delete, or clear.");
        }
    }
}
