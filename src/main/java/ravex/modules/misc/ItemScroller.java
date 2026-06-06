package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;

public class ItemScroller extends Module {
    public static final ItemScroller INSTANCE = new ItemScroller();

    public final NumberParameter delay = new NumberParameter("Delay", 80.0, 0.0, 500.0, 10.0);

    private boolean pauseListening = false;

    private ItemScroller() {
        super("ItemScroller", Category.MISC);
        addParameter(delay);
    }

    /**
     * Called from MixinMultiPlayerGameMode when a THROW click happens
     * while Ctrl+Shift are held. Drops all items of the same type.
     */
    public boolean isPauseListening() {
        return pauseListening;
    }

    public boolean handleClick(int slotId) {
        if (pauseListening) return false;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return false;

        AbstractContainerMenu menu = player.containerMenu;
        if (slotId < 0 || slotId >= menu.slots.size()) return false;
        Slot slot = menu.slots.get(slotId);
        if (!slot.hasItem()) return false;

        Item target = slot.getItem().getItem();
        pauseListening = true;

        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (s.hasItem() && s.getItem().getItem() == target) {
                mc.gameMode.handleInventoryMouseClick(
                    menu.containerId, s.index, 1, ClickType.THROW, player);
            }
        }

        pauseListening = false;
        return true;
    }
}
