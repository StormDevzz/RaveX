package ravex.modules.player;

import java.util.LinkedHashSet;
import java.util.Set;


public class InventoryCleanerData {
    public static final InventoryCleanerData INSTANCE = new InventoryCleanerData();

    private final Set<String> selectedItems = new LinkedHashSet<>();

    private InventoryCleanerData() {}

    public Set<String> getSelectedItems() {
        return selectedItems;
    }

    public boolean isSelected(String itemId) {
        return selectedItems.contains(itemId);
    }

    public void toggle(String itemId) {
        if (selectedItems.contains(itemId)) {
            selectedItems.remove(itemId);
        } else {
            selectedItems.add(itemId);
        }
    }

    public void select(String itemId) {
        selectedItems.add(itemId);
    }

    public void deselect(String itemId) {
        selectedItems.remove(itemId);
    }

    public void clear() {
        selectedItems.clear();
    }
}
