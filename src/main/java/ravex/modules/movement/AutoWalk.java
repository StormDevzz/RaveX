package ravex.modules.movement;
import ravex.integrations.baritone.BaritoneIntegration;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import java.util.List;
import java.util.Random;
public class AutoWalk extends Module {
    public static final AutoWalk INSTANCE = new AutoWalk();
    public final ModeParameter mode = new ModeParameter("Mode", "Simple", List.of("Simple", "Baritone"));
    public final NumberParameter baritoneInterval = new NumberParameter("Interval", 30.0, 5.0, 120.0, 5.0);
    public final NumberParameter baritoneRange = new NumberParameter("Range", 2000.0, 100.0, 10000.0, 100.0);

    {
        baritoneInterval.setVisible(() -> "Baritone".equals(mode.getValue()));
        baritoneRange.setVisible(() -> "Baritone".equals(mode.getValue()));
    }
    private final Random random = new Random();
    private long lastGotoTime = 0;

    private boolean baritoneChecked = false;
    private boolean baritoneAvailable = false;

    @Override
    protected void onEnable() {
        if ("Baritone".equals(mode.getValue())) {
            baritoneChecked = false;
            baritoneAvailable = false;
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String m = mode.getValue();
        if ("Simple".equals(m)) {
            mc.options.keyUp.setDown(true);
        } else if ("Baritone".equals(m)) {
            if (!baritoneChecked) {
                baritoneAvailable = BaritoneIntegration.isBaritonePresent();
                baritoneChecked = true;
                if (!baritoneAvailable) {
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§7[§5AutoWalk§7] §cBaritone not found, switching to Simple"), false);
                    mode.setValue("Simple");
                    return;
                }
            }
            mc.options.keyUp.setDown(true);
            long now = System.currentTimeMillis();
            if (now - lastGotoTime >= baritoneInterval.getValue().intValue() * 1000L) {
                int range = baritoneRange.getValue().intValue();
                int x = mc.player.blockPosition().getX() + random.nextInt(range * 2 + 1) - range;
                int z = mc.player.blockPosition().getZ() + random.nextInt(range * 2 + 1) - range;
                mc.player.connection.sendChat("#goto " + x + " " + z);
                lastGotoTime = now;
            }
        }
    }
    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.keyUp.setDown(false);
    }
}
