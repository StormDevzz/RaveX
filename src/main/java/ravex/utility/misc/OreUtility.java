package ravex.utility.misc;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.HashSet;
import java.util.Set;

public class OreUtility {
    private static final Set<Identifier> DEFAULT_XRAY = new HashSet<>();

    static {
        add(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        add(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        add(Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE);
        add(Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        add(Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        add(Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        add(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        add(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        add(Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE, Blocks.ANCIENT_DEBRIS);
        add(Blocks.CHEST, Blocks.ENDER_CHEST, Blocks.FURNACE, Blocks.CRAFTING_TABLE);
        add(Blocks.ENCHANTING_TABLE, Blocks.SPAWNER, Blocks.BEDROCK);
    }

    private static void add(Block... blocks) {
        for (Block b : blocks)
            DEFAULT_XRAY.add(BuiltInRegistries.BLOCK.getKey(b));
    }

    public static Set<Identifier> getDefaultXrayBlocks() {
        return new HashSet<>(DEFAULT_XRAY);
    }

    public static Identifier getIdentifier(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }
}
