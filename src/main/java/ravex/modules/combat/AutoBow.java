package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.item.BowItem;

import ravex.utility.player.InventoryUtility;
import ravex.utility.network.NetworkUtility;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "AutoBow", category = "Combat")
public class AutoBow implements ModuleAccess {
    @Parameter(name = "Charge", min = 10.0, max = 100.0, step = 1.0)
    public double charge = 95.0;
    @Parameter(name = "Silent")
    public boolean silent = true;
    @Parameter(name = "AutoSwitch")
    public boolean autoSwitch = false;
    @Parameter(name = "OnlyWhenTarget")
    public boolean onlyWhenTarget = false;
    private long lastAction = 0;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getPlayer().connection == null) return;
        long now = System.currentTimeMillis();
        if (now - lastAction < 100) return;
        boolean holdingBow = InventoryUtility.isBow(mc.getPlayer().getMainHandItem());
        if (!holdingBow && !autoSwitch) return;
        int bowSlot = -1;
        if (!holdingBow) {
            bowSlot = findBowSlot(mc);
            if (bowSlot == -1) return;
        }
        if (!mc.getPlayer().isUsingItem()) return;
        if (!mc.getPlayer().getUsedItemHand().equals(net.minecraft.world.InteractionHand.MAIN_HAND)) return;
        if (onlyWhenTarget && !(mc.getHitResult() instanceof net.minecraft.world.phys.EntityHitResult)) return;
        float chargeProgress = mc.getPlayer().getTicksUsingItem() / 20.0f;
        chargeProgress = Math.min(chargeProgress, 1.0f);
        float requiredCharge = (float) charge / 100.0f;
        if (chargeProgress < requiredCharge) return;
        if (bowSlot != -1 && silent) {
            NetworkUtility.sendSetCarriedItem(bowSlot);
        } else if (bowSlot != -1) {
            InventoryUtility.selectSlot(mc.getPlayer(), bowSlot);
        }
        mc.getPlayer().connection.send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
            net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.DOWN, 0
        ));
        lastAction = now;
    }
    private int findBowSlot(MinecraftWrapper mc) {
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.isBow(InventoryUtility.getItem(mc.getPlayer(), i))) return i;
        }
        return -1;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoBow").getEnabled();
    }
    public static AutoBow itz() {
        return ravex.manager.ModuleManager.delegate(AutoBow.class);
    }


}