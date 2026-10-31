package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.InventoryUtility;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "BlockMixer", category = "Misc")
public class BlockMixer {
    @Parameter(name = "Swap", modes = {"Normal", "Silent"})
    public String swap = "Normal";
    private static final Random RANDOM = new Random();

    // picks a random hotbar block, no shuffling
    public void shuffle() {
        if (!Modules.enabled(BlockMixer.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        var screen = mc.getCurrentScreen();
        if (screen != null && !(screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;
        // stale creative menu has id 0 but different slots
        if (player.containerMenu != player.inventoryMenu && player.containerMenu.containerId == 0) {
            player.containerMenu = player.inventoryMenu;
        }
        List<Integer> blockSlots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.isBlockItem(InventoryUtility.getItem(player, i))) {
                blockSlots.add(i);
            }
        }
        if (blockSlots.isEmpty()) return;
        int target = blockSlots.get(RANDOM.nextInt(blockSlots.size()));
        if ("Silent".equals(swap)) {
            InventoryUtility.silentSelectSlot(player, target);
        } else {
            InventoryUtility.selectSlot(player, target);
        }
    }
}
