package ravex.modules;
import com.google.gson.JsonObject;
import ravex.manager.ModuleManager;
import net.minecraft.client.gui.GuiGraphics;
import ravex.event.EventBusHolder;
import ravex.event.combat.ModuleToggleEvent;

import ravex.event.client.SoundEvent;
import ravex.parameter.Parameter;
import ravex.parameter.ParameterFactory;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
public abstract class Module {
    protected static <T extends Module> T self(Class<T> type) {
        return ModuleManager.get(type);
    }
    protected static boolean maybeEnabled(Class<? extends Module> type) {
        Module m = ModuleManager.get(type);
        return m != null && m.getEnabled();
    }
    private String name;
    private String category;
    protected boolean enabled;
    private int keyBind = org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;
    private final List<Parameter<?>> parameters = new ArrayList<>();
    private boolean paramFieldsScanned = false;
    private float gearAngle = 0f;
    private long gearLastTick = 0L;
    protected boolean hud;
    private int targetX;
    private int targetY;
    private int width;
    private int height;
    private float displayX;
    private float displayY;
    private boolean animInitialized = false;
    @Contract(pure = true)
    public float getGearAngle() { return gearAngle; }
    @Contract(pure = true)
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
    public void setCategory(String category) {
        this.category = category;
    }
    protected void setName(String name) {
        this.name = name;
    }
    public Module(String name, int defaultX, int defaultY, int width, int height) {
        this.name = name;
        this.category = "Custom";
        this.enabled = false;
        this.hud = true;
        this.targetX = defaultX;
        this.targetY = defaultY;
        this.width = width;
        this.height = height;
        this.gearLastTick = System.currentTimeMillis();
    }
    @Contract(pure = true)
    public boolean isHud() { return hud; }
    public void setHud(boolean hud) { this.hud = hud; }
    @Contract(pure = true)
    public String getName() {
        return name != null ? name : getClass().getSimpleName();
    }
    @Contract(pure = true)
    public String getCategory() {
        return category;
    }
    @Nullable
    public String getDescription() {
        String translated = ravex.utility.misc.LanguageUtility.getDescription(getName());
        if (translated != null && !translated.startsWith("desc_")) return translated;
        if (hud) return ravex.gui.descriptions.HudDescriptions.getDescription(getName());
        return ravex.gui.descriptions.ClickGuiDescriptions.getDescription(getName());
    }
    private ravex.parameter.ModuleCondition enableCondition = () -> true;
    private java.util.function.Supplier<Boolean> visibleCondition = () -> true;
    @Contract(pure = true)
    public boolean getEnabled() {
        return enabled;
    }
    public void setEnableCondition(ravex.parameter.ModuleCondition condition) {
        this.enableCondition = condition;
    }
    @Contract(pure = true)
    public boolean isVisible() {
        return visibleCondition.get();
    }
    public void setVisibleCondition(java.util.function.Supplier<Boolean> condition) {
        this.visibleCondition = condition != null ? condition : () -> true;
    }
    @Contract(pure = true)
    protected boolean hasToggleSound() {
        return !hud;
    }
    public void setEnabled(boolean enabled) {
        if (enabled) ensureNativeLoaded();
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
            EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.FAILURE));
            return;
        }
        if (this.enabled != enabled) {
            this.enabled = enabled;
            var bus = EventBusHolder.get();
            if (enabled) {
                onEnable();
                if (hasToggleSound()) bus.post(new SoundEvent(SoundEvent.Type.ENABLE));
            } else {
                onDisable();
                if (hasToggleSound()) bus.post(new SoundEvent(SoundEvent.Type.DISABLE));
            }
            bus.post(new ModuleToggleEvent(this, enabled));
        }
    }
    @Contract(pure = true)
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
    @Contract(pure = true)
    public int getKeyBind() {
        return keyBind;
    }
    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind;
    }
    @Contract(pure = true)
    public boolean consumesKeyBindPress() {
        return false;
    }
    @Unmodifiable
    public List<Parameter<?>> getParameters() {
        if (!paramFieldsScanned) {
            paramFieldsScanned = true;
            scanParameterFields();
        }
        return parameters;
    }
    protected void scanParameterFields() {
        for (Parameter<?> p : ParameterFactory.scan(this, Module.class)) {
            parameters.add(p);
        }
    }

    protected void addParameter(Parameter<?> p) {
        parameters.add(p);
    }
    protected void onEnable() {}
    protected void onDisable() {}

    protected void ensureNativeLoaded() {
        if (hud) return;
        try {
            Class<?> target = this instanceof ModuleProxy proxy
                ? proxy.getComponent().getClass()
                : getClass();
            java.lang.reflect.Field f = target.getDeclaredField("NATIVE");
            f.setAccessible(true);
            Object lib = f.get(null);
            if (lib instanceof ravex.utility.nativelib.NativeLibraryUtility nat) {
                nat.load();
            }
        } catch (Exception ignored) {}
    }
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
    @Contract(pure = true)
    public float getDisplayX() { return displayX; }
    public void setDisplayX(float v) { this.displayX = v; }
    @Contract(pure = true)
    public float getDisplayY() { return displayY; }
    public void setDisplayY(float v) { this.displayY = v; }
    @Contract(pure = true)
    public boolean isAnimInitialized() { return animInitialized; }
    public void setAnimInitialized(boolean v) { this.animInitialized = v; }
    @Contract(pure = true)
    public int getX() { return Math.round(displayX); }
    @Contract(pure = true)
    public int getY() { return Math.round(displayY); }
    @Contract(pure = true)
    public int getTargetX() { return targetX; }
    @Contract(pure = true)
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
    @Contract(pure = true)
    public int getWidth() { return width; }
    public void setWidth(int w) { this.width = w; }
    @Contract(pure = true)
    public int getHeight() { return height; }
    public void setHeight(int h) { this.height = h; }
}
