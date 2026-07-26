package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.List;
@ModuleInfo(name = "Step", category = "Movement")
public class Step implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Vanilla", "Packet", "Grim"})
    public String mode = "Vanilla";
    @Parameter(name = "Height", min = 1.0, max = 2.5, step = 0.5)
    public double height = 1.0;
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double stepHeight = height;
        var attr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
        if (attr != null) {
            attr.setBaseValue(stepHeight);
        }
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
                    if (stepHeight > 1.0) {
                        connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                            x, y + 1.5, z, false, mc.player.horizontalCollision
                        ));
                    }
                }
            }
        }
    }
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            var attr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
            if (attr != null) {
                attr.setBaseValue(0.6);
            }
        }
    }
    public static Step itz() {
        return ravex.manager.ModuleManager.delegate(Step.class);
    }


}