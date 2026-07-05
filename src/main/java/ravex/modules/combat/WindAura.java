package ravex.modules.combat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.rotation.RotationUtility;
public class WindAura extends Module {
    public static final WindAura INSTANCE = new WindAura();
    public final ModeParameter mode = new ModeParameter("Mode", "Normal", java.util.List.of("Normal", "Silent"));
    public final NumberParameter range = new NumberParameter("Range", 10.0, 3.0, 30.0, 0.5);
    public final NumberParameter delay = new NumberParameter("Delay", 5.0, 1.0, 20.0, 1.0);
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter autoSwitch = new BooleanParameter("Auto Switch", true);
    private int tickCounter = 0;

    @Override
    protected void onEnable() {
        tickCounter = 0;
    }
    private int findWindChargeSlot() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.WIND_CHARGE)) return i;
        }
        return -1;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        tickCounter++;
        if (tickCounter < delay.getValue().intValue()) return;
        tickCounter = 0;
        double r = range.getValue();
        Entity target = null;
        double nearest = r + 1;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue;
            if (!(entity instanceof LivingEntity)) continue;
            if (!players.getValue() && entity instanceof Player) continue;
            double dist = mc.player.distanceTo(entity);
            if (dist > r) continue;
            if (dist < nearest) {
                nearest = dist;
                target = entity;
            }
        }
        if (target == null) return;
        if (autoSwitch.getValue()) {
            int slot = findWindChargeSlot();
            if (slot < 0) return;
            mc.player.getInventory().setSelectedSlot(slot);
        } else {
            if (!mc.player.getMainHandItem().is(Items.WIND_CHARGE)) return;
        }
        float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), target.getBoundingBox().getCenter());
        float yaw = angles[0], pitch = angles[1];
        if (mode.getValue().equals("Silent")) {
            float oldYaw = mc.player.getYRot();
            float oldPitch = mc.player.getXRot();
            mc.player.setYRot(yaw); mc.player.setXRot(pitch);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.setYRot(oldYaw); mc.player.setXRot(oldPitch);
        } else {
            mc.player.setYRot(yaw); mc.player.setXRot(pitch);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
    }
}
