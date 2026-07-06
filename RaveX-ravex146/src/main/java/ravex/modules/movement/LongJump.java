package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class LongJump extends Module {
    public static final LongJump INSTANCE = new LongJump();

    public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "Good", "Custom"));
    public final NumberParameter customBoost = new NumberParameter("Boost", 2.0, 1.0, 5.0, 0.1);

    private boolean boosted = false;

    private LongJump() {
        super("LongJump", Category.MOVEMENT);
        addParameter(mode);
        addParameter(customBoost);
    }

    @Override
    protected void onEnable() {
        boosted = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (mc.player.onGround()) {
            boosted = false;
        } else {
            if (!boosted) {
                double speed = 1.0;
                String m = mode.getValue();
                if (m.equals("Normal")) {
                    speed = 1.4;
                } else if (m.equals("Good")) {
                    speed = 1.8;
                } else if (m.equals("Custom")) {
                    speed = customBoost.getValue();
                }

                Vec3 motion = mc.player.getDeltaMovement();
                mc.player.setDeltaMovement(motion.x * speed, motion.y + 0.05, motion.z * speed);
                boosted = true;
            }
        }
    }
}
