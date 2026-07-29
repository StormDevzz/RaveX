package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.InventoryUtility;
import net.minecraft.world.item.ItemStack;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "ItemSaver", category = "net.minecraft.world.entity.player.Player")
public class ItemSaver {
    @Parameter(name = "MinDurability", min = 1.0, max = 50.0, step = 1.0)
    public double threshold = 10.0;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null) return;
        ItemStack mainHand = InventoryUtility.getMainHand(p);
        if (shouldSave(mainHand)) {
            int safeSlot = InventoryUtility.findSlot(p, s -> !shouldSave(s), 0, 9);
            if (safeSlot != -1)
                InventoryUtility.selectSlot(p, safeSlot);
        }
    }
    public boolean shouldSave(ItemStack stack) {
        if (!Modules.enabled(ItemSaver.class)) return false;
        if (stack.isEmpty() || !stack.isDamageableItem()) return false;
        return (stack.getMaxDamage() - stack.getDamageValue()) <= (int) threshold;
    }
}
