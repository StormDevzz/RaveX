package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import com.google.gson.Gson;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.lang.reflect.Field;
import java.util.List;
@ModuleInfo(name = "Swing", category = "net.minecraft.world.entity.player.Player")
public class Swing extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "1.8", List.of("1.8", "1.12.2", "Custom"));
    public final NumberParameter duration = new NumberParameter("Duration", 6, 1, 20, 1);
    public final ModeParameter swingPath = new ModeParameter("SwingPath", "Normal", List.of("Normal", "Smooth", "Bounce", "Reverse"));
    public final NumberParameter swingCurve = new NumberParameter("SwingCurve", 1.0, 0.1, 5.0, 0.1);
    public final NumberParameter progressCap = new NumberParameter("ProgressCap", 1.0, 0.0, 1.0, 0.05);
    public final NumberParameter progressFloor = new NumberParameter("ProgressFloor", 0.0, 0.0, 1.0, 0.05);
    public final BooleanParameter noEquip = new BooleanParameter("NoEquip", false);
    private static final String LOCO_MAIN = "com.trainguy9512.locomotion.LocomotionMain";
    private Object locoConfig;
    private Object locoConfigData;
    private String savedLocoConfigJson;
    private boolean locomotionAvailable;
    private Swing() {
        
        duration.setVisible(() -> "Custom".equals(mode.getValue()));
        swingPath.setVisible(() -> "Custom".equals(mode.getValue()));
        swingCurve.setVisible(() -> "Custom".equals(mode.getValue()));
        progressCap.setVisible(() -> "Custom".equals(mode.getValue()));
        progressFloor.setVisible(() -> "Custom".equals(mode.getValue()));
        noEquip.setVisible(() -> "Custom".equals(mode.getValue()));
        initLocomotion();
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
    protected void onEnable() {
        
        setLocomotionEnabled(true);
    }
    protected void onDisable() {
        
        setLocomotionEnabled(false);
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Swing").getEnabled();
    }
    public static Swing itz() {
        return ravex.manager.ModuleManager.delegate(Swing.class);
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