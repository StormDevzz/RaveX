package ravex.modules.player;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
public class ItemSaver extends Module {
    public final NumberParameter threshold = new NumberParameter("MinDurability", 10.0, 1.0, 50.0, 1.0);

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        ItemStack mainHand = InventoryUtility.getMainHand(p);
        if (shouldSave(mainHand)) {
            int safeSlot = InventoryUtility.findSlot(p, s -> !shouldSave(s), 0, 9);
            if (safeSlot != -1)
                InventoryUtility.selectSlot(p, safeSlot);
        }
    }
    public boolean shouldSave(ItemStack stack) {
        if (!getEnabled()) return false;
        if (stack.isEmpty() || !stack.isDamageableItem()) return false;
        return (stack.getMaxDamage() - stack.getDamageValue()) <= threshold.getValue().intValue();
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(ItemSaver.class);
    }
    public static ItemSaver itz() {
        return ModuleManager.get(ItemSaver.class);
    }
}
