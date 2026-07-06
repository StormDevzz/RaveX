package ravex.modules.player;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ClickType;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
public class Replenish extends Module {
    public static final Replenish INSTANCE = new Replenish();
    public final NumberParameter threshold = new NumberParameter("Threshold", 32, 1, 64, 1);
    private long lastActionTime = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        if (mc.screen != null) return;
        long now = System.currentTimeMillis();
        if (now - lastActionTime < 200) return;
        Inventory inv = mc.player.getInventory();
        int thr = threshold.getValue().intValue();
        for (int i = 0; i < 9; i++) {
            var stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            int count = stack.getCount();
            if (count >= thr) continue;
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id == null) continue;
            String targetId = id.toString();
            int needed = thr - count;
            for (int j = 9; j < 36; j++) {
                var invStack = inv.getItem(j);
                if (invStack.isEmpty()) continue;
                Identifier invId = BuiltInRegistries.ITEM.getKey(invStack.getItem());
                if (invId == null || !invId.toString().equals(targetId)) continue;
                int available = Math.min(invStack.getCount(), needed);
                if (available <= 0) continue;
                InventoryUtility.clickSlot(mc, mc.player, j, 0, ClickType.QUICK_MOVE);
                lastActionTime = now;
                return;
            }
        }
    }
}
