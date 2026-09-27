package ravex.modules.movement;

import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.manager.ModuleManager;
import ravex.utility.render.ColorUtility;
import ravex.event.EventBusHolder;
import ravex.event.client.SoundEvent;
import ravex.utility.player.PlayerUtility;
import ravex.utility.client.ClientAlertUtility;

@Module(name = "AutoWalk", category = "Movement")
public class AutoWalk {
    @Parameter(name = "Mode", modes = {"Simple", "Baritone"})
    public String mode = "Simple";
    @Parameter(name = "Sprint", visible = "mode=Simple")
    public boolean sprint = false;
    @Parameter(name = "Interval", min = 5.0, max = 120.0, step = 5.0, visible = "mode=Baritone")
    public double baritoneInterval = 30.0;
    @Parameter(name = "Range", min = 100.0, max = 10000.0, step = 100.0, visible = "mode=Baritone")
    public double baritoneRange = 2000.0;

    private long lastGotoTime = 0;

    private boolean isBaritonePresent() {
        return ravex.integrations.baritone.BaritoneIntegration.isBaritonePresent();
    }

    public void onEnable() {
        if ("Baritone".equals(mode) && !isBaritonePresent()) {
            EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.FAILURE));
                ClientAlertUtility.alert(ravex.utility.misc.LanguageUtility.t("autowalk_baritone"), 0xFFFF5555);
            ravex.modules.Module autoWalkMod = ModuleManager.INSTANCE.getByName("AutoWalk");
            if (autoWalkMod != null) autoWalkMod.setEnabled(false);
        }
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        String m = mode;
        if ("Simple".equals(m)) {
            mc.getOptions().keyUp.setDown(true);
            if (sprint) player.setSprinting(true);
        } else if ("Baritone".equals(m)) {
            if (!isBaritonePresent()) {
                EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.FAILURE));
            ClientAlertUtility.alert(ravex.utility.misc.LanguageUtility.t("autowalk_baritone"), 0xFFFF5555);
                ravex.modules.Module autoWalkMod = ModuleManager.INSTANCE.getByName("AutoWalk");
                if (autoWalkMod != null) autoWalkMod.setEnabled(false);
                return;
            }
            mc.getOptions().keyUp.setDown(true);
            long now = System.currentTimeMillis();
            if (now - lastGotoTime >= (int) baritoneInterval * 1000L) {
                int range = (int) baritoneRange;
                double yaw = Math.toRadians(player.getYRot());
                int x = player.blockPosition().getX() + (int)(-Math.sin(yaw) * range);
                int z = player.blockPosition().getZ() + (int)(Math.cos(yaw) * range);
                try {
                    Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
                    Object provider = apiClass.getMethod("getProvider").invoke(null);
                    Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
                    Object behavior = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
                    Class<?> goalXZClass = Class.forName("baritone.api.pathing.goals.GoalXZ");
                    Object goal = goalXZClass.getConstructor(int.class, int.class).newInstance(x, z);
                    behavior.getClass().getMethod("setGoal", goalXZClass).invoke(behavior, goal);
                } catch (Exception ignored) {
                }
                lastGotoTime = now;
            }
        }
    }

    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        mc.getOptions().keyUp.setDown(false);
        if (mc.getPlayer() != null) mc.getPlayer().setSprinting(false);
        if ("Baritone".equals(mode)) {
            try {
                Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
                Object provider = apiClass.getMethod("getProvider").invoke(null);
                Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
                Object behavior = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
                behavior.getClass().getMethod("cancelEverything").invoke(behavior);
            } catch (Exception ignored) {
            }
        }
    }
}
