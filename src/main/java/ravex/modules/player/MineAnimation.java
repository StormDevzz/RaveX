package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.utility.network.NetworkUtility;

@Module(name = "MineAnimation", category = "Player")
public class MineAnimation {
    @Parameter(name = "HideHandSwing")
    public boolean hideSwing = true;
    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(MineAnimation.class) || !event.isSend()) return;
        if (NetworkUtility.isSwingPacket(event.getPacket()) && hideSwing) {
            var mc = MinecraftWrapper.getWrapper();
            if (mc.getGameMode() != null && mc.getGameMode().isDestroying()) {
                event.setCancelled(true);
            }
        }
    }




}