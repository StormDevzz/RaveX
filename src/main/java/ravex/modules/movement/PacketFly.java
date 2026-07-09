package ravex.modules.movement;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
public class PacketFly extends Module {
<<<<<<< HEAD
=======
    public static final PacketFly INSTANCE = new PacketFly();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter mode = new ModeParameter("Mode", "Fast", List.of("Fast", "Damage", "Setback"));
    public final NumberParameter speed = new NumberParameter("Speed", 0.2, 0.05, 1.0, 0.05);
    public final NumberParameter vertical = new NumberParameter("Vertical", 0.2, 0.0, 1.0, 0.05);

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
        }
    }
<<<<<<< HEAD
    public static PacketFly itz() {
        return ModuleManager.get(PacketFly.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
