package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
<<<<<<< HEAD
import ravex.event.Subscribe;
import ravex.event.combat.TotemPopEvent;
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import java.util.HashMap;
import java.util.Map;
public class PopCounter extends Module {
<<<<<<< HEAD
    public final BooleanParameter onlyOwn = new BooleanParameter("OnlyOwn", false);
    private final Map<String, Integer> popCounts = new HashMap<>();

    @Subscribe
    public void onTotemPop(TotemPopEvent event) {
        onPop(event.getPlayer());
    }

=======
    public static final PopCounter INSTANCE = new PopCounter();
    public final BooleanParameter onlyOwn = new BooleanParameter("OnlyOwn", false);
    private final Map<String, Integer> popCounts = new HashMap<>();

>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public void onPop(Player player) {
        if (!getEnabled()) return;
        if (player == Minecraft.getInstance().player && !onlyOwn.getValue()) return;
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
<<<<<<< HEAD

    public static PopCounter itz() {
        return ModuleManager.get(PopCounter.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
