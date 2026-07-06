package ravex.modules.combat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.player.InventoryUtility;
public class KeyPearl extends Module {
    public static final KeyPearl INSTANCE = new KeyPearl();
    public final BooleanParameter silent = new BooleanParameter("Silent", true);

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        int pearlSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (InventoryUtility.isItem(stack, "ender_pearl")) { pearlSlot = i; break; }
        }
        if (pearlSlot == -1) return;
        int prevSlot = InventoryUtility.getSelectedSlot(mc.player);
        if (silent.getValue()) {
            InventoryUtility.selectSlot(mc.player, pearlSlot);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            InventoryUtility.selectSlot(mc.player, prevSlot);
        } else {
            InventoryUtility.selectSlot(mc.player, pearlSlot);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
        setEnabled(false);
    }
}
