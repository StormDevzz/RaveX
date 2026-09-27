package ravex.cmd.cmds;

import net.minecraft.core.BlockPos;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.manager.ModuleManager;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.client.Commands;
import ravex.modules.render.Waypoint;

public class GpsCmd extends Cmd {
    private static BlockPos currentGps;

    public GpsCmd() {
        super("gps", "Set GPS navigation target", "nav");
    }

    public static BlockPos getGpsTarget() {
        return currentGps;
    }

    public static void setGpsTarget(BlockPos pos) {
        currentGps = pos;
    }

    @Override
    public void execute(String[] args) {
        String pref = ModuleManager.get(Commands.class).prefix;
        if (args.length < 2) {
            if (currentGps != null) {
                var mc = MinecraftWrapper.getWrapper();
                double dist = 0;
                if (mc.getPlayer() != null) {
                    dist = Math.sqrt(mc.getPlayer().distanceToSqr(currentGps.getX() + 0.5, currentGps.getY(), currentGps.getZ() + 0.5));
                }
                CmdReg.print(this, "§5[GPS] §7Target: §e" + currentGps.getX() + ", " + currentGps.getY() + ", " + currentGps.getZ() + " §7(" + (int) dist + "m)");
            } else {
                CmdReg.print(this, "§cUsage: " + pref + "gps <x> <z> | " + pref + "gps <x> <y> <z> | " + pref + "gps off");
            }
            return;
        }

        String sub = args[1].toLowerCase();
        if (sub.equals("off") || sub.equals("clear") || sub.equals("reset") || sub.equals("stop")) {
            currentGps = null;
            CmdReg.print(this, "§a[GPS] Destination cleared.");
            return;
        }

        if (sub.equals("wp") || sub.equals("waypoint")) {
            if (args.length < 3) {
                CmdReg.print(this, "§cUsage: " + pref + "gps waypoint <name>");
                return;
            }
            String wpName = args[2];
            for (var wp : Waypoint.getWaypoints()) {
                if (wp.name().equalsIgnoreCase(wpName)) {
                    currentGps = new BlockPos((int) wp.x(), (int) wp.y(), (int) wp.z());
                    CmdReg.print(this, "§a[GPS] Tracking waypoint §e" + wp.name() + " §7(" + currentGps.getX() + ", " + currentGps.getY() + ", " + currentGps.getZ() + ")");
                    return;
                }
            }
            CmdReg.print(this, "§cWaypoint not found: " + wpName);
            return;
        }

        var mc = MinecraftWrapper.getWrapper();
        int defY = mc.getPlayer() != null ? mc.getPlayer().getBlockY() : 64;

        try {
            if (args.length == 3) {
                int x = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);
                currentGps = new BlockPos(x, defY, z);
                CmdReg.print(this, "§a[GPS] Target set to §e" + x + ", " + z);
            } else if (args.length >= 4) {
                int x = Integer.parseInt(args[1]);
                int y = Integer.parseInt(args[2]);
                int z = Integer.parseInt(args[3]);
                currentGps = new BlockPos(x, y, z);
                CmdReg.print(this, "§a[GPS] Target set to §e" + x + ", " + y + ", " + z);
            } else {
                CmdReg.print(this, "§cUsage: " + pref + "gps <x> <z> | " + pref + "gps <x> <y> <z> | " + pref + "gps off");
            }
        } catch (NumberFormatException e) {
            CmdReg.print(this, "§cInvalid coordinates. Numbers expected.");
        }
    }
}
