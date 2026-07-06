package ravex.modules.player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.EquipmentSlot;
import ravex.utility.player.InventoryUtility;
public class AutoMend extends Module {
    public static final AutoMend INSTANCE = new AutoMend();
    public final NumberParameter threshold = new NumberParameter("Threshold%", 50.0, 10.0, 95.0, 5.0);
    public final BooleanParameter silent = new BooleanParameter("SilentSwap", true);

    @Override
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
        if (silent.getValue()) {
            InventoryUtility.selectSlot(p, expSlot);
            p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), 90.0F, p.onGround(), p.horizontalCollision));
            mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
            p.swing(InteractionHand.MAIN_HAND);
            InventoryUtility.selectSlot(p, prevSlot);
        } else {
            InventoryUtility.selectSlot(p, expSlot);
            p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), 90.0F, p.onGround(), p.horizontalCollision));
            mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
            p.swing(InteractionHand.MAIN_HAND);
        }
    }
}
