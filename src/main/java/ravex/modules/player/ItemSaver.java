package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.client.ClientAlertUtility;
import ravex.utility.misc.LanguageUtility;
import ravex.utility.player.InventoryUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

import java.util.HashMap;
import java.util.Map;

@Module(name = "ItemSaver", category = "Player")
public class ItemSaver {
    @Parameter(name = "Durability", min = 1.0, max = 100.0, step = 1.0)
    public double threshold = 10.0;
    // warning follows the Notifications module
    @Parameter(name = "Warn")
    public boolean warn = true;
    @Parameter(name = "WarnCooldown", min = 1.0, max = 60.0, step = 1.0, visible = "warn")
    public double warnCooldown = 10.0;

    private final Map<String, Long> lastWarn = new HashMap<>();

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null) return;
        if (warn) warnLowItems(p);
        var mainHand = InventoryUtility.getMainHand(p);
        if (!shouldSave(mainHand)) return;
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
        if (safeSlot != -1 && safeSlot != current) {
            InventoryUtility.selectSlot(p, safeSlot);
            if (warn) alertOnce(mainHand, "swap", LanguageUtility.t("itemsaver_swapped", mainHand.getHoverName().getString()));
        }
    }

    private void warnLowItems(net.minecraft.client.player.LocalPlayer p) {
        warnStack(p.getMainHandItem());
        warnStack(p.getOffhandItem());
    }

    private void warnStack(net.minecraft.world.item.ItemStack stack) {
        if (!shouldSave(stack)) return;
        String name = stack.getHoverName().getString();
        int percent = (int) Math.round(InventoryUtility.getDurabilityPercent(stack));
        alertOnce(stack, "low", LanguageUtility.t("itemsaver_low", name, percent));
    }

    private void alertOnce(net.minecraft.world.item.ItemStack stack, String kind, String message) {
        String key = kind + ":" + stack.getHoverName().getString();
        long now = System.currentTimeMillis();
        Long last = lastWarn.get(key);
        if (last != null && now - last < (long) (warnCooldown * 1000)) return;
        lastWarn.put(key, now);
        ClientAlertUtility.alert(message, 0xFFFF4444);
    }

    public boolean shouldSave(net.minecraft.world.item.ItemStack stack) {
        if (!Modules.enabled(ItemSaver.class)) return false;
        if (!InventoryUtility.isDamageable(stack)) return false;
        if (stack.getMaxDamage() <= 0) return false;
        return InventoryUtility.getDurabilityPercent(stack) <= threshold;
    }
}
