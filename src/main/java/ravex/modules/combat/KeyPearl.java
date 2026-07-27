package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.SwingUtility;

import ravex.utility.player.InventoryUtility;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "KeyPearl", category = "Combat")
public class KeyPearl implements ModuleAccess {
    @Parameter(name = "Swap", modes = {"Silent", "Normal"})
    public String swap = "Silent";
    public void onEnable() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;
        int pearlSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (InventoryUtility.isItem(stack, "ender_pearl")) { pearlSlot = i; break; }
        }
        if (pearlSlot == -1) return;
        int prevSlot = InventoryUtility.getSelectedSlot(mc.player);
        InventoryUtility.selectSlot(mc.player, pearlSlot);
        mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        if ("Silent".equals(swap)) {
            InventoryUtility.selectSlot(mc.player, prevSlot);
        }
        ravex.manager.ModuleManager.INSTANCE.getByName("KeyPearl").setEnabled(false);
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("KeyPearl").getEnabled();
    }
    public static KeyPearl itz() {
        return ravex.manager.ModuleManager.delegate(KeyPearl.class);
    }


}