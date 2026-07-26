package ravex.modules.combat;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.utility.player.SwingUtility;

import ravex.parameter.ModeParameter;
import ravex.utility.player.InventoryUtility;
import java.util.List;
@ModuleInfo(name = "KeyPearl", category = "Combat")
public class KeyPearl extends ravex.modules.Module {
public final ModeParameter swap = new ModeParameter("Swap", "Silent", List.of("Silent", "Normal"));
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
        mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        if ("Silent".equals(swap.getValue())) {
            InventoryUtility.selectSlot(mc.player, prevSlot);
        }
        enabled = false;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("KeyPearl").getEnabled();
    }
    public static KeyPearl itz() {
        return ravex.manager.ModuleManager.delegate(KeyPearl.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}