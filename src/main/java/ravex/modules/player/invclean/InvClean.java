package ravex.modules.player.invclean;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ActionParameter;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.network.NetworkUtility;
import ravex.utility.player.InventoryUtility;
public class InvClean extends Module {
    public static final InvClean INSTANCE = new InvClean();
    public final BooleanParameter autoClean = new BooleanParameter("AutoClean", false);
    public final NumberParameter interval   = new NumberParameter("Interval(s)", 10, 2, 60, 1);
    public final ActionParameter items = new ActionParameter("Items", () -> {
        Minecraft.getInstance().setScreen(new ravex.gui.clickgui.InvCleanScreen(Minecraft.getInstance().screen));
    });
    private long lastCleanTime = 0;

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.execute(() -> mc.setScreen(new ravex.gui.clickgui.InvCleanScreen(null)));
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        if (!autoClean.getValue()) return;
        long now = System.currentTimeMillis();
        long intervalMs = (long)(interval.getValue() * 1000);
        if (now - lastCleanTime < intervalMs) return;
        lastCleanTime = now;
        cleanInventory(mc);
    }
    public static void cleanInventory(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) return;
        net.minecraft.world.entity.player.Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            var stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            var rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (rl == null) continue;
            String itemId = rl.toString();
            if (InvCleanData.INSTANCE.isSelected(itemId)) {
                final int fi = i;
                mc.execute(() -> {
                    var s = InventoryUtility.getItem(mc.player, fi);
                    if (!s.isEmpty()) {
                        mc.player.drop(true);
                        if (fi < 9) {
                            int prevSelected = InventoryUtility.getSelectedSlot(mc.player);
                            NetworkUtility.sendSetCarriedItem(fi);
                            NetworkUtility.sendDropAll(net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.DOWN);
                            NetworkUtility.sendSetCarriedItem(prevSelected);
                        }
                    }
                });
            }
        }
    }
}
