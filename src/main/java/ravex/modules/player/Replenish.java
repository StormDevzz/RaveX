package ravex.modules.player;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ClickType;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import ravex.utility.player.InventoryUtility;
public class Replenish extends Module {
    public static final Replenish INSTANCE = new Replenish();
    public final NumberParameter threshold = new NumberParameter("Threshold", 32, 1, 64, 1);
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_replenish");
    static {
        NATIVE.load();
    }
    public static native int[] nativeFindReplenish(
        int[] hotbarSlots,
        String[] hotbarItemIds,
        int[] hotbarCounts,
        int[] invSlots,
        String[] invItemIds,
        int[] invCounts,
        int threshold
    );
    private long lastActionTime = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        if (mc.screen != null) return;
        long now = System.currentTimeMillis();
        if (now - lastActionTime < 200) return;
        Inventory inv = mc.player.getInventory();
        int[] hbSlots = new int[9];
        String[] hbIds = new String[9];
        int[] hbCounts = new int[9];
        for (int i = 0; i < 9; i++) {
            hbSlots[i] = i;
            fillItemData(inv.getItem(i), hbIds, hbCounts, i);
        }
        int[] invSlots = new int[27];
        String[] invIds = new String[27];
        int[] invCounts = new int[27];
        for (int i = 9; i < 36; i++) {
            invSlots[i - 9] = i;
            fillItemData(inv.getItem(i), invIds, invCounts, i - 9);
        }
        int thr = threshold.getValue().intValue();
        int[] result = null;
        if (NATIVE.isLoaded()) {
            try {
                result = nativeFindReplenish(hbSlots, hbIds, hbCounts, invSlots, invIds, invCounts, thr);
            } catch (Exception e) {
                result = null;
            }
        }
        if (result == null) {
            result = fallbackFindReplenish(hbSlots, hbIds, hbCounts, invSlots, invIds, invCounts, thr);
        }
        if (result.length >= 3) {
            int invSlot = result[1];
            InventoryUtility.clickSlot(mc, mc.player, invSlot, 0, ClickType.QUICK_MOVE);
            lastActionTime = now;
        }
    }
    private void fillItemData(ItemStack stack, String[] ids, int[] counts, int idx) {
        if (!stack.isEmpty()) {
            Identifier rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
            ids[idx] = rl != null ? rl.toString() : "";
            counts[idx] = stack.getCount();
        } else {
            ids[idx] = "";
            counts[idx] = 0;
        }
    }
    private int[] fallbackFindReplenish(
        int[] hbSlots, String[] hbIds, int[] hbCounts,
        int[] invSlots, String[] invIds, int[] invCounts,
        int thr
    ) {
        for (int i = 0; i < hbSlots.length; i++) {
            if (hbIds[i].isEmpty() || hbCounts[i] >= thr) continue;
            int needed = thr - hbCounts[i];
            if (needed <= 0) continue;
            for (int j = 0; j < invSlots.length; j++) {
                if (invIds[j].isEmpty() || invCounts[j] <= 0) continue;
                if (!invIds[j].equals(hbIds[i])) continue;
                int available = Math.min(invCounts[j], needed);
                return new int[]{hbSlots[i], invSlots[j], available};
            }
        }
        return new int[0];
    }
}
