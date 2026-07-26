package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.EntityUtility;

@ModuleInfo(name = "ShiftInterp", category = "Render")
public class ShiftInterp implements ModuleAccess {
    @Parameter(name = "Target", modes = {"All", "Others", "Self"})
    public String target = "All";

    public boolean shouldCrouch(net.minecraft.world.entity.Entity entity) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("ShiftInterp").getEnabled()) return false;
        if (!(entity instanceof net.minecraft.world.entity.player.Player)) return false;
        Minecraft mc = Minecraft.getInstance();
        boolean isSelf = (entity == mc.player);
        String t = target;
        if (t.equals("Self")) {
            return isSelf;
        } else if (t.equals("Others")) {
            return !isSelf;
        } else {
            return true;
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ShiftInterp").getEnabled();
    }

    public static ShiftInterp itz() {
        return ravex.manager.ModuleManager.delegate(ShiftInterp.class);
    }


}