package ravex.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import ravex.modules.Category;
import ravex.modules.Module;

public class KeepSprint extends Module {
    public static final KeepSprint INSTANCE = new KeepSprint();

    private KeepSprint() {
        super("KeepSprint", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (mc.player.hurtTime > 0 && mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        }

        if (mc.player.hasEffect(MobEffects.BLINDNESS) && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }
    }
}
