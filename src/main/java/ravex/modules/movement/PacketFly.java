package ravex.modules.movement;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
public class PacketFly extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Fast",
        List.of("Fast", "Damage", "Setback", "Grim", "NCP"));
    public final NumberParameter speed = new NumberParameter("Speed", 0.2, 0.05, 1.0, 0.05);
    public final NumberParameter vertical = new NumberParameter("Vertical", 0.2, 0.0, 1.0, 0.05);
    public final NumberParameter tickSkip = new NumberParameter("TickSkip", 2.0, 1.0, 10.0, 1.0);
    public final BooleanParameter groundSpoof = new BooleanParameter("GroundSpoof", true);

    private int tickCounter = 0;
    private boolean grimToggle = false;

    private PacketFly() {
        super("PacketFly");
        tickSkip.setVisible(() -> "Grim".equals(mode.getValue()));
        groundSpoof.setVisible(() -> "Grim".equals(mode.getValue()) || "NCP".equals(mode.getValue()));
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        String m = mode.getValue();
        double spd = speed.getValue();
        double vert = vertical.getValue();
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        boolean onGround = mc.player.onGround();
        boolean hCollision = mc.player.horizontalCollision;
        if (m.equals("Fast")) {
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                x, y + vert, z, false, onGround
            ));
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                x, y - 0.05, z, true, onGround
            ));
        } else if (m.equals("Damage")) {
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                x, y + spd, z, false, onGround
            ));
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                x, y, z, false, onGround
            ));
        } else if (m.equals("Setback")) {
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                x, y + 9, z, false, onGround
            ));
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                x, y + vert, z, false, onGround
            ));
        } else if (m.equals("Grim")) {
            tickCounter++;
            if (tickCounter < tickSkip.getValue().intValue()) return;
            tickCounter = 0;
            grimToggle = !grimToggle;
            boolean gSpoof = groundSpoof.getValue();
            if (grimToggle) {
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y + vert, z, false, gSpoof || onGround
                ));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y + vert * 0.5, z, true, gSpoof || onGround
                ));
            } else {
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y + vert * 0.75, z, false, gSpoof || onGround
                ));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y - 0.05, z, true, gSpoof || onGround
                ));
            }
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                x, y, z, onGround, hCollision
            ));
        } else if (m.equals("NCP")) {
            boolean gSpoof = groundSpoof.getValue();
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                x, y + 0.42, z, false, gSpoof || onGround
            ));
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                x, y + 0.75, z, false, gSpoof || onGround
            ));
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                x, y + vert, z, false, gSpoof || onGround
            ));
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                x, y, z, true, onGround
            ));
        }
    }

    @Override
    protected void onEnable() {
        tickCounter = 0;
        grimToggle = false;
    }

    public static PacketFly itz() {
        return ModuleManager.get(PacketFly.class);
    }
}
