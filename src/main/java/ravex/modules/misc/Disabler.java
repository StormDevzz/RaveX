package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.event.client.TickEvent;
import ravex.utility.network.NetworkUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;

@Module(name = "Disabler", category = "Misc")
public class Disabler {
    @Parameter(name = "Mode", modes = {"VerusCombat", "VerusMovement", "VerusAll"})
    public String mode = "VerusCombat";
    @Parameter(name = "Delay", min = 1.0, max = 20.0, step = 1.0)
    public double delay = 3.0;

    private int tickCounter = 0;

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(Disabler.class) || !event.isSend()) return;
        String modeVal = mode;
        if ("VerusCombat".equals(modeVal) || "VerusAll".equals(modeVal)) {
            var packet = event.getPacket();
            if (packet instanceof ServerboundInteractPacket
                || packet instanceof ServerboundSwingPacket) {
                return;
            }
        }
    }

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!Modules.enabled(Disabler.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getConnection() == null) return;

        String modeVal = mode;
        tickCounter++;

        boolean isMovement = "VerusMovement".equals(modeVal) || "VerusAll".equals(modeVal);
        boolean isCombat = "VerusCombat".equals(modeVal) || "VerusAll".equals(modeVal);

        if (isMovement && tickCounter % (int) delay == 0) {
            double x = mc.getPlayer().getX();
            double y = mc.getPlayer().getY();
            double z = mc.getPlayer().getZ();
            boolean onGround = mc.getPlayer().onGround();
            boolean hc = mc.getPlayer().horizontalCollision;
            double ox = (Math.random() - 0.5) * 0.001;
            double oz = (Math.random() - 0.5) * 0.001;
            NetworkUtility.sendMoveRelative(x + ox, y, z + oz, onGround, hc);
        }

        if (isCombat && tickCounter % ((int) delay * 2) == 0) {
            NetworkUtility.sendPacket(new ServerboundPlayerCommandPacket(
                mc.getPlayer(), ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            NetworkUtility.sendPacket(new ServerboundPlayerCommandPacket(
                mc.getPlayer(), ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        }

        if (tickCounter >= 40) tickCounter = 0;
    }

    public void onEnable() {
        tickCounter = 0;
    }

    public void onDisable() {
        tickCounter = 0;
    }
}
