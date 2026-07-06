package ravex.utility.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

public class NetworkUtility {
    private static Minecraft mc() { return Minecraft.getInstance(); }

    public static void sendStartDestroy(BlockPos pos, Direction dir, int seq) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, dir, seq));
    }

    public static void sendStopDestroy(BlockPos pos, Direction dir, int seq) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, dir, seq));
    }

    public static void sendDropAll(BlockPos pos, Direction dir) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS, pos, dir, 0));
    }

    public static void sendDropStack() {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.DROP_ITEM, BlockPos.ZERO, Direction.DOWN, 0));
    }

    public static void sendSetCarriedItem(int slot) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundSetCarriedItemPacket(slot));
    }

    public static void sendTeleportConfirm(int teleportId) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundAcceptTeleportationPacket(teleportId));
    }

    public static void sendMovePacket(Vec3 pos, boolean onGround, boolean horizontalCollision) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundMovePlayerPacket.Pos(pos.x, pos.y, pos.z, onGround, horizontalCollision));
    }

    public static void sendSwing(InteractionHand hand) {
        var c = mc().getConnection();
        if (c != null)
            c.send(new ServerboundSwingPacket(hand));
    }
}
