package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.InventoryUtility;
import net.minecraft.world.item.ItemStack;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "ItemSaver", category = "Player")
public class ItemSaver {
    @Parameter(name = "Durability", min = 1.0, max = 100.0, step = 1.0)
    public double threshold = 10.0;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null) return;
        ItemStack mainHand = InventoryUtility.getMainHand(p);
        if (shouldSave(mainHand)) {
            int current = InventoryUtility.getSelectedSlot(p);
            int safeSlot = -1;
            for (int offset = 1; offset < 9; offset++) {
                int candidate = current + offset;
                if (candidate < 9 && !shouldSave(InventoryUtility.getItem(p, candidate))) {
                    safeSlot = candidate;
                    break;
                }
                candidate = current - offset;
                if (candidate >= 0 && !shouldSave(InventoryUtility.getItem(p, candidate))) {
                    safeSlot = candidate;
                    break;
                }
            }
            if (safeSlot == -1)
                safeSlot = InventoryUtility.findSlot(p, s -> !shouldSave(s), 0, 9);
            if (safeSlot != -1 && safeSlot != current)
                InventoryUtility.selectSlot(p, safeSlot);
        }
    }
    public boolean shouldSave(ItemStack stack) {
        if (!Modules.enabled(ItemSaver.class)) return false;
        if (stack.isEmpty() || !stack.isDamageableItem()) return false;
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) return false;
        double remainingPercent = (maxDamage - stack.getDamageValue()) * 100.0 / maxDamage;
        return remainingPercent <= threshold;
    }
}
