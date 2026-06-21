package ravex.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;

public class KeepSprint extends Module {
    public static final KeepSprint INSTANCE = new KeepSprint();

    public final NumberParameter speed = new NumberParameter("Speed", 100, 0, 100, 5);

    private KeepSprint() {
        super("KeepSprint", Category.COMBAT);
        addParameter(speed);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (mc.player.hurtTime > 0) {
            mc.player.setSprinting(true);
            double multiplier = speed.getValue() / 100.0;
            if (multiplier < 1.0) {
                Vec3 vel = mc.player.getDeltaMovement();
                mc.player.setDeltaMovement(vel.x * multiplier, vel.y, vel.z * multiplier);
            }
        }

        if (mc.player.hasEffect(MobEffects.BLINDNESS) && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }
    }
}
