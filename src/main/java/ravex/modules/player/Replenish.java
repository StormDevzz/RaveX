package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.InventoryUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "Replenish", category = "Player")
public class Replenish {
    @Parameter(name = "Threshold", min = 1, max = 64, step = 1)
    public double threshold = 32;
    // legit: moves the items inside the inventory screen
    @Parameter(name = "Legit")
    public boolean legit = false;

    private long lastActionTime = 0;

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getGameMode() == null) return;

        if (!legit && mc.getCurrentScreen() != null) return;
        // slot ids differ inside other menus
        if (legit && mc.getCurrentScreen() != null
                && !(mc.getCurrentScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;
        long now = System.currentTimeMillis();
        if (now - lastActionTime < 100) return;
        var inv = player.getInventory();
        int thr = (int) threshold;
        for (int i = 0; i < 9; i++) {
            var stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            int count = stack.getCount();
            if (count >= thr) continue;
            String targetId = InventoryUtility.getItemId(stack);
            if (targetId == null) continue;
            // single source: the biggest stack
            int source = -1;
            int sourceCount = 0;
            for (int j = 9; j < 36; j++) {
                var invStack = inv.getItem(j);
                if (invStack.isEmpty()) continue;
                String id = InventoryUtility.getItemId(invStack);
                if (id == null || !id.equals(targetId)) continue;
                if (invStack.getCount() > sourceCount) {
                    sourceCount = invStack.getCount();
                    source = j;
                }
            }
            if (source == -1) continue;

            int needed = thr - count;
            // legit only shows the screen
            if (legit && mc.getCurrentScreen() == null) {
                InventoryUtility.openInventoryScreen(player);
            }
            lastActionTime = now;
            if (sourceCount <= needed) {
                InventoryUtility.clickSlot(mc, player, source, 0, InventoryUtility.QUICK_MOVE);
            } else {
                // exact fill: pick up, click the missing count over
                int from = InventoryUtility.inventorySlotToContainerSlot(source);
                int to = InventoryUtility.inventorySlotToContainerSlot(i);
                InventoryUtility.clickSlot(mc, player, source, 0, InventoryUtility.PICKUP);
                for (int k = 0; k < needed; k++) {
                    // right click places exactly one item
                    InventoryUtility.handleInventoryClick(mc, player, to, 1, InventoryUtility.PICKUP);
                }
                InventoryUtility.handleInventoryClick(mc, player, from, 0, InventoryUtility.PICKUP);
            }
            return;
        }
    }
}
