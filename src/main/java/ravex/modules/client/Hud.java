package ravex.modules.client;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;

import ravex.parameter.NumberParameter;
import ravex.parameter.ColorParameter;

@ModuleInfo(name = "Hud", category = "Client")
public class Hud extends ravex.modules.Module {
public final BooleanParameter hudEditor = new BooleanParameter("HudEditor", false);
    public final NumberParameter editorOpacity = new NumberParameter("EditorOpacity", 120, 0, 255, 1);
    public final BooleanParameter editorBackground = new BooleanParameter("EditorBackground", false);
    public final BooleanParameter editorBlur = new BooleanParameter("EditorBlur", true);
    public final BooleanParameter dragEnabled = new BooleanParameter("Drag", false);
    public final ColorParameter panelColor = new ColorParameter("PanelColor", 0x00000000);
    public final BooleanParameter showCounter = new BooleanParameter("ShowCounter", true);

    public static ravex.modules.Module draggingHud = null;
    public static int dragOffX = 0;
    public static int dragOffY = 0;

    private Hud() {
        
        enabled = true;
    }
    public void onTick() {
        if (hudEditor.getValue()) {
            hudEditor.setValue(false);
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                mc.execute(() -> mc.setScreen(new ravex.gui.hudeditor.HudEditorScreen(null)));
            }
        }
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Hud").getEnabled();
    }

    public static Hud itz() {
        return ravex.manager.ModuleManager.delegate(Hud.class);
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