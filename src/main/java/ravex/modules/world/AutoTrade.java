package ravex.modules.world;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import ravex.utility.misc.MobUtility;
import java.util.Comparator;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "AutoTrade", category = "World")
public class AutoTrade implements ModuleAccess {
    @Parameter(name = "Range", min = 2.0, max = 6.0, step = 0.5)
    public double range = 4.0;
    @Parameter(name = "Mode", modes = {"Best", "Cheapest", "First"})
    public String mode = "Best";
    @Parameter(name = "MaxTrades", min = 1, max = 100, step = 1)
    public double maxTrades = 10;
    @Parameter(name = "AutoOpen")
    public boolean autoOpen = true;
    private int tradesDone = 0;
    private long lastActionTime = 0;
    public void onEnable() {
        tradesDone = 0;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastActionTime < 200) return;
        if (player.containerMenu instanceof MerchantMenu menu) {
            MerchantOffers offers = menu.getOffers();
            if (offers == null || offers.isEmpty()) return;
            int max = (int) maxTrades;
            if (tradesDone >= max) return;
            MerchantOffer best = findBestOffer(offers);
            if (best != null && !best.isOutOfStock()) {
                int slot = offers.indexOf(best);
                mc.gameMode.handleInventoryMouseClick(
                    menu.containerId, slot, 0,
                    net.minecraft.world.inventory.ClickType.PICKUP, player
                );
                tradesDone++;
                lastActionTime = now;
            }
            return;
        }
        if (!autoOpen) return;
        double r = range;
        for (var entity : mc.level.entitiesForRendering()) {
            if (MobUtility.isVillager(entity)) {
                player.interactOn(entity, net.minecraft.world.InteractionHand.MAIN_HAND);
                lastActionTime = now;
                break;
            }
        }
    }
    private MerchantOffer findBestOffer(MerchantOffers offers) {
        String m = mode;
        return switch (m) {
            case "Cheapest" -> offers.stream()
                .filter(o -> !o.isOutOfStock())
                .min(Comparator.comparingInt(this::getCost))
                .orElse(null);
            case "First" -> offers.stream()
                .filter(o -> !o.isOutOfStock())
                .findFirst().orElse(null);
            default -> offers.stream()
                .filter(o -> !o.isOutOfStock())
                .max(Comparator.comparingDouble(o ->
                    (double) o.getResult().getCount() / Math.max(1, getCost(o))))
                .orElse(null);
        };
    }
    private int getCost(MerchantOffer offer) {
        int cost = 0;
        var a = offer.getCostA();
        var b = offer.getCostB();
        if (!a.isEmpty()) cost += a.getCount();
        if (!b.isEmpty()) cost += b.getCount();
        return cost;
    }
    public static AutoTrade itz() {
        return ravex.manager.ModuleManager.delegate(AutoTrade.class);
    }


}