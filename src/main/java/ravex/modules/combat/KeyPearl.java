package ravex.modules.combat;
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.utility.player.InventoryUtility;
import java.util.List;
public class KeyPearl extends Module {
    public final ModeParameter swap = new ModeParameter("Swap", "Silent", List.of("Silent", "Normal"));

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
        InventoryUtility.selectSlot(mc.player, pearlSlot);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        if ("Silent".equals(swap.getValue())) {
            InventoryUtility.selectSlot(mc.player, prevSlot);
        }
        setEnabled(false);
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(KeyPearl.class);
    }
    public static KeyPearl itz() {
        return ModuleManager.get(KeyPearl.class);
    }

}