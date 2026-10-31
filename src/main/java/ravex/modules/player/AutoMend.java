package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.network.NetworkUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.entity.EquipmentSlot;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "AutoMend", category = "Player")
public class AutoMend {
    // start mending below this percent of max durability
    @Parameter(name = "Durability", min = 1.0, max = 100.0, step = 1.0)
    public double threshold = 100.0;
    // silent downward aim, camera stays still
    @Parameter(name = "Rotations")
    public boolean rotations = true;
    // skip pieces without mending, bottles are wasted
    @Parameter(name = "MendingOnly")
    public boolean mendingOnly = false;
    @Parameter(name = "Swap", modes = {"Silent", "Normal"})
    public String swapMode = "Silent";

    // tilt shown on the body, camera never moves
    public static volatile float visiblePitch = Float.NaN;
    public static volatile int visiblePitchTicks = 0;

    public void onDisable() {
        visiblePitch = Float.NaN;
        visiblePitchTicks = 0;
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        if (visiblePitchTicks > 0 && --visiblePitchTicks == 0) {
            visiblePitch = Float.NaN;
        }
        if (mc.getCurrentScreen() != null) return;

        if (!needsMend(p)) return;
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

        float oldPitch = p.getXRot();
        if (rotations) {
            NetworkUtility.sendRot(p.getYRot(), 90.0F, p.onGround(), p.horizontalCollision);
            p.setXRot(90.0F);
            visiblePitch = 90.0F;
            visiblePitchTicks = 12;
        }
        mc.getGameMode().useItem(p, net.minecraft.world.InteractionHand.MAIN_HAND);
        SwingUtility.swing(p, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (rotations) {
            p.setXRot(oldPitch);
        }

        if (silent && prevSlot != expSlot) {
            InventoryUtility.selectSlot(p, prevSlot);
        }
    }

    // armor and both hands, only pieces below the percent count
    private boolean needsMend(net.minecraft.client.player.LocalPlayer p) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : slots) {
            if (belowThreshold(p.getItemBySlot(slot))) return true;
        }
        return belowThreshold(p.getMainHandItem()) || belowThreshold(p.getOffhandItem());
    }

    private boolean belowThreshold(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem() || !stack.isDamaged()) return false;
        if (mendingOnly && !InventoryUtility.hasEnchantment(stack, "minecraft:mending")) return false;
        double maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) return false;
        double durabilityPct = ((maxDamage - stack.getDamageValue()) / maxDamage) * 100.0;
        return durabilityPct <= threshold;
    }
}
