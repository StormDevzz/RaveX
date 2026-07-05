package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import ravex.parameter.StringParameter;
public class AutoAuth extends Module {
    public static final AutoAuth INSTANCE = new AutoAuth();
    public final StringParameter password = new StringParameter("Password", "r1v2x");
    public final NumberParameter delay = new NumberParameter("Delay", 3.0, 0.0, 20.0, 1.0);
    private int tickCounter = 0;
    private boolean hasRegistered = false;

    @Override
    protected void onEnable() { tickCounter = 0; hasRegistered = false; }
    @Override
    protected void onDisable() { hasRegistered = false; }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        if (hasRegistered) return;
        tickCounter++;
        if (tickCounter >= delay.getValue().intValue() * 20) {
            String pw = password.getValue();
            if (pw.isEmpty()) pw = "r1v2x";
            mc.player.connection.sendCommand("register " + pw + " " + pw);
            mc.player.connection.sendCommand("login " + pw);
            hasRegistered = true;
        }
    }
}
