package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.SwingUtility;
import ravex.utility.player.InventoryUtility;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "KeyPearl", category = "Combat")
public class KeyPearl {
    @Parameter(name = "Swap", modes = {"Silent", "Normal"})
    public String swap = "Silent";
    public void onEnable() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        int pearlSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(player, i);
            if (InventoryUtility.isItem(stack, "ender_pearl")) { pearlSlot = i; break; }
        }
        if (pearlSlot == -1) return;
        int prevSlot = InventoryUtility.getSelectedSlot(player);
        InventoryUtility.selectSlot(player, pearlSlot);
        var gm = mc.getGameMode();
        if (gm != null) {
            gm.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        if ("Silent".equals(swap)) {
            InventoryUtility.selectSlot(player, prevSlot);
        }
        Modules.setEnabled(KeyPearl.class, false);
    }
}
