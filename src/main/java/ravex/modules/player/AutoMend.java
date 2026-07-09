package ravex.modules.player;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.EquipmentSlot;
<<<<<<< HEAD
import ravex.utility.player.InventoryUtility;
import java.util.List;
public class AutoMend extends Module {
    public final NumberParameter threshold = new NumberParameter("Threshold", 50.0, 10.0, 95.0, 5.0);
    public final ModeParameter swapMode = new ModeParameter("Swap", "Silent", List.of("Normal", "Silent"));
=======
public class AutoMend extends Module {
    public static final AutoMend INSTANCE = new AutoMend();
    public final NumberParameter threshold = new NumberParameter("Threshold%", 50.0, 10.0, 95.0, 5.0);
    public final BooleanParameter silent = new BooleanParameter("SilentSwap", true);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

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
<<<<<<< HEAD
        int prevSlot = InventoryUtility.getSelectedSlot(p);
        boolean silent = "Silent".equals(swapMode.getValue());
        InventoryUtility.selectSlot(p, expSlot);
        p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), 90.0F, p.onGround(), p.horizontalCollision));
        mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
        p.swing(InteractionHand.MAIN_HAND);
        if (silent) {
            InventoryUtility.selectSlot(p, prevSlot);
=======
        int prevSlot = p.getInventory().getSelectedSlot();
        if (silent.getValue()) {
            p.getInventory().setSelectedSlot(expSlot);
            p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), 90.0F, p.onGround(), p.horizontalCollision));
            mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
            p.swing(InteractionHand.MAIN_HAND);
            p.getInventory().setSelectedSlot(prevSlot);
        } else {
            p.getInventory().setSelectedSlot(expSlot);
            p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), 90.0F, p.onGround(), p.horizontalCollision));
            mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
            p.swing(InteractionHand.MAIN_HAND);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(AutoMend.class);
    }
    public static AutoMend itz() {
        return ModuleManager.get(AutoMend.class);
    }

}