package ravex.modules.player.autoregear;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import net.minecraft.world.inventory.ClickType;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ActionParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import ravex.utility.player.InventoryUtility;
import ravex.gui.clickgui.AutoReGearScreen;
import java.util.HashMap;
import java.util.Map;
public class AutoReGear extends Module {
    public static final AutoReGear INSTANCE = new AutoReGear();
    public final NumberParameter delayParam = new NumberParameter("Delay(ms)", 200, 50, 1000, 50);
    public final ActionParameter items = new ActionParameter("Items", () -> {
        Minecraft.getInstance().setScreen(
            new AutoReGearScreen(Minecraft.getInstance().screen)
        );
    });
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_autoregear");
    static {
        NATIVE.load();
    }
    public static native int nativeCalculateRegear(
        String[] containerItemIds,
        int[] containerCounts,
        String[] targetItemIds,
        int[] targetCounts,
        int[] currentCounts
    );
    private long lastActionTime = 0;

    @Override
    public void saveExtra(JsonObject obj) {
        JsonArray arr = new JsonArray();
        for (Map.Entry<String, Integer> entry : AutoReGearData.INSTANCE.getSelectedItems().entrySet()) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("id", entry.getKey());
            itemObj.addProperty("count", entry.getValue());
            arr.add(itemObj);
        }
        obj.add("regearItems", arr);
    }
    @Override
    public void loadExtra(JsonObject obj) {
        if (!obj.has("regearItems")) return;
        AutoReGearData.INSTANCE.clear();
        JsonArray arr = obj.getAsJsonArray("regearItems");
        for (int i = 0; i < arr.size(); i++) {
            JsonObject itemObj = arr.get(i).getAsJsonObject();
            if (itemObj.has("id") && itemObj.has("count")) {
                AutoReGearData.INSTANCE.setTargetCount(
                    itemObj.get("id").getAsString(),
                    itemObj.get("count").getAsInt()
                );
            }
        }
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        if (!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen)) return;
        String title = containerScreen.getTitle().getString().toLowerCase();
        if (!title.contains("shulker") && !title.contains("chest") && !title.contains("box") && !title.contains("barrel")) return;
        long now = System.currentTimeMillis();
        if (now - lastActionTime < delayParam.getValue().longValue()) return;
        var menu = mc.player.containerMenu;
        if (menu == null || menu.slots.size() < 63) return;
        String[] containerItemIds = new String[27];
        int[] containerCounts = new int[27];
        for (int i = 0; i < 27; i++) {
            var slot = menu.getSlot(i);
            var stack = slot.getItem();
            if (!stack.isEmpty()) {
                var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                containerItemIds[i] = id != null ? id.toString() : "";
                containerCounts[i] = stack.getCount();
            } else {
                containerItemIds[i] = "";
                containerCounts[i] = 0;
            }
        }
        var selectedItems = AutoReGearData.INSTANCE.getSelectedItems();
        if (selectedItems.isEmpty()) return;
        String[] targetItemIds = selectedItems.keySet().toArray(new String[0]);
        int[] targetCounts = new int[targetItemIds.length];
        int[] currentCounts = new int[targetItemIds.length];
        Map<String, Integer> currentInventoryCounts = new HashMap<>();
        for (int i = 0; i < targetItemIds.length; i++) {
            targetCounts[i] = selectedItems.get(targetItemIds[i]);
            currentInventoryCounts.put(targetItemIds[i], 0);
        }
        for (int i = 27; i < 63; i++) {
            var slot = menu.getSlot(i);
            var stack = slot.getItem();
            if (!stack.isEmpty()) {
                var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (id != null) {
                    String idStr = id.toString();
                    if (idStr.equals("minecraft:golden_apple") && currentInventoryCounts.containsKey("minecraft:enchanted_golden_apple")) {
                        idStr = "minecraft:enchanted_golden_apple";
                    }
                    if (currentInventoryCounts.containsKey(idStr)) {
                        currentInventoryCounts.put(idStr, currentInventoryCounts.get(idStr) + stack.getCount());
                    }
                }
            }
        }
        for (int i = 0; i < targetItemIds.length; i++) {
            currentCounts[i] = currentInventoryCounts.get(targetItemIds[i]);
        }
        int containerSlotToClick = -1;
        if (NATIVE.isLoaded()) {
            try {
                containerSlotToClick = nativeCalculateRegear(
                    containerItemIds,
                    containerCounts,
                    targetItemIds,
                    targetCounts,
                    currentCounts
                );
            } catch (Exception e) {
                containerSlotToClick = -1;
            }
        }
        if (containerSlotToClick == -1) {
            containerSlotToClick = fallbackCalculateRegear(
                containerItemIds,
                containerCounts,
                targetItemIds,
                targetCounts,
                currentCounts
            );
        }
        if (containerSlotToClick >= 0 && containerSlotToClick < 27) {
            InventoryUtility.handleInventoryClick(mc, (net.minecraft.client.player.LocalPlayer) mc.player, containerSlotToClick, 0, ClickType.QUICK_MOVE);
            lastActionTime = now;
        }
    }
    private int fallbackCalculateRegear(
        String[] containerItemIds,
        int[] containerCounts,
        String[] targetItemIds,
        int[] targetCounts,
        int[] currentCounts
    ) {
        for (int i = 0; i < targetItemIds.length; i++) {
            if (currentCounts[i] < targetCounts[i]) {
                String targetId = targetItemIds[i];
                for (int slot = 0; slot < 27; slot++) {
                    String containerId = containerItemIds[slot];
                    if (containerId.equals(targetId) && containerCounts[slot] > 0) {
                        return slot;
                    }
                    if (targetId.equals("minecraft:enchanted_golden_apple") && containerId.equals("minecraft:golden_apple") && containerCounts[slot] > 0) {
                        return slot;
                    }
                }
            }
        }
        return -1;
    }
}
