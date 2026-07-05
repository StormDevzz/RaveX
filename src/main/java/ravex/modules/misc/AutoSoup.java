package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import ravex.mixin.client.AccessorMinecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
public class AutoSoup extends Module {
    public static final AutoSoup INSTANCE = new AutoSoup();
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
        int prevSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(potionSlot);
        ((AccessorMinecraft) mc).invokeStartUseItem();
        lastUse = now;
        if (prevSlot != potionSlot) {
            player.getInventory().setSelectedSlot(prevSlot);
        }
    }
    private int findHealingPotion(LocalPlayer player) {
        int end = hotbarOnly.getValue() ? 9 : 36;
        int start = hotbarOnly.getValue() ? 0 : 9;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isHealingPotion(stack)) return i;
        }
        if (hotbarOnly.getValue()) return -1;
        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isHealingPotion(stack)) return i;
        }
        return -1;
    }
    private boolean isHealingPotion(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() != Items.POTION && stack.getItem() != Items.SPLASH_POTION) return false;
        PotionContents contents = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents != null
            && contents.potion().isPresent()
            && (contents.potion().get() == Potions.HEALING
             || contents.potion().get() == Potions.STRONG_HEALING);
    }
}
