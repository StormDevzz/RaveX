package ravex.modules.player;

import com.google.gson.Gson;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class Swing extends Module {
    public static final Swing INSTANCE = new Swing();

    public final ModeParameter mode = new ModeParameter("Mode", "1.8", List.of("1.8", "1.12.2", "Custom"));
    public final NumberParameter duration = new NumberParameter("Duration", 6, 1, 20, 1);
    public final ModeParameter swingPath = new ModeParameter("Swing Path", "Normal", List.of("Normal", "Smooth", "Bounce", "Reverse"));
    public final NumberParameter swingCurve = new NumberParameter("Swing Curve", 1.0, 0.1, 5.0, 0.1);
    public final NumberParameter progressCap = new NumberParameter("Progress Cap", 1.0, 0.0, 1.0, 0.05);
    public final NumberParameter progressFloor = new NumberParameter("Progress Floor", 0.0, 0.0, 1.0, 0.05);
    public final BooleanParameter noEquip = new BooleanParameter("No Equip", false);
    public final BooleanParameter handAnimations = new BooleanParameter("Hand Animations", true);

    private static final String NEA_BASE_MOD = "dev.tr7zw.notenoughanimations.versionless.NEABaseMod";
    private static final String NEA_API = "dev.tr7zw.notenoughanimations.api.NotEnoughAnimationsApi";
    private static final String LOCO_MAIN = "com.trainguy9512.locomotion.LocomotionMain";

    private static final Set<String> HAND_FIELDS = Set.of(
            "enableOffhandHiding",
            "showLastUsedSword",
            "itemSwapAnimation",
            "customBowRotationLock",
            "clampCrossbowAnimations",
            "enableEatDrinkAnimation",
            "enableRotationLocking"
    );

    private Object neaConfig;
    private Class<?> neaConfigClass;
    private Method refreshEnabledAnimations;
    private String savedConfigJson;
    private String handConfigJson;
    private boolean neaAvailable;
    private boolean prevHandState = true;

    private Object locoConfig;
    private Object locoConfigData;
    private String savedLocoConfigJson;
    private boolean locomotionAvailable;

    private Swing() {
        super("Swing", Category.PLAYER);
        addParameter(mode);
        addParameter(duration);
        addParameter(swingPath);
        addParameter(swingCurve);
        addParameter(progressCap);
        addParameter(progressFloor);
        addParameter(noEquip);
        addParameter(handAnimations);

        duration.setVisible(() -> "Custom".equals(mode.getValue()));
        swingPath.setVisible(() -> "Custom".equals(mode.getValue()));
        swingCurve.setVisible(() -> "Custom".equals(mode.getValue()));
        progressCap.setVisible(() -> "Custom".equals(mode.getValue()));
        progressFloor.setVisible(() -> "Custom".equals(mode.getValue()));
        noEquip.setVisible(() -> "Custom".equals(mode.getValue()));

        initNEA();
        initLocomotion();
    }

    private void initNEA() {
        try {
            Class<?> neaBaseMod = Class.forName(NEA_BASE_MOD);
            neaConfig = neaBaseMod.getField("config").get(null);
            neaConfigClass = neaConfig.getClass();
            Class<?> api = Class.forName(NEA_API);
            refreshEnabledAnimations = api.getMethod("refreshEnabledAnimations");
            neaAvailable = true;
        } catch (Exception e) {
            neaAvailable = false;
        }
    }

    private void initLocomotion() {
        try {
            Class<?> mainClass = Class.forName(LOCO_MAIN);
            locoConfig = mainClass.getField("CONFIG").get(null);
            locoConfigData = locoConfig.getClass().getMethod("data").invoke(locoConfig);
            locomotionAvailable = true;
        } catch (Exception e) {
            locomotionAvailable = false;
        }
    }

    private void setNEAEnabled(boolean enabled) {
        if (!neaAvailable || neaConfig == null) return;
        try {
            if (enabled) {
                if (savedConfigJson != null) {
                    neaConfig = new Gson().fromJson(savedConfigJson, neaConfigClass);
                    Class.forName(NEA_BASE_MOD).getField("config").set(null, neaConfig);
                    savedConfigJson = null;
                    refreshEnabledAnimations.invoke(null);
                }
            } else {
                savedConfigJson = new Gson().toJson(neaConfig);
                neaConfig = neaConfigClass.getDeclaredConstructor().newInstance();
                for (Field field : neaConfigClass.getFields()) {
                    if (field.getType() == boolean.class) {
                        field.setBoolean(neaConfig, false);
                    }
                }
                Class.forName(NEA_BASE_MOD).getField("config").set(null, neaConfig);
                refreshEnabledAnimations.invoke(null);
            }
        } catch (Exception ignored) {
        }
    }

    private void setLocomotionEnabled(boolean enabled) {
        if (!locomotionAvailable || locoConfig == null) return;
        try {
            if (enabled) {
                if (savedLocoConfigJson != null) {
                    locoConfigData = new Gson().fromJson(savedLocoConfigJson, locoConfigData.getClass());
                    savedLocoConfigJson = null;
                }
                Field configDataField = locoConfig.getClass().getDeclaredField("configData");
                configDataField.setAccessible(true);
                configDataField.set(locoConfig, locoConfigData);
                Object firstPersonPlayer = locoConfigData.getClass().getField("firstPersonPlayer").get(locoConfigData);
                firstPersonPlayer.getClass().getField("enableRenderer").setBoolean(firstPersonPlayer, true);
                locoConfig.getClass().getMethod("save").invoke(locoConfig);
            } else {
                savedLocoConfigJson = new Gson().toJson(locoConfigData);
                Object firstPersonPlayer = locoConfigData.getClass().getField("firstPersonPlayer").get(locoConfigData);
                firstPersonPlayer.getClass().getField("enableRenderer").setBoolean(firstPersonPlayer, false);
                locoConfig.getClass().getMethod("save").invoke(locoConfig);
            }
        } catch (Exception ignored) {
        }
    }

    private void applyHandAnimations(boolean enabled) {
        if (!getEnabled() || !neaAvailable || neaConfig == null) return;
        try {
            if (!enabled) {
                if (handConfigJson == null) {
                    handConfigJson = new Gson().toJson(neaConfig);
                }
                for (Field field : neaConfigClass.getFields()) {
                    if (field.getType() == boolean.class && HAND_FIELDS.contains(field.getName())) {
                        field.setBoolean(neaConfig, false);
                    }
                }
                Class.forName(NEA_BASE_MOD).getField("config").set(null, neaConfig);
            } else {
                if (handConfigJson != null) {
                    neaConfig = new Gson().fromJson(handConfigJson, neaConfigClass);
                    Class.forName(NEA_BASE_MOD).getField("config").set(null, neaConfig);
                    handConfigJson = null;
                }
            }
            refreshEnabledAnimations.invoke(null);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        setNEAEnabled(true);
        prevHandState = handAnimations.getValue();
        if (prevHandState) {
            setLocomotionEnabled(true);
        }
        applyHandAnimations(prevHandState);
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        setLocomotionEnabled(false);
        setNEAEnabled(false);
    }

    @Override
    public void onTick() {
        boolean currentHand = handAnimations.getValue();
        if (currentHand != prevHandState) {
            prevHandState = currentHand;
            if (getEnabled()) {
                if (currentHand) {
                    setLocomotionEnabled(true);
                } else {
                    setLocomotionEnabled(false);
                }
                applyHandAnimations(currentHand);
            }
        }
    }
}
