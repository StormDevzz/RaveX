package ravex.modules.combat;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.utility.player.InventoryUtility;
import java.util.List;
public class KeyPearl extends Module {
    public final ModeParameter swap = new ModeParameter("Swap", "Silent", List.of("Silent", "Normal"));
=======
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
public class KeyPearl extends Module {
    public static final KeyPearl INSTANCE = new KeyPearl();
    public final BooleanParameter silent = new BooleanParameter("Silent", true);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        int pearlSlot = -1;
        for (int i = 0; i < 9; i++) {
<<<<<<< HEAD
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
=======
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.ENDER_PEARL)) { pearlSlot = i; break; }
        }
        if (pearlSlot == -1) return;
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        if (silent.getValue()) {
            mc.player.getInventory().setSelectedSlot(pearlSlot);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.getInventory().setSelectedSlot(prevSlot);
        } else {
            mc.player.getInventory().setSelectedSlot(pearlSlot);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
        setEnabled(false);
    }
}
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
