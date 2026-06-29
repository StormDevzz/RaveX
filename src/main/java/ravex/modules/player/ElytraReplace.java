package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.entity.EquipmentSlot;

public class ElytraReplace extends Module {
    public static final ElytraReplace INSTANCE = new ElytraReplace();

    public final NumberParameter minDurability = new NumberParameter("Min Durability", 10.0, 1.0, 50.0, 1.0);
    public final BooleanParameter preferBetter = new BooleanParameter("Prefer Better", true);

    private ElytraReplace() {
        super("ElytraReplace", Category.PLAYER);
        addParameter(minDurability);
        addParameter(preferBetter);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null) return;

        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(Items.ELYTRA)) return;

        int maxDmg = chest.getMaxDamage();
        int currentDmg = chest.getDamageValue();
        int durability = maxDmg - currentDmg;

        if (durability <= minDurability.getValue().intValue()) {
            int replacementSlot = findReplacementElytraSlot(p);
            if (replacementSlot != -1) {
                int containerSlot = replacementSlot < 9 ? replacementSlot + 36 : replacementSlot;
                
                mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, p);
                mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, 6, 0, ClickType.PICKUP, p);
                mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, p);
            }
        }
    }

    private int findReplacementElytraSlot(LocalPlayer p) {
        int bestSlot = -1;
        int bestDurability = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.is(Items.ELYTRA)) {
                int dur = stack.getMaxDamage() - stack.getDamageValue();
                if (dur > minDurability.getValue().intValue()) {
                    if (!preferBetter.getValue()) {
                        return i;
                    }
                    if (dur > bestDurability) {
                        bestDurability = dur;
                        bestSlot = i;
                    }
                }
            }
        }
        return bestSlot;
    }
}
