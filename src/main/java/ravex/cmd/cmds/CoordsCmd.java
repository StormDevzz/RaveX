package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.mcwrapper.MinecraftWrapper;
public class CoordsCmd extends Cmd {
    public CoordsCmd() {
        super("coords", "Show your coordinates");
    }
    @Override
    public void execute(String[] args) {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        var p = mc.getPlayer();
        CmdReg.print(String.format("§5[RaveX] §7XYZ: §e%.1f §7/ §e%.1f §7/ §e%.1f", p.getX(), p.getY(), p.getZ()));
        if (mc.getLevel() != null) {
            if (mc.getLevel().dimension().equals(net.minecraft.world.level.Level.NETHER)) {
                CmdReg.print(String.format("§7→ Overworld: §e%.1f §7/ §7- §7/ §e%.1f", p.getX() * 8, p.getZ() * 8));
            } else if (mc.getLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
                CmdReg.print(String.format("§7→ Nether: §e%.1f §7/ §7- §7/ §e%.1f", p.getX() / 8, p.getZ() / 8));
            }
        }
    }
}
