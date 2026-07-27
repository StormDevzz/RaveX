package ravex.modules;
import com.google.gson.JsonObject;
import ravex.manager.ModuleManager;
import net.minecraft.client.gui.GuiGraphics;
import ravex.event.EventBusHolder;
import ravex.event.combat.ModuleToggleEvent;

import ravex.event.client.SoundEvent;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.MultiSelectParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.Parameter;
import ravex.parameter.StringParameter;
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
    @Unmodifiable
    public List<Parameter<?>> getParameters() {
        if (!paramFieldsScanned) {
            paramFieldsScanned = true;
            scanParameterFields();
        }
        return parameters;
    }
    protected void scanParameterFields() {
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
                } else if (field.isAnnotationPresent(ravex.modules.annotations.Parameter.class)) {
                    field.setAccessible(true);
                    ravex.modules.annotations.Parameter ann = field.getAnnotation(ravex.modules.annotations.Parameter.class);
                    Parameter<?> param = createParameterFromAnnotation(field, ann);
                    if (param != null && !parameters.contains(param)) {
                        param.bind(field, this);
                        parameters.add(param);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private Parameter<?> createParameterFromAnnotation(java.lang.reflect.Field field, ravex.modules.annotations.Parameter ann) {
        Class<?> type = field.getType();
        String name = ann.name();
        if (type == boolean.class || type == Boolean.class) {
            try {
                return new BooleanParameter(name, field.getBoolean(this));
            } catch (IllegalAccessException e) {
                return new BooleanParameter(name, false);
            }
        }
        if (ann.color() && (type == int.class || type == Integer.class)) {
            try {
                return new ColorParameter(name, field.getInt(this));
            } catch (IllegalAccessException e) {
                return new ColorParameter(name, 0xFFFFFFFF);
            }
        }
        if (type == int.class || type == Integer.class) {
            try {
                return new NumberParameter(name, field.getInt(this), ann.min(), ann.max(), ann.step());
            } catch (IllegalAccessException e) {
                return new NumberParameter(name, 0, ann.min(), ann.max(), ann.step());
            }
        }
        if (type == double.class || type == Double.class) {
            try {
                return new NumberParameter(name, field.getDouble(this), ann.min(), ann.max(), ann.step());
            } catch (IllegalAccessException e) {
                return new NumberParameter(name, 0.0, ann.min(), ann.max(), ann.step());
            }
        }
        if (type == float.class || type == Float.class) {
            try {
                return new NumberParameter(name, field.getFloat(this), ann.min(), ann.max(), ann.step());
            } catch (IllegalAccessException e) {
                return new NumberParameter(name, 0.0, ann.min(), ann.max(), ann.step());
            }
        }
        if (type == String.class) {
            String[] modes = ann.modes();
            if (modes.length > 0) {
                String defaultValue;
                try {
                    defaultValue = (String) field.get(this);
                } catch (IllegalAccessException e) {
                    defaultValue = modes[0];
                }
                return new ModeParameter(name, defaultValue, java.util.Arrays.asList(modes));
            }
            try {
                return new StringParameter(name, (String) field.get(this));
            } catch (IllegalAccessException e) {
                return new StringParameter(name, "");
            }
        }
        if (java.util.List.class.isAssignableFrom(type)) {
            String[] options = ann.options().length > 0 ? ann.options() : ann.modes();
            if (options.length > 0) {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.List<String> val = (java.util.List<String>) field.get(this);
                    return new MultiSelectParameter(name, val != null ? val : new java.util.ArrayList<>(), java.util.Arrays.asList(options));
                } catch (IllegalAccessException e) {
                    return new MultiSelectParameter(name, new java.util.ArrayList<>(), java.util.Arrays.asList(options));
                }
            }
        }
        return null;
    }
    protected void addParameter(Parameter<?> p) {
        parameters.add(p);
    }
    protected void onEnable() {}
    protected void onDisable() {}

    protected void ensureNativeLoaded() {
        if (hud) return;
        try {
            java.lang.reflect.Field f = getClass().getDeclaredField("NATIVE");
            f.setAccessible(true);
            Object lib = f.get(null);
            if (lib instanceof ravex.utility.nativelib.NativeLibraryUtility) {
                ((ravex.utility.nativelib.NativeLibraryUtility) lib).load();
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
