package ravex.modules;

import com.google.gson.JsonObject;
import ravex.parameter.Parameter;
import ravex.utility.sound.SoundUtility;
import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    private final String name;
    private final Category category;
    private boolean enabled;
    private int keyBind = org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;
    private final List<Parameter<?>> parameters = new ArrayList<>();
    private float gearAngle = 0f;
    private long gearLastTick = 0L;

    public float getGearAngle() { return gearAngle; }
    public long getGearLastTick() { return gearLastTick; }
    public void setGearAngle(float angle, long tickTime) {
        this.gearAngle = angle % 360f;
        this.gearLastTick = tickTime;
    }

    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
        this.enabled = false;
        this.gearLastTick = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public String getDescription() {
        return ravex.descriptions.ClickGuiDescriptions.getDescription(name);
    }

    private ravex.parameter.ModuleCondition enableCondition = () -> true;

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnableCondition(ravex.parameter.ModuleCondition condition) {
        this.enableCondition = condition;
    }

    public void setEnabled(boolean enabled) {
        if (isToggleLocked()) return;
        if (enabled && !enableCondition.canEnable()) {
            SoundUtility.playFailure();
            return;
        }
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                onEnable();
                SoundUtility.playEnable();
                if (ravex.modules.client.Notifications.INSTANCE != null) {
                    ravex.modules.client.Notifications.notifyToggle(this, true);
                }
            } else {
                onDisable();
                SoundUtility.playDisable();
                if (ravex.modules.client.Notifications.INSTANCE != null) {
                    ravex.modules.client.Notifications.notifyToggle(this, false);
                }
            }
            ravex.modules.client.DesktopGui.onModuleToggle(this.getName(), enabled);
        }
    }

    public boolean isToggleLocked() {
        for (Parameter<?> p : parameters) {
            if (p instanceof ravex.parameter.ToggleLockParameter tlp && tlp.getValue()) {
                return true;
            }
        }
        return false;
    }

    public void toggle() {
        if (isToggleLocked()) return;
        setEnabled(!enabled);
    }

    public int getKeyBind() {
        return keyBind;
    }

    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind;
    }

    public List<Parameter<?>> getParameters() {
        return parameters;
    }

    protected void addParameter(Parameter<?> p) {
        parameters.add(p);
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}

    public void saveExtra(JsonObject obj) {}
    public void loadExtra(JsonObject obj) {}
}
