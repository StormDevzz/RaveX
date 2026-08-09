package ravex.modules.misc;
import ravex.mixin.client.AccessorMinecraft;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.InventoryUtility;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import ravex.mcwrapper.MinecraftWrapper;




@Module(name = "AutoSoup", category = "Misc")
public class AutoSoup {
    @Parameter(name = "Health", min = 1.0, max = 20.0, step = 1.0)
    public double health = 10.0;
    @Parameter(name = "HotbarOnly")
    public boolean hotbarOnly = true;
    private long lastUse = 0;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        net.minecraft.client.player.LocalPlayer player = mc.getPlayer();
        if (player == null) return;
        long now = System.currentTimeMillis();
        if (now - lastUse < 500) return;
        if (player.getHealth() + player.getAbsorptionAmount() > health) return;
        if (!player.getMainHandItem().isEmpty() && !isHealingPotion(player.getMainHandItem())) return;
        int potionSlot = findHealingPotion(player);
        if (potionSlot == -1) return;
        int prevSlot = InventoryUtility.getSelectedSlot(player);
        InventoryUtility.selectSlot(player, potionSlot);
        ((AccessorMinecraft) mc).invokeStartUseItem();
        lastUse = now;
        if (prevSlot != potionSlot) {
            InventoryUtility.selectSlot(player, prevSlot);
        }
    }
    private int findHealingPotion(net.minecraft.client.player.LocalPlayer player) {
        int end = hotbarOnly ? 9 : 36;
        int start = hotbarOnly ? 0 : 9;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(player, i);
            if (isHealingPotion(stack)) return i;
        }
        if (hotbarOnly) return -1;
        for (int i = 9; i < 36; i++) {
            var stack = InventoryUtility.getItem(player, i);
            if (isHealingPotion(stack)) return i;
        }
        return -1;
    }
    private boolean isHealingPotion(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!InventoryUtility.isPotion(stack) && !InventoryUtility.isItem(stack, "splash_potion")) return false;
        PotionContents contents = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents != null
            && contents.potion().isPresent()
            && (contents.potion().get() == Potions.HEALING
             || contents.potion().get() == Potions.STRONG_HEALING);
    }




}