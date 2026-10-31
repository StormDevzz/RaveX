package ravex.modules.movement;

import ravex.integrations.baritone.BaritoneIntegration;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.manager.ModuleManager;
import ravex.event.EventBusHolder;
import ravex.event.client.SoundEvent;
import ravex.utility.client.ClientAlertUtility;

@Module(name = "AutoWalk", category = "Movement")
public class AutoWalk {
    @Parameter(name = "Mode", modes = {"Simple", "Baritone"})
    public String mode = "Simple";
    @Parameter(name = "Interval", min = 5.0, max = 120.0, step = 5.0, visible = "mode=Baritone")
    public double baritoneInterval = 30.0;
    @Parameter(name = "Range", min = 100.0, max = 10000.0, step = 100.0, visible = "mode=Baritone")
    public double baritoneRange = 2000.0;

    private final BaritoneIntegration baritone = new BaritoneIntegration();
    private long lastGotoTime = 0;
    private boolean notified = false;

    public void onEnable() {
        lastGotoTime = 0;
        notified = false;
        if ("Baritone".equals(mode)
                && (!BaritoneIntegration.isBaritonePresent() || !baritone.init())) {
            refuse();
        }
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;

        if ("Simple".equals(mode)) {
            mc.getOptions().keyUp.setDown(true);
            return;
        }

        if (!BaritoneIntegration.isBaritonePresent() || !baritone.isAvailable()) {
            refuse();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastGotoTime >= (int) baritoneInterval * 1000L) {
            int range = (int) baritoneRange;
            double yaw = Math.toRadians(player.getYRot());
            int x = player.blockPosition().getX() + (int) (-Math.sin(yaw) * range);
            int z = player.blockPosition().getZ() + (int) (Math.cos(yaw) * range);
            baritone.setGoalXZ(x, z);
            lastGotoTime = now;
        }
    }

    private void refuse() {
        if (notified) return;
        notified = true;
        EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.FAILURE));
        ClientAlertUtility.alert(ravex.utility.misc.LanguageUtility.t("autowalk_baritone"), 0xFFFF5555);
        ravex.modules.Module autoWalkMod = ModuleManager.INSTANCE.getByName("AutoWalk");
        if (autoWalkMod != null) autoWalkMod.setEnabled(false);
    }

    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getOptions() != null) {
            mc.getOptions().keyUp.setDown(false);
        }
        if ("Baritone".equals(mode)) {
            baritone.cancelPathing();
        }
    }
}
