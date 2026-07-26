package ravex.modules.combat;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import ravex.utility.misc.MobUtility;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.InventoryUtility;
@ModuleInfo(name = "WindAura", category = "Combat")
public class WindAura extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Normal", java.util.List.of("Normal", "Silent"));
    public final NumberParameter range = new NumberParameter("Range", 10.0, 3.0, 30.0, 0.5);
    public final NumberParameter delay = new NumberParameter("Delay", 5.0, 1.0, 20.0, 1.0);
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter autoSwitch = new BooleanParameter("AutoSwitch", true);
    private int tickCounter = 0;
    protected void onEnable() {
        tickCounter = 0;
    }
    private int findWindChargeSlot() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.isItem(InventoryUtility.getItem(mc.player, i), "wind_charge")) return i;
        }
        return -1;
    }
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
            var living = MobUtility.asLivingEntity(entity);
            if (living == null || MobUtility.isSelf(living)) continue;
            if (!players.getValue() && MobUtility.isPlayer(living)) continue;
            double dist = MobUtility.distanceToPlayer(entity);
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
            InventoryUtility.selectSlot(mc.player, slot);
        } else {
            if (!InventoryUtility.isItem(mc.player.getMainHandItem(), "wind_charge")) return;
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
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("WindAura").getEnabled();
    }
    public static WindAura itz() {
        return ravex.manager.ModuleManager.delegate(WindAura.class);
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