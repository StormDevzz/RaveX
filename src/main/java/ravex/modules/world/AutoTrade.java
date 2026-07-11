package ravex.modules.world;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.misc.MobUtility;
import java.util.Comparator;
import java.util.List;
public class AutoTrade extends Module {
    public final NumberParameter range = new NumberParameter("Range", 4.0, 2.0, 6.0, 0.5);
    public final ModeParameter mode = new ModeParameter("Mode", "Best", List.of("Best", "Cheapest", "First"));
    public final NumberParameter maxTrades = new NumberParameter("MaxTrades", 10, 1, 100, 1);
    public final BooleanParameter autoOpen = new BooleanParameter("AutoOpen", true);
    private int tradesDone = 0;
    private long lastActionTime = 0;

    @Override
    protected void onEnable() {
        tradesDone = 0;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastActionTime < 200) return;
        if (player.containerMenu instanceof MerchantMenu menu) {
            MerchantOffers offers = menu.getOffers();
            if (offers == null || offers.isEmpty()) return;
            int max = maxTrades.getValue().intValue();
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
        if (!autoOpen.getValue()) return;
        double r = range.getValue();
        for (var entity : mc.level.entitiesForRendering()) {
            if (MobUtility.isVillager(entity)) {
                player.interactOn(entity, net.minecraft.world.InteractionHand.MAIN_HAND);
                lastActionTime = now;
                break;
            }
        }
    }
    private MerchantOffer findBestOffer(MerchantOffers offers) {
        String m = mode.getValue();
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
        return ModuleManager.get(AutoTrade.class);
    }
}
