package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;



@ModuleInfo(name = "AutoMend", category = "net.minecraft.world.entity.player.Player")
public class AutoMend implements ModuleAccess {
    @Parameter(name = "Threshold", min = 10.0, max = 95.0, step = 5.0)
    public double threshold = 50.0;
    @Parameter(name = "Swap", modes = {"Normal", "Silent"})
    public String swapMode = "Silent";
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        net.minecraft.client.player.LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        boolean needsMend = false;
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : armorSlots) {
            var stack = p.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamaged()) {
                double maxDamage = stack.getMaxDamage();
                double currentDamage = stack.getDamageValue();
                double durabilityPct = ((maxDamage - currentDamage) / maxDamage) * 100.0;
                if (durabilityPct < threshold) {
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
        boolean silent = "Silent".equals(swapMode);
        InventoryUtility.selectSlot(p, expSlot);
        p.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(p.getYRot(), 90.0F, p.onGround(), p.horizontalCollision));
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


}