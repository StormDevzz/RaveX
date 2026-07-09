package ravex.modules.misc;
<<<<<<< HEAD
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.parameter.NumberParameter;
public class FastItem extends Module {
    public final NumberParameter delay = new NumberParameter("Delay", 0.0, 0.0, 500.0, 10.0);

    public long getDelayMs() {
        return delay.getValue().longValue();
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(FastItem.class);
    }

    public static FastItem itz() {
        return ModuleManager.get(FastItem.class);
=======
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
public class FastItem extends Module {
    public static final FastItem INSTANCE = new FastItem();
    public final NumberParameter delay = new NumberParameter("Delay(ms)", 80.0, 0.0, 500.0, 10.0);
    private long lastTransfer = 0;

    public boolean handleSlotHover(int slotIndex, long now) {
        if (!getEnabled()) return false;
        long d = delay.getValue().longValue();
        if (now - lastTransfer < d) return false;
        lastTransfer = now;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return false;
        if (slotIndex < 0 || slotIndex >= mc.player.containerMenu.slots.size()) return false;
        mc.gameMode.handleInventoryMouseClick(
            mc.player.containerMenu.containerId,
            slotIndex, 0, ClickType.QUICK_MOVE, mc.player);
        return true;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }
}
