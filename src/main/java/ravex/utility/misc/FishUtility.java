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

    public static long hookAgeMs() {
        if (!hasHook()) return 0;
        long now = System.currentTimeMillis();
        if (hookCastTime == 0) hookCastTime = now;
        return now - hookCastTime;
    }

    public static boolean useRod(String swapMode) {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getGameMode() == null) return false;

        int previous = InventoryUtility.getSelectedSlot(player);
        int rodSlot = findBestRodSlot();
        boolean mainRod = isRod(player.getMainHandItem());
        boolean offhandRod = isRod(player.getOffhandItem());
        if (rodSlot == -1 && !mainRod && !offhandRod) return false;
        if ("None".equals(swapMode) && !mainRod && !offhandRod) return false;

        boolean swap = !"None".equals(swapMode) && rodSlot != -1 && rodSlot != previous;
        if (swap) {
            InventoryUtility.selectSlot(player, rodSlot);
            if ("Silent".equals(swapMode)) InventoryUtility.silentSelectSlot(player, rodSlot);
            mainRod = isRod(player.getMainHandItem());
        }

        if (mainRod) {
            mc.getGameMode().useItem(player, InteractionHand.MAIN_HAND);
        } else {
            mc.getGameMode().useItem(player, InteractionHand.OFF_HAND);
        }
        SwingUtility.swingMainHand(player);

        if (swap) {
            InventoryUtility.selectSlot(player, previous);
            if ("Silent".equals(swapMode)) InventoryUtility.silentSelectSlot(player, previous);
        }
        return true;
    }

    public static int findBestRodSlot() {
        int best = -1;
        int bestScore = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = InventoryUtility.getItem(MinecraftWrapper.getWrapper().getPlayer(), i);
            if (!isRod(stack)) continue;
            int score = rodScore(stack);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    public static boolean isRod(ItemStack stack) {
        return stack != null && !stack.isEmpty() && InventoryUtility.isItem(stack, "fishing_rod");
    }

    public static int rodScore(ItemStack stack) {
        int score = 0;
        score += InventoryUtility.getEnchantmentLevel(stack, "minecraft:luck_of_the_sea");
        score += InventoryUtility.getEnchantmentLevel(stack, "minecraft:lure");
        score += InventoryUtility.getEnchantmentLevel(stack, "minecraft:mending");
        score += InventoryUtility.getEnchantmentLevel(stack, "minecraft:unbreaking");
        return score;
    }
}
