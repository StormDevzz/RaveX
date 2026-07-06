package ravex.modules.combat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import ravex.utility.misc.MobUtility;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import java.util.List;
import ravex.utility.player.InventoryUtility;
public class AutoWeapon extends Module {
    public static final AutoWeapon INSTANCE = new AutoWeapon();
    public final ModeParameter swapMode = new ModeParameter("SwapMode", "Normal",
            List.of("Normal", "Silent", "None"));
    public final BooleanParameter swordsOnly = new BooleanParameter("SwordsOnly", false);

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (swapMode.getValue().equals("None")) return;
        if (mc.options.keyAttack.isDown() && mc.hitResult instanceof EntityHitResult hit) {
            LivingEntity target = MobUtility.asLivingEntity(hit.getEntity());
            if (target == null) return;
            if (MobUtility.isDead(target) || MobUtility.isSelf(target)) return;
            int bestSlot = -1;
            double bestDmg = -1.0;
            for (int i = 0; i < 9; i++) {
                var stack = InventoryUtility.getItem(mc.player, i);
                if (swordsOnly.getValue() && !isSword(stack.getItem())) continue;
                double dmg = getWeaponDamage(stack);
                if (dmg > bestDmg) {
                    bestDmg = dmg;
                    bestSlot = i;
                }
            }
            if (bestSlot != -1 && bestSlot != InventoryUtility.getSelectedSlot(mc.player) && bestDmg > 1.0) {
                if (swapMode.getValue().equals("Silent")) {
                    mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(bestSlot));
                } else {
                    InventoryUtility.selectSlot(mc.player, bestSlot);
                }
            }
        }
    }
    private boolean isSword(net.minecraft.world.item.Item item) {
        return item == net.minecraft.world.item.Items.WOODEN_SWORD ||
               item == net.minecraft.world.item.Items.STONE_SWORD ||
               item == net.minecraft.world.item.Items.IRON_SWORD ||
               item == net.minecraft.world.item.Items.GOLDEN_SWORD ||
               item == net.minecraft.world.item.Items.DIAMOND_SWORD ||
               item == net.minecraft.world.item.Items.NETHERITE_SWORD;
    }
    private double getWeaponDamage(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        String name = stack.getItem().toString().toLowerCase();
        double dmg = 0.0;
        if (name.contains("netherite_sword")) dmg = 8.0;
        else if (name.contains("diamond_sword")) dmg = 7.0;
        else if (name.contains("netherite_axe")) dmg = 7.0;
        else if (name.contains("mace")) dmg = 6.5;
        else if (name.contains("diamond_axe")) dmg = 6.0;
        else if (name.contains("iron_sword")) dmg = 6.0;
        else if (name.contains("iron_axe")) dmg = 5.0;
        else if (name.contains("stone_sword")) dmg = 5.0;
        else if (name.contains("stone_axe")) dmg = 4.0;
        else if (name.contains("golden_sword") || name.contains("wooden_sword")) dmg = 4.0;
        return dmg;
    }
}
