package ravex.manager;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import java.lang.reflect.Field;

public class HotbarManager {
    public static final HotbarManager INSTANCE = new HotbarManager();

    private int originalSlot = -1;
    private static Field selectedField = null;

    static {
        try {
            try {
                selectedField = Inventory.class.getDeclaredField("selected");
            } catch (NoSuchFieldException e) {
                selectedField = Inventory.class.getDeclaredField("selectedSlot");
            }
            selectedField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HotbarManager() {}

    public int getSelectedSlot() {
        var player = Minecraft.getInstance().player;
        if (player == null || selectedField == null) return 0;
        try {
            return selectedField.getInt(player.getInventory());
        } catch (Exception e) {
            return 0;
        }
    }

    public void swapToSlot(int slot) {
        var player = Minecraft.getInstance().player;
        if (player == null || selectedField == null) return;
        if (originalSlot == -1) {
            originalSlot = getSelectedSlot();
        }
        try {
            selectedField.setInt(player.getInventory(), slot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void swapBack() {
        var player = Minecraft.getInstance().player;
        if (player == null || originalSlot == -1 || selectedField == null) return;
        try {
            selectedField.setInt(player.getInventory(), originalSlot);
        } catch (Exception e) {
            e.printStackTrace();
        }
        originalSlot = -1;
    }
}
