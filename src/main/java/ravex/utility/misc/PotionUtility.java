package ravex.utility.misc;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
public class PotionUtility {
    public static String getPotionName(ItemStack stack) {
        if (stack.isEmpty()) return "Unknown";
        PotionContents contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (contents == null) {
            var name = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (name != null) {
                String path = name.getPath();
                if (path.equals("potion")) return "§fWater Bottle";
                if (path.equals("splash_potion")) return "§fSplash Water Bottle";
                if (path.equals("lingering_potion")) return "§fLingering Water Bottle";
                return "§f" + path.replace("_", " ");
            }
            return "§fUnknown";
        }
        var potionHolder = contents.potion();
        if (potionHolder != null && potionHolder.isPresent()) {
            Holder<Potion> holder = potionHolder.get();
            Identifier id = BuiltInRegistries.POTION.getKey(holder.value());
            String path = id != null ? id.getPath() : "unknown";
            String name = path.replace("_", " ");
            if (stack.is(Items.SPLASH_POTION)) return "§dSplash " + name;
            if (stack.is(Items.LINGERING_POTION)) return "§dLingering " + name;
            return "§d" + name;
        }
        return "§fUnknown Potion";
    }
}
