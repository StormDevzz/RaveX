package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.utility.network.NetworkUtility;
public class ClipCmd extends Cmd {
    public ClipCmd() {
        super("clip", "Clip forward in look direction", "hclip");
    }
    @Override
    public void execute(String[] args) {
        var mc = ravex.mcwrapper.MinecraftWrapper.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        double dist = 1.0;
        if (args.length > 1) {
            try { dist = Double.parseDouble(args[1]); } catch (NumberFormatException e) {
                CmdReg.print("§c[RaveX] Invalid number: §e" + args[1]);
                return;
            }
        }
        double yaw = Math.toRadians(mc.player.getYRot());
        double dx = -Math.sin(yaw) * dist;
        double dz = Math.cos(yaw) * dist;
        double tx = mc.player.getX() + dx;
        double tz = mc.player.getZ() + dz;
        double ty = mc.player.getY();
        NetworkUtility.sendMoveRelative(tx, ty, tz, true, mc.player.horizontalCollision);
        mc.player.setPos(tx, ty, tz);
        CmdReg.print(String.format("§aClipped §e%.1f §ablocks forward.", dist));
    }
}
