package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import java.util.List;

public class NoSlowDown extends Module {
    public static final NoSlowDown INSTANCE = new NoSlowDown();

    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla",
            List.of("Vanilla", "Grim", "NCP"));

    private NoSlowDown() {
        super("NoSlowDown", Category.MOVEMENT);
        addParameter(mode);
    }
}
