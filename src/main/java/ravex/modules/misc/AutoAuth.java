package ravex.modules.misc;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;

import ravex.parameter.NumberParameter;
import ravex.parameter.StringParameter;
@ModuleInfo(name = "AutoAuth", category = "Misc")
public class AutoAuth extends ravex.modules.Module {
public final StringParameter password = new StringParameter("Password", "r1v2x");
    public final NumberParameter delay = new NumberParameter("Delay", 3.0, 0.0, 20.0, 1.0);
    private int tickCounter = 0;
    private boolean hasRegistered = false;
    protected void onEnable() { tickCounter = 0; hasRegistered = false; }
    protected void onDisable() { hasRegistered = false; }
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

    public static AutoAuth itz() {
        return ravex.manager.ModuleManager.delegate(AutoAuth.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}