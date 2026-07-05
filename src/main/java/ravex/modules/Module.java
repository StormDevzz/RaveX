package ravex.modules;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import ravex.parameter.Parameter;
import ravex.utility.sound.SoundUtility;
import java.util.ArrayList;
import java.util.List;
public abstract class Module {
    private String name;
    private Category category;
    private boolean enabled;
    private int keyBind = org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;
    private final List<Parameter<?>> parameters = new ArrayList<>();
    private boolean paramFieldsScanned = false;
    private float gearAngle = 0f;
    private long gearLastTick = 0L;
    protected final boolean hud;
    private int targetX;
    private int targetY;
    private int width;
    private int height;
    private float displayX;
    private float displayY;
    private boolean animInitialized = false;
    public float getGearAngle() { return gearAngle; }
    public long getGearLastTick() { return gearLastTick; }
    public void setGearAngle(float angle, long tickTime) {
        this.gearAngle = angle % 360f;
        this.gearLastTick = tickTime;
    }
    protected Module() {
        this.enabled = false;
        this.hud = false;
        this.gearLastTick = System.currentTimeMillis();
    }

    public Module(String name) {
        this.name = name;
        this.enabled = false;
        this.hud = false;
        this.gearLastTick = System.currentTimeMillis();
    }
    public void setCategory(Category category) {
        this.category = category;
    }
    public Module(String name, int defaultX, int defaultY, int width, int height) {
        this.name = name;
        this.category = Category.CUSTOM;
        this.enabled = false;
        this.hud = true;
        this.targetX = defaultX;
        this.targetY = defaultY;
        this.width = width;
        this.height = height;
        this.gearLastTick = System.currentTimeMillis();
    }
    public boolean isHud() { return hud; }
    public String getName() {
        return name != null ? name : getClass().getSimpleName();
    }
    public Category getCategory() {
        return category;
    }
    public String getDescription() {
        if (hud) return ravex.gui.descriptions.HudDescriptions.getDescription(getName());
        return ravex.gui.descriptions.ClickGuiDescriptions.getDescription(getName());
    }
    private ravex.parameter.ModuleCondition enableCondition = () -> true;
    private java.util.function.Supplier<Boolean> visibleCondition = () -> true;
    public boolean getEnabled() {
        return enabled;
    }
    public void setEnableCondition(ravex.parameter.ModuleCondition condition) {
        this.enableCondition = condition;
    }
    public boolean isVisible() {
        return visibleCondition.get();
    }
    public void setVisibleCondition(java.util.function.Supplier<Boolean> condition) {
        this.visibleCondition = condition != null ? condition : () -> true;
    }
    protected boolean hasToggleSound() {
        return !hud;
    }
    public void setEnabled(boolean enabled) {
        if (hud) {
            if (this.enabled != enabled) {
                this.enabled = enabled;
                if (enabled) onEnable();
                else onDisable();
            }
            return;
        }
        if (isToggleLocked()) return;
        if (enabled && !enableCondition.canEnable()) {
            SoundUtility.playFailure();
            return;
        }
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                onEnable();
                if (hasToggleSound()) SoundUtility.playEnable();
                if (ravex.modules.client.Notifications.INSTANCE != null) {
                    ravex.modules.client.Notifications.notifyToggle(this, true);
                }
            } else {
                onDisable();
                if (hasToggleSound()) SoundUtility.playDisable();
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
        if (!hud && isToggleLocked()) return;
        setEnabled(!enabled);
    }
    public int getKeyBind() {
        return keyBind;
    }
    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind;
    }
    public List<Parameter<?>> getParameters() {
        if (!paramFieldsScanned) {
            paramFieldsScanned = true;
            scanParameterFields();
        }
        return parameters;
    }
    private void scanParameterFields() {
        Class<?> clazz = getClass();
        while (clazz != Module.class && clazz != null) {
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                if (Parameter.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    try {
                        Parameter<?> param = (Parameter<?>) field.get(this);
                        if (param != null && !parameters.contains(param)) {
                            parameters.add(param);
                        }
                    } catch (IllegalAccessException ignored) {}
                }
            }
            clazz = clazz.getSuperclass();
        }
    }
    protected void addParameter(Parameter<?> p) {
        parameters.add(p);
    }
    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
    public void render(GuiGraphics graphics, float partialTicks) {}
    public void saveExtra(JsonObject obj) {}
    public void loadExtra(JsonObject obj) {}
    public void updateAnimation() {
        if (!animInitialized) {
            displayX = targetX;
            displayY = targetY;
            animInitialized = true;
        }
        float speed = 0.25f;
        displayX += (targetX - displayX) * speed;
        displayY += (targetY - displayY) * speed;
        if (Math.abs(targetX - displayX) < 0.3f) displayX = targetX;
        if (Math.abs(targetY - displayY) < 0.3f) displayY = targetY;
    }
    public float getDisplayX() { return displayX; }
    public void setDisplayX(float v) { this.displayX = v; }
    public float getDisplayY() { return displayY; }
    public void setDisplayY(float v) { this.displayY = v; }
    public boolean isAnimInitialized() { return animInitialized; }
    public void setAnimInitialized(boolean v) { this.animInitialized = v; }
    public int getX() { return Math.round(displayX); }
    public int getY() { return Math.round(displayY); }
    public int getTargetX() { return targetX; }
    public int getTargetY() { return targetY; }
    public void setX(int x) {
        this.targetX = x;
        if (!animInitialized) {
            displayX = x;
            animInitialized = true;
        }
    }
    public void setY(int y) {
        this.targetY = y;
        if (!animInitialized) {
            displayY = y;
            animInitialized = true;
        }
    }
    public int getWidth() { return width; }
    public void setWidth(int w) { this.width = w; }
    public int getHeight() { return height; }
    public void setHeight(int h) { this.height = h; }
}
