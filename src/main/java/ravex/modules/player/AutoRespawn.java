package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

public class AutoRespawn extends Module {
    public static final AutoRespawn INSTANCE = new AutoRespawn();

    private AutoRespawn() {
        super("AutoRespawn", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isAlive()) {
            mc.player.connection.send(new ServerboundClientCommandPacket(
                ServerboundClientCommandPacket.Action.PERFORM_RESPAWN
            ));
        }
    }
}
