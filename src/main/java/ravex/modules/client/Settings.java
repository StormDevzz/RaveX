package ravex.modules.client;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.ModeParameter;

import ravex.parameter.NumberParameter;
import ravex.parameter.Parameter;
@ModuleInfo(name = "Settings", category = "Client")
public class Settings extends ravex.modules.Module {
public final NumberParameter headerTextX = new NumberParameter("HeaderTextX", 24, 10, 60, 1);
    public final NumberParameter moduleTextX = new NumberParameter("ModuleTextX", 9, 3, 30, 1);
    public final ravex.parameter.ColorParameter menuColor = new ravex.parameter.ColorParameter("MenuColor", 0xFF0066FF);
    public final Parameter<String> language = new ModeParameter("Language", "English", java.util.List.of("English", "Russian"));
    private String prevLanguage = "English";
    private Settings() {
        
        enabled = true;
    }
    public void onTick() {
        String lang = language.getValue();
        if (!lang.equals(prevLanguage)) {
            prevLanguage = lang;
            ravex.utility.misc.LanguageUtility.setLanguage(lang);
        }
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Settings").getEnabled();
    }

    public static Settings itz() {
        return ravex.manager.ModuleManager.delegate(Settings.class);
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