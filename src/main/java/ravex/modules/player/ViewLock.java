package ravex.modules.player;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import net.minecraft.client.Minecraft;
public class ViewLock extends Module {
    public static final ViewLock INSTANCE = new ViewLock();
    public final BooleanParameter lockYaw = new BooleanParameter("LockYaw", true);
    public final BooleanParameter lockPitch = new BooleanParameter("LockPitch", true);

    public boolean shouldLockYaw(double yRot, double xRot) {
        return getEnabled() && lockYaw.getValue();
    }
    public boolean shouldLockPitch(double yRot, double xRot) {
        return getEnabled() && lockPitch.getValue();
    }
}
