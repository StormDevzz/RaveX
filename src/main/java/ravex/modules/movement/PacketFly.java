package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
import java.util.List;




@ModuleInfo(name = "PacketFly", category = "Movement")
public class PacketFly implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Fast", "Damage", "Setback"})
    public String mode = "Fast";
    @Parameter(name = "Speed", min = 0.05, max = 1.0, step = 0.05)
    public double speed = 0.2;
    @Parameter(name = "Vertical", min = 0.0, max = 1.0, step = 0.05)
    public double vertical = 0.2;
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        String m = mode;
        double spd = speed;
        double vert = vertical;
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        boolean onGround = mc.player.onGround();
        if (m.equals("Fast")) {
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                x, y + vert, z, false, onGround
            ));
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                x, y - 0.05, z, true, onGround
            ));
        } else if (m.equals("Damage")) {
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                x, y + spd, z, false, onGround
            ));
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                x, y, z, false, onGround
            ));
        } else if (m.equals("Setback")) {
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                x, y + 9, z, false, onGround
            ));
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                x, y + vert, z, false, onGround
            ));
        }
    }
    public static PacketFly itz() {
        return ravex.manager.ModuleManager.delegate(PacketFly.class);
    }


}