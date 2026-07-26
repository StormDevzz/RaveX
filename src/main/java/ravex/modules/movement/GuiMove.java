package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
@ModuleInfo(name = "GuiMove", category = "Movement")
public class GuiMove implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Vanilla", "NoClick", "NCPStrict", "Grim", "Matrix"})
    public String mode = "Vanilla";
    @Parameter(name = "Sneak")
    public boolean sneak = false;
    @Parameter(name = "NoJump")
    public boolean noJump = false;
    @Parameter(name = "NoSprint")
    public boolean noSprint = false;
    public Screen closedScreen = null;
    public int grimCooldown = 0;
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("GuiMove").getEnabled();
    }
    public static GuiMove itz() {
        return ravex.manager.ModuleManager.delegate(GuiMove.class);
    }


}