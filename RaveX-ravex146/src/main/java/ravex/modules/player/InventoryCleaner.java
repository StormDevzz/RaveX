package ravex.modules.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ActionParameter;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;

public class InventoryCleaner extends Module {
    public static final InventoryCleaner INSTANCE = new InventoryCleaner();

    public final BooleanParameter autoClean = new BooleanParameter("Auto Clean", false);
    public final NumberParameter interval   = new NumberParameter("Interval (s)", 10, 2, 60, 1);
    public final ActionParameter items = new ActionParameter("Items", () -> {
        Minecraft.getInstance().setScreen(new ravex.gui.clickgui.InventoryCleanerScreen(Minecraft.getInstance().screen));
    });

    private long lastCleanTime = 0;

    private InventoryCleaner() {
        super("InventoryCleaner", Category.PLAYER);
        addParameter(autoClean);
        addParameter(interval);
        addParameter(items);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.execute(() -> mc.setScreen(new ravex.gui.clickgui.InventoryCleanerScreen(null)));
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

        Inventory inv = mc.player.getInventory();

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            Identifier rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (rl == null) continue;

            String itemId = rl.toString();
            if (InventoryCleanerData.INSTANCE.isSelected(itemId)) {



                int slot = i;
                final int finalI = i;
                mc.execute(() -> {
                    ItemStack s = mc.player.getInventory().getItem(finalI);
                    if (!s.isEmpty()) {

                        mc.player.drop(true);
                        if (finalI < 9) {
                            int prevSelected = ravex.manager.HotbarManager.INSTANCE.getSelectedSlot();
                            ravex.manager.HotbarManager.INSTANCE.swapToSlot(finalI);
                            mc.getConnection().send(
                                new ServerboundPlayerActionPacket(
                                    ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS,
                                    BlockPos.ZERO,
                                    Direction.DOWN,
                                    0
                                )
                            );
                            ravex.manager.HotbarManager.INSTANCE.swapToSlot(prevSelected);
                        }
                    }
                });
            }
        }
    }
}
