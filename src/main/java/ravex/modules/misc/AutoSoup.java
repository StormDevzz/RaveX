package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import ravex.mixin.client.AccessorMinecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
public class AutoSoup extends Module {
    public final NumberParameter health = new NumberParameter("Health", 10.0, 1.0, 20.0, 1.0);
    public final BooleanParameter hotbarOnly = new BooleanParameter("HotbarOnly", true);
    private long lastUse = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        long now = System.currentTimeMillis();
        if (now - lastUse < 500) return;
        if (player.getHealth() + player.getAbsorptionAmount() > health.getValue()) return;
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
    private int findHealingPotion(LocalPlayer player) {
        int end = hotbarOnly.getValue() ? 9 : 36;
        int start = hotbarOnly.getValue() ? 0 : 9;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(player, i);
            if (isHealingPotion(stack)) return i;
        }
        if (hotbarOnly.getValue()) return -1;
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

    public static AutoSoup itz() {
        return ModuleManager.get(AutoSoup.class);
    }
}
