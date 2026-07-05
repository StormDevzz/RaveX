package ravex.modules.render;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
public class ShiftInterp extends Module {
    public static final ShiftInterp INSTANCE = new ShiftInterp();
    public final ModeParameter target = new ModeParameter("Target", "All", java.util.List.of("All", "Others", "Self"));

    public boolean shouldCrouch(Entity entity) {
        if (!getEnabled()) return false;
        if (!(entity instanceof Player)) return false;
        Minecraft mc = Minecraft.getInstance();
        boolean isSelf = (entity == mc.player);
        String t = target.getValue();
        if (t.equals("Self")) {
            return isSelf;
        } else if (t.equals("Others")) {
            return !isSelf;
        } else {
            return true;
        }
    }
}
