package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.misc.NativeLoader;
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

    static {
        NativeLoader.load();
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
        } else if (lastOnGroundPos != null && lastOnGroundPos.y - p.getY() > fallDistance.getValue()) {
            boolean isVoid = false;
            try {
                isVoid = nativeIsVoidFall(p.getY(), p.getDeltaMovement().y,
                    mc.level.getMinY(), fallDistance.getValue());
            } catch (UnsatisfiedLinkError e) {
                isVoid = p.getY() < mc.level.getMinY() && p.getDeltaMovement().y < 0;
            }

            if (isVoid) {
                if (mode.getValue().equals("Teleport")) {
                    p.setDeltaMovement(0, 0, 0);
                    p.teleportTo(lastOnGroundPos.x, lastOnGroundPos.y, lastOnGroundPos.z);
                } else if (mode.getValue().equals("Bounce")) {
                    p.setDeltaMovement(p.getDeltaMovement().x, 0.45, p.getDeltaMovement().z);
                }
            }
        }
    }

    private native boolean nativeIsVoidFall(double y, double motionY, int worldMinY, double fallDist);
}
