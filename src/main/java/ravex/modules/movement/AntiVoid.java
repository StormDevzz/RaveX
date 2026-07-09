package ravex.modules.movement;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.movement.VoidUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.List;
public class AntiVoid extends Module {
<<<<<<< HEAD
=======
    public static final AntiVoid INSTANCE = new AntiVoid();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter fallDistance = new NumberParameter("Distance", 5.0, 1.0, 10.0, 0.5);
    public final ModeParameter mode = new ModeParameter("Mode", "Teleport", List.of("Teleport", "Bounce"));
    private Vec3 lastOnGroundPos = null;

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
            if (VoidUtility.isFallingIntoVoid(p)) {
                if (mode.getValue().equals("Teleport")) {
                    p.setDeltaMovement(0, 0, 0);
                    p.teleportTo(lastOnGroundPos.x, lastOnGroundPos.y, lastOnGroundPos.z);
                } else if (mode.getValue().equals("Bounce")) {
                    p.setDeltaMovement(p.getDeltaMovement().x, 0.45, p.getDeltaMovement().z);
                }
            }
        }
    }
    public static AntiVoid itz() {
        return ModuleManager.get(AntiVoid.class);
    }
}
