package ravex.modules.movement;

import net.minecraft.client.Minecraft;
import ravex.modules.Category;
import ravex.modules.Module;

public class NoRotate extends Module {
    public static final NoRotate INSTANCE = new NoRotate();

    private float savedYaw;
    private float savedPitch;

    private NoRotate() {
        super("NoRotate", Category.MOVEMENT);
    }

    public void saveRotation() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            savedYaw = mc.player.getYRot();
            savedPitch = mc.player.getXRot();
        }
    }

    public void restoreRotation() {
        if (!getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.setYRot(savedYaw);
            mc.player.setXRot(savedPitch);
        }
    }
}
