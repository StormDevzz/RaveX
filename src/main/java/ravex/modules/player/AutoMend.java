package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.EquipmentSlot;

public class AutoMend extends Module {
    public static final AutoMend INSTANCE = new AutoMend();

    public final NumberParameter threshold = new NumberParameter("Threshold %", 50.0, 10.0, 95.0, 5.0);
    public final BooleanParameter silent = new BooleanParameter("Silent Swap", true);

    private AutoMend() {
        super("AutoMend", Category.PLAYER);
        addParameter(threshold);
        addParameter(silent);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;

        // 1. Check if any armor piece is damaged and below the threshold
        boolean needsMend = false;
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = p.getItemBySlot(slot);
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

        // 2. Find experience bottle in the hotbar
        int expSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).is(Items.EXPERIENCE_BOTTLE)) {
                expSlot = i;
                break;
            }
        }

        if (expSlot == -1) return; // No XP bottles in the hotbar

        // 3. Perform mending throw under feet
        int prevSlot = p.getInventory().getSelectedSlot();

        if (silent.getValue()) {
            // Fast swap slot, throw packet-based look down, use item, swing hand, swap slot back
            p.getInventory().setSelectedSlot(expSlot);

            // Send look packet straight down so bottle spawns at feet (taking 4 arguments in 1.21.4)
            p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), 90.0F, p.onGround(), p.horizontalCollision));

            // Interact/use bottle
            mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
            p.swing(InteractionHand.MAIN_HAND);

            // Immediately swap back to the previous slot in the same tick
            p.getInventory().setSelectedSlot(prevSlot);
        } else {
            // Swap slot, look down, throw, swing, and keep slot active
            p.getInventory().setSelectedSlot(expSlot);
            p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), 90.0F, p.onGround(), p.horizontalCollision));
            mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
            p.swing(InteractionHand.MAIN_HAND);
        }
    }
}
