package ravex.modules.player.invclean;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.core.registries.BuiltInRegistries;

import ravex.parameter.ActionParameter;
import ravex.utility.network.NetworkUtility;
import ravex.utility.player.InventoryUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "InvClean", category = "net.minecraft.world.entity.player.Player")
public class InvClean {
    @Parameter(name = "AutoClean")
    public boolean autoClean = false;
    @Parameter(name = "Interval", min = 2, max = 60, step = 1)
    public double interval = 10;
    public final ActionParameter items = new ActionParameter("Items", () -> {
        MinecraftWrapper.getWrapper().setScreen(new ravex.gui.clickgui.InvCleanScreen(MinecraftWrapper.getWrapper().getCurrentScreen()));
    });
    private long lastCleanTime = 0;
    public void onEnable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        mc.execute(() -> mc.setScreen(new ravex.gui.clickgui.InvCleanScreen(null)));
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getConnection() == null) return;
        if (!autoClean) return;
        long now = System.currentTimeMillis();
        long intervalMs = (long)(interval * 1000);
        if (now - lastCleanTime < intervalMs) return;
        lastCleanTime = now;
        cleanInventory(mc);
    }
    public static void cleanInventory(MinecraftWrapper mc) {
        if (mc.getPlayer() == null || mc.getConnection() == null) return;
        net.minecraft.world.entity.player.Inventory inv = mc.getPlayer().getInventory();
        for (int i = 0; i < 36; i++) {
            var stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            var rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (rl == null) continue;
            String itemId = rl.toString();
            if (InvCleanData.INSTANCE.isSelected(itemId)) {
                final int fi = i;
                mc.execute(() -> {
                    var s = InventoryUtility.getItem(mc.getPlayer(), fi);
                    if (!s.isEmpty()) {
                        mc.getPlayer().drop(true);
                        if (fi < 9) {
                            int prevSelected = InventoryUtility.getSelectedSlot(mc.getPlayer());
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