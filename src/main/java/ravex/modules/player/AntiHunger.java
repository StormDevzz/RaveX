package ravex.modules.player;
import ravex.event.network.PacketEvent;
import ravex.event.Subscribe;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.utility.network.NetworkUtility;

@Module(name = "AntiHunger", category = "Player")
public class AntiHunger {
    @Parameter(name = "Mode", modes = {"NCP", "NCPStrict", "UNCP"})
    public String mode = "NCP";

    @Parameter(name = "UNCPDelay", min = 1, max = 20, step = 1, visible = "mode=UNCP")
    public int uncpDelay = 3;

    private int uncpCounter = 0;

    private boolean canSprint() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        return p != null && (p.getFoodData().getFoodLevel() > 5 || p.getAbilities().flying || p.getAbilities().mayfly);
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(AntiHunger.class) || !event.isSend()) return;
        String m = mode;
        var packet = event.getPacket();

        if ("UNCP".equals(m)) {
            if (NetworkUtility.isMovePacket(packet)) {
                uncpCounter++;
                if (uncpCounter % uncpDelay != 0) {
                    NetworkUtility.setMoveOnGround(packet, false);
                }
            }
            if (NetworkUtility.isCommandPacket(packet)) {
                event.setCancelled(true);
            }
            return;
        }

        if (NetworkUtility.isMovePacket(packet)) {
            NetworkUtility.setMoveOnGround(packet, false);
        }

        if (NetworkUtility.isSprintCommand(packet)) {
            if ("NCP".equals(m)) {
                event.setCancelled(true);
            } else if ("NCPStrict".equals(m) && !canSprint()) {
                event.setCancelled(true);
            }
        }
    }

    public void onDisable() {
        uncpCounter = 0;
    }
}
