package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.player.PlayerUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class AntiVoid extends Module {
    public static final AntiVoid INSTANCE = new AntiVoid();

    public final NumberParameter fallDistance = new NumberParameter("Distance", 5.0, 1.0, 10.0, 0.5);
    public final ModeParameter mode = new ModeParameter("Mode", "Teleport", List.of("Teleport", "Bounce"));

    private Vec3 lastOnGroundPos = null;

    private AntiVoid() {
        super("AntiVoid", Category.MOVEMENT);
        addParameter(fallDistance);
        addParameter(mode);
    }

    @Override
    protected void onEnable() {
        lastOnGroundPos = null;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        if (p.onGround()) {
            lastOnGroundPos = p.position();
        } else if (lastOnGroundPos != null && PlayerUtility.isOverVoid()) {
            if (lastOnGroundPos.y - p.getY() > fallDistance.getValue()) {
                if (mode.getValue().equals("Teleport")) {
                    p.setDeltaMovement(0, 0, 0);
                    p.teleportTo(lastOnGroundPos.x, lastOnGroundPos.y, lastOnGroundPos.z);
                } else if (mode.getValue().equals("Bounce")) {
                    p.setDeltaMovement(p.getDeltaMovement().x, 0.45, p.getDeltaMovement().z);
                }
            }
        }
    }
}
