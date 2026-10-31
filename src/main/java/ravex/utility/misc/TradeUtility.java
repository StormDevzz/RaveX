package ravex.utility.misc;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import ravex.mcwrapper.MinecraftWrapper;

public final class TradeUtility {
    private TradeUtility() {}

    private static MerchantOffers offers() {
        var player = MinecraftWrapper.getWrapper().getPlayer();
        if (player == null || !(player.containerMenu instanceof MerchantMenu menu)) return null;
        return menu.getOffers();
    }

    public static boolean isTradeOpen() {
        return offers() != null;
    }

    public static int offerCount() {
        MerchantOffers list = offers();
        return list == null ? 0 : list.size();
    }

    public static boolean isOutOfStock(int index) {
        MerchantOffers list = offers();
        return list == null || index < 0 || index >= list.size() || list.get(index).isOutOfStock();
    }

    // best / Cheapest / First, returns the offer slot or -1.
    public static int findOffer(String mode) {
        MerchantOffers list = offers();
        if (list == null || list.isEmpty()) return -1;
        int best = -1;
        double bestScore = -1;
        int cheapest = Integer.MAX_VALUE;
        for (int i = 0; i < list.size(); i++) {
            MerchantOffer offer = list.get(i);
            if (offer.isOutOfStock()) continue;
            if ("First".equals(mode)) return i;
            int cost = offerCost(offer);
            if ("Cheapest".equals(mode)) {
                if (cost < cheapest) {
                    cheapest = cost;
                    best = i;
                }
            } else {
                double score = (double) offer.getResult().getCount() / Math.max(1, cost);
                if (score > bestScore) {
                    bestScore = score;
                    best = i;
                }
            }
        }
        return best;
    }

    public static boolean takeOffer(int index) {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        MerchantOffers list = offers();
        if (player == null || list == null || index < 0 || index >= list.size()) return false;
        MerchantMenu menu = (MerchantMenu) player.containerMenu;
        mc.getRaw().gameMode.handleInventoryMouseClick(menu.containerId, index, 0, ClickType.PICKUP, player);
        return true;
    }

    public static boolean openNearestVillager(double range) {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return false;
        double best = range;
        net.minecraft.world.entity.Entity target = null;
        for (var entity : mc.getLevel().entitiesForRendering()) {
            if (!EntityUtility.isVillager(entity)) continue;
            double dist = player.distanceTo(entity);
            if (dist <= best) {
                best = dist;
                target = entity;
            }
        }
        if (target == null) return false;
        player.interactOn(target, InteractionHand.MAIN_HAND);
        return true;
    }

    private static int offerCost(MerchantOffer offer) {
        int cost = 0;
        var a = offer.getCostA();
        var b = offer.getCostB();
        if (!a.isEmpty()) cost += a.getCount();
        if (!b.isEmpty()) cost += b.getCount();
        return cost;
    }
}
