package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.chat.Component;

import ravex.parameter.StringParameter;
import ravex.mcwrapper.MinecraftWrapper;

@ModuleInfo(name = "MessageAura", category = "Misc")
public class MessageAura implements ModuleAccess {
    @Parameter(name = "Message")
    public String message = "Hello from RaveX!";
    @Parameter(name = "Interval", min = 1.0, max = 60.0, step = 0.5)
    public double interval = 5.0;

    private long lastMessageTime;

    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (System.currentTimeMillis() - lastMessageTime >= interval * 1000) {
            mc.player.displayClientMessage(Component.literal(message), false);
            lastMessageTime = System.currentTimeMillis();
        }
    }
    public void onEnable() {
        lastMessageTime = System.currentTimeMillis();
    }

    public static MessageAura itz() {
        return ravex.manager.ModuleManager.delegate(MessageAura.class);
    }


}