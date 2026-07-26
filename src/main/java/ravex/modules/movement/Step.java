package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.List;
@ModuleInfo(name = "Step", category = "Movement")
public class Step extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Vanilla", List.of("Vanilla", "Packet", "Grim"));
    public final NumberParameter height = new NumberParameter("Height", 1.0, 1.0, 2.5, 0.5);
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double stepHeight = height.getValue();
        var attr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
        if (attr != null) {
            attr.setBaseValue(stepHeight);
        }
        String modeVal = mode.getValue();
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
    protected void onDisable() {
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

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}