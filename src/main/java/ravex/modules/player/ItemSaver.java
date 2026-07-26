package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
@ModuleInfo(name = "ItemSaver", category = "net.minecraft.world.entity.player.Player")
public class ItemSaver extends ravex.modules.Module {
public final NumberParameter threshold = new NumberParameter("MinDurability", 10.0, 1.0, 50.0, 1.0);
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
        return ravex.manager.ModuleManager.INSTANCE.getByName("ItemSaver").getEnabled();
    }
    public static ItemSaver itz() {
        return ravex.manager.ModuleManager.delegate(ItemSaver.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}