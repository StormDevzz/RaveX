package ravex.utility.misc;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;

public final class FishUtility {
    private static long hookCastTime = 0;

    private FishUtility() {}

    public static void reset() {
        hookCastTime = 0;
    }

    public static boolean hasHook() {
        var player = MinecraftWrapper.getWrapper().getPlayer();
        if (player == null) return false;
        FishingHook hook = player.fishing;
        return hook != null && !hook.isRemoved();
    }

    public static boolean isBiting() {
        var player = MinecraftWrapper.getWrapper().getPlayer();
        if (player == null) return false;
        FishingHook hook = player.fishing;
        if (hook == null || hook.isRemoved()) return false;
        if (((ravex.mixin.player.AccessorFishingHook) hook).getBiting()) return true;
        return hook.getHookedIn() != null;
    }

    // milliseconds the current bobber has been in the water, 0 when unknown.
    public static long hookAgeMs() {
        if (!hasHook()) return 0;
        long now = System.currentTimeMillis();
        if (hookCastTime == 0) hookCastTime = now;
        return now - hookCastTime;
    }

    // swaps to the best rod, uses it and optionally swaps back.
    public static boolean useRod(boolean antiBreak, boolean swapBack) {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getGameMode() == null) return false;
        int prev = InventoryUtility.getSelectedSlot(player);
        int rodSlot = findBestRodSlot(antiBreak);
        ItemStack main = player.getMainHandItem();
        if (rodSlot == -1 && !isRod(main, antiBreak) && !isRod(player.getOffhandItem(), antiBreak)) return false;

        boolean swapped = false;
        if (rodSlot != -1 && rodSlot != prev) {
            InventoryUtility.selectSlot(player, rodSlot);
            swapped = true;
        }
        if (isRod(player.getMainHandItem(), antiBreak)) {
            mc.getGameMode().useItem(player, InteractionHand.MAIN_HAND);
        } else {
            mc.getGameMode().useItem(player, InteractionHand.OFF_HAND);
        }
        SwingUtility.swingMainHand(player);
        if (swapped && swapBack) InventoryUtility.selectSlot(player, prev);
        return true;
    }

    public static int findBestRodSlot(boolean antiBreak) {
        int best = -1;
        int bestScore = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = InventoryUtility.getItem(MinecraftWrapper.getWrapper().getPlayer(), i);
            if (!isRod(stack, antiBreak)) continue;
            int score = rodScore(stack);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    public static boolean isRod(ItemStack stack, boolean antiBreak) {
        if (stack == null || stack.isEmpty() || !InventoryUtility.isItem(stack, "fishing_rod")) return false;
        if (antiBreak && stack.getMaxDamage() > 0 && stack.getDamageValue() >= stack.getMaxDamage() - 1) return false;
        return true;
    }

    // lure and luck of the sea make the fish come (and bite) faster.
    public static int rodScore(ItemStack stack) {
        int score = 0;
        score += InventoryUtility.getEnchantmentLevel(stack, "minecraft:luck_of_the_sea");
        score += InventoryUtility.getEnchantmentLevel(stack, "minecraft:lure");
        score += InventoryUtility.getEnchantmentLevel(stack, "minecraft:mending");
        score += InventoryUtility.getEnchantmentLevel(stack, "minecraft:unbreaking");
        return score;
    }
}
