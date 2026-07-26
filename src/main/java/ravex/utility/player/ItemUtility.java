package ravex.utility.player;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class ItemUtility {
    public static boolean isSeed(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("minecraft") &&
            (id.getPath().endsWith("_seeds") || id.getPath().contains("seed"));
    }

    public static boolean isItem(ItemStack stack, String name) {
        if (stack == null || stack.isEmpty()) return false;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && (id.getPath().equals(name) || id.toString().equals(name));
    }

    public static boolean isPickaxe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_pickaxe");
    }

    public static boolean isSword(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_sword");
    }

    public static boolean isTool(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.endsWith("_pickaxe") || path.endsWith("_axe") || path.endsWith("_shovel") || path.endsWith("_hoe") || path.endsWith("_sword");
    }

    public static boolean isBrewIngredient(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String name = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return name.equals("nether_wart") || name.equals("glowstone_dust") || name.equals("redstone")
            || name.equals("gunpowder") || name.equals("fermented_spider_eye") || name.equals("blaze_powder")
            || name.equals("ghast_tear") || name.equals("magma_cream") || name.equals("sugar")
            || name.equals("rabbit_foot") || name.equals("glistering_melon_slice") || name.equals("golden_carrot")
            || name.equals("spider_eye") || name.equals("pufferfish") || name.equals("phantom_membrane")
            || name.equals("dragon_breath") || name.equals("turtle_helmet");
    }

    public static String getItemName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null ? id.getPath() : "";
    }
}
