package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "MineAnimation", category = "Player")
public class MineAnimation {
    @Parameter(name = "HideHandSwing")
    public boolean hideSwing = true;
    @Parameter(name = "HideBlockCracks")
    public boolean hideCracks = true;

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(MineAnimation.class) || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundSwingPacket && hideSwing) {
            var mc = MinecraftWrapper.getWrapper();
            if (mc.getGameMode() != null && mc.getGameMode().isDestroying()) {
                event.setCancelled(true);
            }
        }
    }




}