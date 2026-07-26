package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import com.google.gson.Gson;

import java.lang.reflect.Field;
import java.util.List;
@ModuleInfo(name = "Swing", category = "net.minecraft.world.entity.player.Player")
public class Swing implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"1.8", "1.12.2", "Custom"})
    public String mode = "1.8";
    @Parameter(name = "Duration", min = 1, max = 20, step = 1)
    public double duration = 6;
    @Parameter(name = "SwingPath", modes = {"Normal", "Smooth", "Bounce", "Reverse"})
    public String swingPath = "Normal";
    @Parameter(name = "SwingCurve", min = 0.1, max = 5.0, step = 0.1)
    public double swingCurve = 1.0;
    @Parameter(name = "ProgressCap", min = 0.0, max = 1.0, step = 0.05)
    public double progressCap = 1.0;
    @Parameter(name = "ProgressFloor", min = 0.0, max = 1.0, step = 0.05)
    public double progressFloor = 0.0;
    @Parameter(name = "NoEquip")
    public boolean noEquip = false;
    private static final String LOCO_MAIN = "com.trainguy9512.locomotion.LocomotionMain";
    private Object locoConfig;
    private Object locoConfigData;
    private String savedLocoConfigJson;
    private boolean locomotionAvailable;
    private Swing() {
        
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
    public void onEnable() {
        
        setLocomotionEnabled(true);
    }
    public void onDisable() {
        
        setLocomotionEnabled(false);
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Swing").getEnabled();
    }
    public static Swing itz() {
        return ravex.manager.ModuleManager.delegate(Swing.class);
    }


}