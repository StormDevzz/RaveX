package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.TradeUtility;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "AutoTrade", category = "World")
public class AutoTrade {
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
        long now = System.currentTimeMillis();
        if (now - lastActionTime < 200) return;
        if (TradeUtility.isTradeOpen()) {
            if (tradesDone >= (int) maxTrades) return;
            int slot = TradeUtility.findOffer(mode);
            if (slot != -1 && TradeUtility.takeOffer(slot)) {
                tradesDone++;
                lastActionTime = now;
            }
            return;
        }
        if (!autoOpen) return;
        if (MinecraftWrapper.getWrapper().getPlayer() == null) return;
        if (TradeUtility.openNearestVillager(range)) lastActionTime = now;
    }
}
