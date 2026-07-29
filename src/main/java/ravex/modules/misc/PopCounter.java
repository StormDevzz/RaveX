package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.chat.Component;
import ravex.utility.misc.EntityUtility;
import ravex.event.Subscribe;
import ravex.event.combat.TotemPopEvent;
import java.util.HashMap;
import java.util.Map;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "PopCounter", category = "Misc")
public class PopCounter {
    @Parameter(name = "OnlyOwn")
    public boolean onlyOwn = false;
    private final Map<String, Integer> popCounts = new HashMap<>();

    @Subscribe
    public void onTotemPop(TotemPopEvent event) {
        onPop(event.getPlayer());
    }

    public void onPop(net.minecraft.world.entity.player.Player player) {
        if (!Modules.enabled(PopCounter.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        var self = mc.getPlayer();
        if (player == self && !onlyOwn) return;
        if (player == self) return;
        String name = player.getName().getString();
        int count = popCounts.getOrDefault(name, 1);
        if (count == 1) {
            popCounts.put(name, 2);
        } else {
            popCounts.put(name, count + 1);
        }
        String msg = String.format("§7[§6PopCounter§7] §e%s §7just popped §6%d §7%s",
                name, count, count == 1 ? "totem" : "totems");
        if (self != null) {
            self.displayClientMessage(Component.literal(msg), false);
        }
    }
}
