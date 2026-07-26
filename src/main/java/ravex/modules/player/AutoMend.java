package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import ravex.utility.player.SwingUtility;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.EquipmentSlot;
import ravex.utility.player.InventoryUtility;
import java.util.List;
@ModuleInfo(name = "AutoMend", category = "net.minecraft.world.entity.player.Player")
public class AutoMend extends ravex.modules.Module {
public final NumberParameter threshold = new NumberParameter("Threshold", 50.0, 10.0, 95.0, 5.0);
    public final ModeParameter swapMode = new ModeParameter("Swap", "Silent", List.of("Normal", "Silent"));
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        boolean needsMend = false;
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : armorSlots) {
            var stack = p.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamaged()) {
                double maxDamage = stack.getMaxDamage();
                double currentDamage = stack.getDamageValue();
                double durabilityPct = ((maxDamage - currentDamage) / maxDamage) * 100.0;
                if (durabilityPct < threshold.getValue()) {
                    needsMend = true;
                    break;
                }
            }
        }
        if (!needsMend) return;
        int expSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.isItem(InventoryUtility.getItem(p, i), "experience_bottle")) {
                expSlot = i;
                break;
            }
        }
        if (expSlot == -1) return;
        int prevSlot = InventoryUtility.getSelectedSlot(p);
        boolean silent = "Silent".equals(swapMode.getValue());
        InventoryUtility.selectSlot(p, expSlot);
        p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), 90.0F, p.onGround(), p.horizontalCollision));
        mc.gameMode.useItem(p, net.minecraft.world.InteractionHand.MAIN_HAND);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (silent) {
            InventoryUtility.selectSlot(p, prevSlot);
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoMend").getEnabled();
    }
    public static AutoMend itz() {
        return ravex.manager.ModuleManager.delegate(AutoMend.class);
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