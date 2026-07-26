package ravex.utility.misc;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class TradingUtility {
    public static boolean isTradeMenu(AbstractContainerMenu menu) {
        return menu instanceof MerchantMenu;
    }

    public static MerchantOffers getOffers(AbstractContainerMenu menu) {
        if (menu instanceof MerchantMenu m) return m.getOffers();
        return null;
    }

    public static int getOfferCount(AbstractContainerMenu menu) {
        var offers = getOffers(menu);
        return offers != null ? offers.size() : 0;
    }

    public static boolean isOutOfStock(AbstractContainerMenu menu, int index) {
        var offers = getOffers(menu);
        return offers == null || index < 0 || index >= offers.size() || offers.get(index).isOutOfStock();
    }

    public static int getCost(AbstractContainerMenu menu, int index) {
        var offers = getOffers(menu);
        if (offers == null || index < 0 || index >= offers.size()) return Integer.MAX_VALUE;
        MerchantOffer offer = offers.get(index);
        int cost = 0;
        var a = offer.getCostA();
        var b = offer.getCostB();
        if (!a.isEmpty()) cost += a.getCount();
        if (!b.isEmpty()) cost += b.getCount();
        return cost;
    }

    public static int getResultCount(AbstractContainerMenu menu, int index) {
        var offers = getOffers(menu);
        if (offers == null || index < 0 || index >= offers.size()) return 0;
        return offers.get(index).getResult().getCount();
    }

    public static int findBestIndex(AbstractContainerMenu menu, String mode) {
        var offers = getOffers(menu);
        if (offers == null || offers.isEmpty()) return -1;
        return switch (mode) {
            case "Cheapest" -> findCheapest(offers);
            case "First" -> findFirst(offers);
            default -> findBest(offers);
        };
    }

    private static int findCheapest(MerchantOffers offers) {
        int bestIdx = -1;
        int bestCost = Integer.MAX_VALUE;
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i).isOutOfStock()) continue;
            int cost = 0;
            var a = offers.get(i).getCostA();
            var b = offers.get(i).getCostB();
            if (!a.isEmpty()) cost += a.getCount();
            if (!b.isEmpty()) cost += b.getCount();
            if (cost < bestCost) { bestCost = cost; bestIdx = i; }
        }
        return bestIdx;
    }

    private static int findFirst(MerchantOffers offers) {
        for (int i = 0; i < offers.size(); i++) {
            if (!offers.get(i).isOutOfStock()) return i;
        }
        return -1;
    }

    private static int findBest(MerchantOffers offers) {
        int bestIdx = -1;
        double bestRatio = 0;
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i).isOutOfStock()) continue;
            int cost = 0;
            var a = offers.get(i).getCostA();
            var b = offers.get(i).getCostB();
            if (!a.isEmpty()) cost += a.getCount();
            if (!b.isEmpty()) cost += b.getCount();
            double ratio = (double) offers.get(i).getResult().getCount() / Math.max(1, cost);
            if (ratio > bestRatio) { bestRatio = ratio; bestIdx = i; }
        }
        return bestIdx;
    }
}
