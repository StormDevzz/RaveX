package ravex.modules.player.invclean;
import java.util.LinkedHashSet;
import java.util.Set;
public class InvCleanData {
    public static final InvCleanData INSTANCE = new InvCleanData();
    private final Set<String> selectedItems = new LinkedHashSet<>();
    private InvCleanData() {}
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
