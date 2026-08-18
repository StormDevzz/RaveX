package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.chat.Component;

import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "MessageAura", category = "Misc")
public class MessageAura {
    @Parameter(name = "Message")
    public String message = "Hello from RaveX!";
    @Parameter(name = "Interval", min = 1.0, max = 60.0, step = 0.5)
    public double interval = 5.0;

    private long lastMessageTime;

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;

        if (System.currentTimeMillis() - lastMessageTime >= interval * 1000) {
            mc.getPlayer().displayClientMessage(Component.literal(message), false);
            lastMessageTime = System.currentTimeMillis();
        }
    }
    public void onEnable() {
        lastMessageTime = System.currentTimeMillis();
    }




}