package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
public class ShiftInterp extends Module {
=======
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
public class ShiftInterp extends Module {
    public static final ShiftInterp INSTANCE = new ShiftInterp();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(ShiftInterp.class);
    }

    public static ShiftInterp itz() {
        return ModuleManager.get(ShiftInterp.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
