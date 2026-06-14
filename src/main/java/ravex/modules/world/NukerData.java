package ravex.modules.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashSet;
import java.util.Set;

public class NukerData {
    public static final NukerData INSTANCE = new NukerData();

    private final Set<Identifier> selectedBlocks = new LinkedHashSet<>();

    private NukerData() {
        selectDefault();
    }

    private void selectDefault() {
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.STONE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.COBBLESTONE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.GRAVEL));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.SAND));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.OAK_LOG));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.OAK_LEAVES));
    }

    public Set<Identifier> getSelectedBlocks() {
        return selectedBlocks;
    }

    public boolean isSelected(Block block) {
        return selectedBlocks.contains(BuiltInRegistries.BLOCK.getKey(block));
    }

    public boolean isSelected(Identifier id) {
        return selectedBlocks.contains(id);
    }

    public void toggle(Identifier id) {
        if (selectedBlocks.contains(id)) {
            selectedBlocks.remove(id);
        } else {
            selectedBlocks.add(id);
        }
    }

    public void select(Identifier id) {
        selectedBlocks.add(id);
    }

    public void deselect(Identifier id) {
        selectedBlocks.remove(id);
    }

    public void clear() {
        selectedBlocks.clear();
    }
}
