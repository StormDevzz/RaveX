package ravex.integrations.baritone;

import java.lang.reflect.Method;

public class BaritoneIntegration {
    private Object primaryBaritone;
    private Method cancelMethod;
    private Object settingsObject;
    private Method parseAndApply;
    private boolean available;
    private boolean debug = false;

    public static boolean isBaritonePresent() {
        try {
            Class.forName("baritone.api.BaritoneAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean init() {
        if (available) return true;
        try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Method getProvider = apiClass.getMethod("getProvider");
            Object provider = getProvider.invoke(null);
            if (provider == null) { if (debug) System.out.println("[RaveX] BaritoneIntegration: provider is null"); return false; }

            Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            if (baritone == null) { if (debug) System.out.println("[RaveX] BaritoneIntegration: primaryBaritone is null"); return false; }
            primaryBaritone = baritone;

            Object behavior = primaryBaritone.getClass().getMethod("getPathingBehavior").invoke(primaryBaritone);
            cancelMethod = behavior.getClass().getMethod("cancelEverything");

            settingsObject = apiClass.getMethod("getSettings").invoke(null);
            if (settingsObject == null) { if (debug) System.out.println("[RaveX] BaritoneIntegration: settingsObject is null"); return false; }

            Class<?> utilClass = Class.forName("baritone.api.utils.SettingsUtil");
            parseAndApply = utilClass.getMethod("parseAndApply", settingsObject.getClass(), String.class, String.class);

            if (debug) System.out.println("[RaveX] BaritoneIntegration: init OK");
            available = true;
            return true;
        } catch (Exception e) {
            if (debug) { System.out.println("[RaveX] BaritoneIntegration init FAILED: " + e); e.printStackTrace(System.out); }
            available = false;
            return false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public void cancelPathing() {
        if (!available || primaryBaritone == null) return;
        try {
            Object behavior = primaryBaritone.getClass().getMethod("getPathingBehavior").invoke(primaryBaritone);
            cancelMethod.invoke(behavior);
        } catch (Exception ignored) {
        }
    }

    public void applyBoolean(String settingName, boolean value) {
        if (!available) { if (debug) System.out.println("[RaveX] applyBoolean(" + settingName + "," + value + ") SKIPPED: not available"); return; }
        try {
            String lower = settingName.toLowerCase();
            String strVal = value ? "true" : "false";
            if (debug) System.out.println("[RaveX] applyBoolean: parseAndApply(settingsObject, \"" + lower + "\", \"" + strVal + "\")");
            parseAndApply.invoke(null, settingsObject, lower, strVal);
            if (debug) System.out.println("[RaveX] applyBoolean(" + settingName + "," + value + ") OK");
        } catch (Exception e) {
            if (debug) { System.out.println("[RaveX] applyBoolean FAILED: " + e); e.printStackTrace(System.out); }
        }
    }

    public void applyColor(String settingName, int argb) {
        if (!available) { if (debug) System.out.println("[RaveX] applyColor(" + settingName + "," + argb + ") SKIPPED: not available"); return; }
        try {
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            String strVal = r + "," + g + "," + b;
            if (debug) System.out.println("[RaveX] applyColor: parseAndApply(settingsObject, \"" + settingName.toLowerCase() + "\", \"" + strVal + "\")");
            parseAndApply.invoke(null, settingsObject, settingName.toLowerCase(), strVal);
            if (debug) System.out.println("[RaveX] applyColor(" + settingName + "," + argb + ") OK");
        } catch (Exception e) {
            if (debug) { System.out.println("[RaveX] applyColor FAILED: " + e); e.printStackTrace(System.out); }
        }
    }
}
