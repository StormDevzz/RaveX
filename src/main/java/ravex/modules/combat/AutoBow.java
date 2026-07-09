package ravex.modules.combat;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
<<<<<<< HEAD
=======
import net.minecraft.world.item.Items;
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
<<<<<<< HEAD
import ravex.utility.player.InventoryUtility;
public class AutoBow extends Module {
=======
public class AutoBow extends Module {
    public static final AutoBow INSTANCE = new AutoBow();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter charge = new NumberParameter("Charge", 95.0, 10.0, 100.0, 1.0);
    public final BooleanParameter silent = new BooleanParameter("Silent", true);
    public final BooleanParameter autoSwitch = new BooleanParameter("AutoSwitch", false);
    public final BooleanParameter onlyWhenTarget = new BooleanParameter("OnlyWhenTarget", false);
    private long lastAction = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        long now = System.currentTimeMillis();
        if (now - lastAction < 100) return;
<<<<<<< HEAD
        boolean holdingBow = InventoryUtility.isBow(mc.player.getMainHandItem());
=======
        boolean holdingBow = mc.player.getMainHandItem().is(Items.BOW);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (!holdingBow && !autoSwitch.getValue()) return;
        int bowSlot = -1;
        if (!holdingBow) {
            bowSlot = findBowSlot(mc);
            if (bowSlot == -1) return;
        }
        if (!mc.player.isUsingItem()) return;
        if (!mc.player.getUsedItemHand().equals(InteractionHand.MAIN_HAND)) return;
        if (onlyWhenTarget.getValue() && !(mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult)) return;
        float chargeProgress = mc.player.getTicksUsingItem() / 20.0f;
        chargeProgress = Math.min(chargeProgress, 1.0f);
        float requiredCharge = charge.getValue().floatValue() / 100.0f;
        if (chargeProgress < requiredCharge) return;
        if (bowSlot != -1 && silent.getValue()) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(bowSlot));
        } else if (bowSlot != -1) {
<<<<<<< HEAD
            InventoryUtility.selectSlot(mc.player, bowSlot);
=======
            mc.player.getInventory().setSelectedSlot(bowSlot);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
        mc.player.connection.send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
            BlockPos.ZERO, Direction.DOWN, 0
        ));
        lastAction = now;
    }
    private int findBowSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
<<<<<<< HEAD
            if (InventoryUtility.isBow(InventoryUtility.getItem(mc.player, i))) return i;
        }
        return -1;
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(AutoBow.class);
    }
    public static AutoBow itz() {
        return ModuleManager.get(AutoBow.class);
    }

}
=======
            if (mc.player.getInventory().getItem(i).is(Items.BOW)) return i;
        }
        return -1;
    }
}
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
