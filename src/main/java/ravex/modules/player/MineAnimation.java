package ravex.modules.player;
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
public class MineAnimation extends Module {
    public final BooleanParameter hideSwing = new BooleanParameter("HideHandSwing", true);
    public final BooleanParameter hideCracks = new BooleanParameter("HideBlockCracks", true);

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!getEnabled() || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundSwingPacket && hideSwing.getValue()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null && mc.gameMode.isDestroying()) {
                event.setCancelled(true);
            }
        }
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(MineAnimation.class);
    }
    public static MineAnimation itz() {
        return ModuleManager.get(MineAnimation.class);
    }

}