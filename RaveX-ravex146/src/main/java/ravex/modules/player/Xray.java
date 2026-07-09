package ravex.modules.player;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ActionParameter;

import java.util.HashSet;
import java.util.Set;

public class Xray extends Module {
    public static final Xray INSTANCE = new Xray();

    public final ActionParameter blocks = new ActionParameter("Blocks", () -> {
        Minecraft.getInstance().setScreen(new ravex.gui.blockbrowser.XRayBlockBrowserScreen(Minecraft.getInstance().screen));
    });

    private final Set<Identifier> selectedBlocks = new HashSet<>();

    private Xray() {
        super("Xray", Category.RENDER);
        addParameter(blocks);
        initDefaultSelected();
    }

    private void initDefaultSelected() {
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.DIAMOND_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.DEEPSLATE_DIAMOND_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.IRON_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.DEEPSLATE_IRON_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.GOLD_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.DEEPSLATE_GOLD_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.EMERALD_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.DEEPSLATE_EMERALD_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.LAPIS_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.DEEPSLATE_LAPIS_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.REDSTONE_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.DEEPSLATE_REDSTONE_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.COAL_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.DEEPSLATE_COAL_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.COPPER_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.DEEPSLATE_COPPER_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.NETHER_QUARTZ_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.NETHER_GOLD_ORE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.ANCIENT_DEBRIS));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.CHEST));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.ENDER_CHEST));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.FURNACE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.CRAFTING_TABLE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.ENCHANTING_TABLE));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.SPAWNER));
        selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.BEDROCK));
    }

    public boolean isBlockSelected(Block block) {
        return selectedBlocks.contains(BuiltInRegistries.BLOCK.getKey(block));
    }

    public void setBlockSelected(Block block, boolean selected) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (selected) {
            selectedBlocks.add(id);
        } else {
            selectedBlocks.remove(id);
        }
    }

    public boolean isBlockSelected(Identifier id) {
        return selectedBlocks.contains(id);
    }

    public Set<Identifier> getSelectedBlocks() {
        return selectedBlocks;
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }
}
