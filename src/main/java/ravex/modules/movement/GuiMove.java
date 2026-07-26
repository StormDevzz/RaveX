package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.gui.screens.Screen;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import java.util.List;
@ModuleInfo(name = "GuiMove", category = "Movement")
public class GuiMove extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Vanilla", List.of("Vanilla", "NoClick", "NCPStrict", "Grim", "Matrix"));
    public final BooleanParameter sneak = new BooleanParameter("Sneak", false);
    public final BooleanParameter noJump = new BooleanParameter("NoJump", false);
    public final BooleanParameter noSprint = new BooleanParameter("NoSprint", false);
    public Screen closedScreen = null;
    public int grimCooldown = 0;
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("GuiMove").getEnabled();
    }
    public static GuiMove itz() {
        return ravex.manager.ModuleManager.delegate(GuiMove.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}