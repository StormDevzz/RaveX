package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.List;

public class Step extends Module {
    public static final Step INSTANCE = new Step();

    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla", List.of("Vanilla", "Packet"));
    public final NumberParameter height = new NumberParameter("Height", 1.0, 1.0, 2.5, 0.5);

    private Step() {
        super("Step", Category.MOVEMENT);
        addParameter(mode);
        addParameter(height);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double stepHeight = height.getValue();
        var attr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
        if (attr != null) {
            attr.setBaseValue(stepHeight);
        }

        if (mode.getValue().equalsIgnoreCase("Packet")) {
            
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
        }
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            var attr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
            if (attr != null) {
                attr.setBaseValue(0.6); 
            }
        }
    }
}
