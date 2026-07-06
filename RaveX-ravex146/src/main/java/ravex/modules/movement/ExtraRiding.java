package ravex.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;

public class ExtraRiding extends Module {
    public static final ExtraRiding INSTANCE = new ExtraRiding();

    public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "Custom"));
    public final NumberParameter speed = new NumberParameter("Speed", 2.0, 1.0, 5.0, 0.1);

    private ExtraRiding() {
        super("ExtraRiding", Category.MOVEMENT);
        addParameter(mode);
        addParameter(speed);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) return;

        double mult = mode.getValue().equals("Custom") ? speed.getValue() : 2.0;
        Vec3 motion = vehicle.getDeltaMovement();
        vehicle.setDeltaMovement(motion.x * mult, motion.y, motion.z * mult);
    }
}
