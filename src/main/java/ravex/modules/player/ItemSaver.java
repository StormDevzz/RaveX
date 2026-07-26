package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;



@ModuleInfo(name = "ItemSaver", category = "net.minecraft.world.entity.player.Player")
public class ItemSaver implements ModuleAccess {
    @Parameter(name = "MinDurability", min = 1.0, max = 50.0, step = 1.0)
    public double threshold = 10.0;
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.player.LocalPlayer p = mc.player;
        if (p == null) return;
        ItemStack mainHand = InventoryUtility.getMainHand(p);
        if (shouldSave(mainHand)) {
            int safeSlot = InventoryUtility.findSlot(p, s -> !shouldSave(s), 0, 9);
            if (safeSlot != -1)
                InventoryUtility.selectSlot(p, safeSlot);
        }
    }
    public boolean shouldSave(ItemStack stack) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("ItemSaver").getEnabled()) return false;
        if (stack.isEmpty() || !stack.isDamageableItem()) return false;
        return (stack.getMaxDamage() - stack.getDamageValue()) <= (int) threshold;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ItemSaver").getEnabled();
    }
    public static ItemSaver itz() {
        return ravex.manager.ModuleManager.delegate(ItemSaver.class);
    }


}