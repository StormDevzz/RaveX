package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.event.client.TickEvent;
import ravex.event.network.PacketEvent;
import ravex.event.Subscribe;
import ravex.mixin.network.AccessorServerboundMovePlayerPacket;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import java.util.List;




@ModuleInfo(name = "NoFall", category = "Movement")
public class NoFall implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Vanilla", "NCP", "Grim"})
    public String mode = "Vanilla";

    private boolean wasOnGround = true;

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("NoFall").getEnabled() || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (!(packet instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket movePacket)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if ("Grim".equals(mode)) return;

        if (mc.player.fallDistance <= 2.0) return;

        AccessorServerboundMovePlayerPacket accessor = (AccessorServerboundMovePlayerPacket) movePacket;
        accessor.setOnGround(true);
    }

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("NoFall").getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String modeVal = mode;
        if ("Grim".equals(modeVal)) {
            if (wasOnGround && !mc.player.onGround() && mc.player.fallDistance > 0.5) {
                mc.player.setDeltaMovement(
                    mc.player.getDeltaMovement().x,
                    0.42,
                    mc.player.getDeltaMovement().z
                );
                mc.player.fallDistance = 0;
            }
            wasOnGround = mc.player.onGround();
        }
    }

    public static NoFall itz() {
        return ravex.manager.ModuleManager.delegate(NoFall.class);
    }


}