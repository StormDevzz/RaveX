package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ClickType;

import ravex.utility.player.InventoryUtility;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "Replenish", category = "net.minecraft.world.entity.player.Player")
public class Replenish implements ModuleAccess {
    @Parameter(name = "Threshold", min = 1, max = 64, step = 1)
    public double threshold = 32;
    private long lastActionTime = 0;
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        if (mc.screen != null) return;
        long now = System.currentTimeMillis();
        if (now - lastActionTime < 200) return;
        Inventory inv = mc.player.getInventory();
        int thr = (int) threshold;
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
                InventoryUtility.clickSlot(ravex.mcwrapper.MinecraftWrapper.getWrapper(), mc.player, j, 0, ClickType.QUICK_MOVE);
                lastActionTime = now;
                return;
            }
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Replenish").getEnabled();
    }
    public static Replenish itz() {
        return ravex.manager.ModuleManager.delegate(Replenish.class);
    }


}