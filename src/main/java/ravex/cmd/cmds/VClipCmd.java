package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.utility.network.NetworkUtility;
public class VClipCmd extends Cmd {
    public VClipCmd() {
        super("vclip", "Clip vertically", "clip2");
    }
    @Override
    public void execute(String[] args) {
        var mc = ravex.mcwrapper.MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getConnection() == null) return;
        double dist = 1.0;
        if (args.length > 1) {
            try { dist = Double.parseDouble(args[1]); } catch (NumberFormatException e) {
                CmdReg.print("§c[RaveX] Invalid number: §e" + args[1]);
                return;
            }
        }
        double tx = mc.getPlayer().getX();
        double ty = mc.getPlayer().getY() + dist;
        double tz = mc.getPlayer().getZ();
        NetworkUtility.sendMoveRelative(tx, ty, tz, true, mc.getPlayer().horizontalCollision);
        mc.getPlayer().setPos(tx, ty, tz);
        CmdReg.print(String.format("§aClipped §e%.1f §ablocks %s.", Math.abs(dist), (dist >= 0 ? "up" : "down")));
    }
}
