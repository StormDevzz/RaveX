package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.movement.VoidUtility;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;



@Module(name = "AntiVoid", category = "Movement")
public class AntiVoid {
    @Parameter(name = "Distance", min = 1.0, max = 10.0, step = 0.5)
    public double fallDistance = 5.0;
    @Parameter(name = "Mode", modes = {"Teleport", "Bounce"})
    public String mode = "Teleport";
    private net.minecraft.world.phys.Vec3 lastOnGroundPos = null;
    public void onEnable() {
        lastOnGroundPos = null;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        net.minecraft.client.player.LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (p.onGround()) {
            lastOnGroundPos = p.position();
        } else if (lastOnGroundPos != null && lastOnGroundPos.y - p.getY() > fallDistance) {
            if (VoidUtility.isFallingIntoVoid(p)) {
                if (mode.equals("Teleport")) {
                    p.setDeltaMovement(0, 0, 0);
                    p.teleportTo(lastOnGroundPos.x, lastOnGroundPos.y, lastOnGroundPos.z);
                } else if (mode.equals("Bounce")) {
                    p.setDeltaMovement(p.getDeltaMovement().x, 0.45, p.getDeltaMovement().z);
                }
            }
        }
    }



}