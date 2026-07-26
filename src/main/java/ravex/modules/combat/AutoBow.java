package ravex.modules.combat;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.item.BowItem;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
@ModuleInfo(name = "AutoBow", category = "Combat")
public class AutoBow extends ravex.modules.Module {
public final NumberParameter charge = new NumberParameter("Charge", 95.0, 10.0, 100.0, 1.0);
    public final BooleanParameter silent = new BooleanParameter("Silent", true);
    public final BooleanParameter autoSwitch = new BooleanParameter("AutoSwitch", false);
    public final BooleanParameter onlyWhenTarget = new BooleanParameter("OnlyWhenTarget", false);
    private long lastAction = 0;
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        long now = System.currentTimeMillis();
        if (now - lastAction < 100) return;
        boolean holdingBow = InventoryUtility.isBow(mc.player.getMainHandItem());
        if (!holdingBow && !autoSwitch.getValue()) return;
        int bowSlot = -1;
        if (!holdingBow) {
            bowSlot = findBowSlot(mc);
            if (bowSlot == -1) return;
        }
        if (!mc.player.isUsingItem()) return;
        if (!mc.player.getUsedItemHand().equals(net.minecraft.world.InteractionHand.MAIN_HAND)) return;
        if (onlyWhenTarget.getValue() && !(mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult)) return;
        float chargeProgress = mc.player.getTicksUsingItem() / 20.0f;
        chargeProgress = Math.min(chargeProgress, 1.0f);
        float requiredCharge = charge.getValue().floatValue() / 100.0f;
        if (chargeProgress < requiredCharge) return;
        if (bowSlot != -1 && silent.getValue()) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(bowSlot));
        } else if (bowSlot != -1) {
            InventoryUtility.selectSlot(mc.player, bowSlot);
        }
        mc.player.connection.send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
            net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.DOWN, 0
        ));
        lastAction = now;
    }
    private int findBowSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.isBow(InventoryUtility.getItem(mc.player, i))) return i;
        }
        return -1;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoBow").getEnabled();
    }
    public static AutoBow itz() {
        return ravex.manager.ModuleManager.delegate(AutoBow.class);
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