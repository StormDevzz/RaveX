package ravex.modules.movement;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.BooleanParameter;
import net.minecraft.client.Minecraft;
import java.util.List;

public class AutoWalk extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Simple", List.of("Simple", "Baritone"));
    public final NumberParameter baritoneInterval = new NumberParameter("Interval", 30.0, 5.0, 120.0, 5.0);
    public final NumberParameter baritoneRange = new NumberParameter("Range", 2000.0, 100.0, 10000.0, 100.0);
    public final BooleanParameter silentMode = new BooleanParameter("SilentMode", true);

    {
        baritoneInterval.setVisible(() -> "Baritone".equals(mode.getValue()));
        baritoneRange.setVisible(() -> "Baritone".equals(mode.getValue()));
        silentMode.setVisible(() -> "Baritone".equals(mode.getValue()));
    }

    private long lastGotoTime = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String m = mode.getValue();
        if ("Simple".equals(m)) {
            mc.options.keyUp.setDown(true);
        } else if ("Baritone".equals(m)) {
            mc.options.keyUp.setDown(true);
            long now = System.currentTimeMillis();
            if (now - lastGotoTime >= baritoneInterval.getValue().intValue() * 1000L) {
                int range = baritoneRange.getValue().intValue();
                double yaw = Math.toRadians(mc.player.getYRot());
                int x = mc.player.blockPosition().getX() + (int)(-Math.sin(yaw) * range);
                int z = mc.player.blockPosition().getZ() + (int)(Math.cos(yaw) * range);
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

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.keyUp.setDown(false);
        if ("Baritone".equals(mode.getValue())) {
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
    public static AutoWalk itz() {
        return ModuleManager.get(AutoWalk.class);
    }
}
