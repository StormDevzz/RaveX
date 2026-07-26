package ravex.modules.misc;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import ravex.parameter.NumberParameter;
import ravex.parameter.StringParameter;

@ModuleInfo(name = "MessageAura", category = "Misc")
public class MessageAura extends ravex.modules.Module {
public final StringParameter message = new StringParameter("Message", "Hello from RaveX!");
    public final NumberParameter interval = new NumberParameter("Interval", 5.0, 1.0, 60.0, 0.5);

    private long lastMessageTime;

    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (System.currentTimeMillis() - lastMessageTime >= interval.getValue() * 1000) {
            mc.player.displayClientMessage(Component.literal(message.getValue()), false);
            lastMessageTime = System.currentTimeMillis();
        }
    }
    protected void onEnable() {
        lastMessageTime = System.currentTimeMillis();
    }

    public static MessageAura itz() {
        return ravex.manager.ModuleManager.delegate(MessageAura.class);
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