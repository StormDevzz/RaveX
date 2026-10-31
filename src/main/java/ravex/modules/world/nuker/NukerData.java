package ravex.modules.world.nuker;
import java.util.LinkedHashSet;
import java.util.Set;
public class NukerData {
    public static final NukerData INSTANCE = new NukerData();
    // ids stored as namespace:path strings
    private final Set<String> selectedBlocks = new LinkedHashSet<>();
    private NukerData() {
        selectDefault();
    }
    private void selectDefault() {
        selectedBlocks.add("minecraft:stone");
        selectedBlocks.add("minecraft:cobblestone");
        selectedBlocks.add("minecraft:dirt");
        selectedBlocks.add("minecraft:grass_block");
        selectedBlocks.add("minecraft:gravel");
        selectedBlocks.add("minecraft:sand");
        selectedBlocks.add("minecraft:oak_log");
        selectedBlocks.add("minecraft:oak_leaves");
    }
    public Set<String> getSelectedBlocks() {
        return selectedBlocks;
    }
    public boolean isSelected(String id) {
        return id != null && selectedBlocks.contains(id);
    }
    public boolean isSelected(net.minecraft.resources.Identifier id) {
        return id != null && isSelected(id.toString());
    }
    public void toggle(String id) {
        if (id == null) return;
        if (!selectedBlocks.remove(id)) {
            selectedBlocks.add(id);
        }
    }
    public void toggle(net.minecraft.resources.Identifier id) {
        if (id != null) toggle(id.toString());
    }
    public void select(String id) {
        if (id != null) selectedBlocks.add(id);
    }
    public void deselect(String id) {
        selectedBlocks.remove(id);
    }
    public void clear() {
        selectedBlocks.clear();
    }
}
