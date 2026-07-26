package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ClickType;

import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
@ModuleInfo(name = "Replenish", category = "Player")
public class Replenish extends ravex.modules.Module {
public final NumberParameter threshold = new NumberParameter("Threshold", 32, 1, 64, 1);
    private long lastActionTime = 0;
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
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Replenish").getEnabled();
    }
    public static Replenish itz() {
        return ravex.manager.ModuleManager.delegate(Replenish.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}