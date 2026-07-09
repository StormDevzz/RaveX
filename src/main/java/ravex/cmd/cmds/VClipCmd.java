package ravex.cmd.cmds;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
public class VClipCmd extends Cmd {
    public VClipCmd() {
        super("vclip", "Clip vertically", "clip2");
    }
    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        double dist = 1.0;
        if (args.length > 1) {
            try { dist = Double.parseDouble(args[1]); } catch (NumberFormatException e) {
                CmdReg.print("§c[RaveX] Invalid number: §e" + args[1]);
                return;
            }
        }
        double tx = mc.player.getX();
        double ty = mc.player.getY() + dist;
        double tz = mc.player.getZ();
        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(tx, ty, tz, true, mc.player.horizontalCollision));
        mc.player.setPos(tx, ty, tz);
        CmdReg.print(String.format("§aClipped §e%.1f §ablocks %s.", Math.abs(dist), (dist >= 0 ? "up" : "down")));
    }
}
