package ravex.modules.combat;
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

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        int pearlSlot = -1;
        for (int i = 0; i < 9; i++) {
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
