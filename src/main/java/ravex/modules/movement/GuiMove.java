package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "GuiMove", category = "Movement")
public class GuiMove {
    @Parameter(name = "Mode", modes = {"Vanilla", "NoClick", "NCPStrict", "Grim", "Matrix"})
    public String mode = "Vanilla";
    @Parameter(name = "Sneak")
    public boolean sneak = false;
    @Parameter(name = "NoJump")
    public boolean noJump = false;
    @Parameter(name = "NoSprint")
    public boolean noSprint = false;
    @Parameter(name = "DisableInClickGui")
    public boolean disableInClickGui = false;
}
