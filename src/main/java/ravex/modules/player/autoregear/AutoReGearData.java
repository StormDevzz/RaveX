package ravex.modules.player.autoregear;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
public class AutoReGearData {
    public static final AutoReGearData INSTANCE = new AutoReGearData();
    private final Map<String, Integer> selectedItems = new LinkedHashMap<>();
    private AutoReGearData() {
        selectedItems.put("minecraft:totem_of_undying", 6);
        selectedItems.put("minecraft:end_crystal", 64);
        selectedItems.put("minecraft:enchanted_golden_apple", 64);
        selectedItems.put("minecraft:obsidian", 64);
        selectedItems.put("minecraft:experience_bottle", 64);
        selectedItems.put("minecraft:ender_pearl", 16);
    }
    public Map<String, Integer> getSelectedItems() {
        return selectedItems;
    }
    public boolean isSelected(String itemId) {
        return selectedItems.containsKey(itemId);
    }
    public int getTargetCount(String itemId) {
        return selectedItems.getOrDefault(itemId, 0);
    }
    public void setTargetCount(String itemId, int count) {
        if (count <= 0) {
            selectedItems.remove(itemId);
        } else {
            selectedItems.put(itemId, count);
        }
    }
    public void toggle(String itemId, int defaultMax) {
        if (selectedItems.containsKey(itemId)) {
            selectedItems.remove(itemId);
        } else {
            selectedItems.put(itemId, defaultMax);
        }
    }
    public void deselect(String itemId) {
        selectedItems.remove(itemId);
    }
    public void clear() {
        selectedItems.clear();
    }
}
