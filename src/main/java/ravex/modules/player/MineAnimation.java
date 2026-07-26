package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;

@ModuleInfo(name = "MineAnimation", category = "net.minecraft.world.entity.player.Player")
public class MineAnimation implements ModuleAccess {
    @Parameter(name = "HideHandSwing")
    public boolean hideSwing = true;
    @Parameter(name = "HideBlockCracks")
    public boolean hideCracks = true;

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("MineAnimation").getEnabled() || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundSwingPacket && hideSwing) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null && mc.gameMode.isDestroying()) {
                event.setCancelled(true);
            }
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("MineAnimation").getEnabled();
    }
    public static MineAnimation itz() {
        return ravex.manager.ModuleManager.delegate(MineAnimation.class);
    }


}