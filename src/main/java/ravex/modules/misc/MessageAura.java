package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ravex.manager.ModuleManager;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import ravex.parameter.StringParameter;

public class MessageAura extends Module {
    public final StringParameter message = new StringParameter("Message", "Hello from RaveX!");
    public final NumberParameter interval = new NumberParameter("Interval", 5.0, 1.0, 60.0, 0.5);

    private long lastMessageTime;

    public MessageAura() {
        super("MessageAura");
        setCategory(Category.MISC);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (System.currentTimeMillis() - lastMessageTime >= interval.getValue() * 1000) {
            mc.player.displayClientMessage(Component.literal(message.getValue()), false);
            lastMessageTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void onEnable() {
        lastMessageTime = System.currentTimeMillis();
    }

    public static MessageAura itz() {
        return ModuleManager.get(MessageAura.class);
    }
}
