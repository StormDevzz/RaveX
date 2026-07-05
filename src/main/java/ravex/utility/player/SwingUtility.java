package ravex.utility.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

public class SwingUtility {
    public static void swing(LocalPlayer player, InteractionHand hand) {
        if (player != null) player.swing(hand);
    }

    public static void swingMainHand(LocalPlayer player) {
        swing(player, InteractionHand.MAIN_HAND);
    }

    public static void swingOffHand(LocalPlayer player) {
        swing(player, InteractionHand.OFF_HAND);
    }

    public static void swingServer(LocalPlayer player, InteractionHand hand) {
        if (player != null && player.connection != null)
            player.connection.send(new net.minecraft.network.protocol.game.ServerboundSwingPacket(hand));
    }
}
