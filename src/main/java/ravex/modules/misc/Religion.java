package ravex.modules.misc;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import ravex.parameter.BooleanParameter;
@ModuleInfo(name = "Religion", category = "Misc")
public class Religion extends ravex.modules.Module {
public final BooleanParameter christianity = new BooleanParameter("Christianity", false);
    public final BooleanParameter atheism = new BooleanParameter("Atheism", false);
    public final BooleanParameter islam = new BooleanParameter("Islam", false);
    public final BooleanParameter buddhism = new BooleanParameter("Buddhism", false);
    public final BooleanParameter hinduism = new BooleanParameter("Hinduism", false);
    public final BooleanParameter suka = new BooleanParameter("Suka", false);
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        LocalPlayer player = mc.player;
        if (player == null || player.connection == null) return;
        if (christianity.getValue()) {
            player.connection.sendChat("Amen");
        } else if (atheism.getValue()) {
            player.connection.sendChat("Nothing");
        } else if (islam.getValue()) {
            player.connection.sendChat("AllahuAkbar");
        } else if (buddhism.getValue()) {
            player.connection.sendChat("OmManiPadmeHum");
        } else if (hinduism.getValue()) {
            player.connection.sendChat("Hare Krishna");
        } else if (suka.getValue()) {
            player.connection.sendChat("Suka");
        }

    }

    public static Religion itz() {
        return ravex.manager.ModuleManager.delegate(Religion.class);
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