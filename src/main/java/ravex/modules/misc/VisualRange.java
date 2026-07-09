package ravex.modules.misc;
import ravex.modules.Category;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
public class VisualRange extends Module {
    public static final VisualRange INSTANCE = new VisualRange();
    private final List<String> knownPlayers = new ArrayList<>();

    @Override
    protected void onEnable() {
        knownPlayers.clear();
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        List<String> currentPlayers = new ArrayList<>();
        for (Player p : mc.level.players()) {
            if (p == mc.player) continue;
            String name = p.getName().getString();
            currentPlayers.add(name);
            if (!knownPlayers.contains(name)) {
                mc.player.displayClientMessage(Component.literal("§7[§cRaveX§7] §f" + name + " §7entered visual range!"), false);
            }
        }
        for (String name : knownPlayers) {
            if (!currentPlayers.contains(name)) {
                mc.player.displayClientMessage(Component.literal("§7[§cRaveX§7] §f" + name + " §7left visual range!"), false);
            }
        }
        knownPlayers.clear();
        knownPlayers.addAll(currentPlayers);
    }
}
