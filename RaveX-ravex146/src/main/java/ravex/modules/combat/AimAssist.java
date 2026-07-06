package ravex.modules.combat;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.BowItem;

public class AimAssist extends Module {
    public static final AimAssist INSTANCE = new AimAssist();

    public final ModeParameter targetMode = new ModeParameter("Target", "Players", java.util.List.of("Players", "Monsters", "All"));
    public final NumberParameter fov = new NumberParameter("FOV", 45.0, 10.0, 180.0, 5.0);
    public final NumberParameter speed = new NumberParameter("Speed", 5.0, 1.0, 20.0, 0.5);
    public final BooleanParameter bowOnly = new BooleanParameter("BowOnly", false);

    private AimAssist() {
        super("AimAssist", Category.COMBAT);
        addParameter(targetMode);
        addParameter(fov);
        addParameter(speed);
        addParameter(bowOnly);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (bowOnly.getValue() && !(mc.player.getMainHandItem().getItem() instanceof BowItem)) {
            return;
        }

        
        LivingEntity target = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity p)) continue;
            if (p == mc.player || !p.isAlive()) continue;

            
            String mode = targetMode.getValue();
            if (mode.equals("Players") && !(p instanceof Player)) continue;
            if (mode.equals("Monsters") && !(p instanceof Monster)) continue;

            double dist = mc.player.distanceTo(p);
            if (dist < bestDist && dist <= 40.0) {
                target = p;
                bestDist = dist;
            }
        }

        if (target != null) {
            
            double dx = target.getX() - mc.player.getX();
            double dy = (target.getY() + target.getEyeHeight(target.getPose())) - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
            double dz = target.getZ() - mc.player.getZ();

            double dh = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, dh));

            float diffYaw = yaw - mc.player.getYRot();
            float diffPitch = pitch - mc.player.getXRot();

            diffYaw = net.minecraft.util.Mth.wrapDegrees(diffYaw);
            diffPitch = net.minecraft.util.Mth.wrapDegrees(diffPitch);

            float speedVal = speed.getValue().floatValue();
            if (Math.abs(diffYaw) < fov.getValue()) {
                mc.player.setYRot(mc.player.getYRot() + (diffYaw / speedVal));
                mc.player.setXRot(mc.player.getXRot() + (diffPitch / speedVal));
            }
        }
    }
}
