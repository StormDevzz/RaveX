package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.List;
import ravex.utility.misc.PotionUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "Step", category = "Movement")
public class Step {
    @Parameter(name = "Mode", modes = {"Vanilla", "Packet", "Grim"})
    public String mode = "Vanilla";
    @Parameter(name = "Height", min = 1.0, max = 2.5, step = 0.5)
    public double height = 1.0;
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;
        PotionUtility.setStepHeight(mc.player, height);
        String modeVal = mode;
        if (modeVal.equalsIgnoreCase("Packet")) {
            if (mc.player.horizontalCollision && mc.player.onGround()) {
                var connection = mc.player.connection;
                if (connection != null) {
                    double x = mc.player.getX();
                    double y = mc.player.getY();
                    double z = mc.player.getZ();
                    connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                        x, y + 0.41999998688698, z, false, mc.player.horizontalCollision
                    ));
                    connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                        x, y + 0.7531999805212, z, false, mc.player.horizontalCollision
                    ));
                }
            }
        } else if (modeVal.equalsIgnoreCase("Grim")) {
            if (mc.player.horizontalCollision && mc.player.onGround()) {
                var connection = mc.player.connection;
                if (connection != null) {
                    double x = mc.player.getX();
                    double y = mc.player.getY();
                    double z = mc.player.getZ();
                    connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                        x, y + 0.42, z, false, mc.player.horizontalCollision
                    ));
                    connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                        x, y + 0.75, z, false, mc.player.horizontalCollision
                    ));
                    connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                        x, y + 1.0, z, false, mc.player.horizontalCollision
                    ));
                    if (height > 1.0) {
                        connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                            x, y + 1.5, z, false, mc.player.horizontalCollision
                        ));
                    }
                }
            }
        }
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player != null) {
            PotionUtility.resetStepHeight(mc.player);
        }
    }



}