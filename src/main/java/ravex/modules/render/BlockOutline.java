package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import java.util.List;

@ModuleInfo(name = "BlockOutline", category = "Render")
public class BlockOutline extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Thin", List.of("Thin", "Thick"));
    public final ColorParameter color = new ColorParameter("Color", 0xFFFFFF55);
    public final BooleanParameter filled = new BooleanParameter("Filled", true);
    public final BooleanParameter smooth = new BooleanParameter("Smooth", false);
    
    public static boolean vanillaOutlineEnabled = true;

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("BlockOutline").getEnabled();
    }

    public static BlockOutline itz() {
        return ravex.manager.ModuleManager.delegate(BlockOutline.class);
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