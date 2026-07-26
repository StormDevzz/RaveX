package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ravex.utility.misc.EntityUtility;
import ravex.event.Subscribe;
import ravex.event.combat.TotemPopEvent;

import java.util.HashMap;
import java.util.Map;
@ModuleInfo(name = "PopCounter", category = "Misc")
public class PopCounter implements ModuleAccess {
    @Parameter(name = "OnlyOwn")
    public boolean onlyOwn = false;
    private final Map<String, Integer> popCounts = new HashMap<>();

    @Subscribe
    public void onTotemPop(TotemPopEvent event) {
        onPop(event.getPlayer());
    }

    public void onPop(net.minecraft.world.entity.player.Player player) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("PopCounter").getEnabled()) return;
        if (player == Minecraft.getInstance().player && !onlyOwn) return;
        if (player == Minecraft.getInstance().player) return;
        String name = player.getName().getString();
        int count = popCounts.getOrDefault(name, 1);
        if (count == 1) {
            popCounts.put(name, 2);
        } else {
            popCounts.put(name, count + 1);
        }
        String msg = String.format("§7[§6PopCounter§7] §e%s §7just popped §6%d §7%s",
                name, count, count == 1 ? "totem" : "totems");
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(msg), false);
        }
    }

    public static PopCounter itz() {
        return ravex.manager.ModuleManager.delegate(PopCounter.class);
    }


}