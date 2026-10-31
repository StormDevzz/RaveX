package ravex.modules.world;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.ScreenUtility;
import ravex.utility.misc.SignUtility;

@Module(name = "AutoSign", category = "World")
public class AutoSign {
    @Parameter(name = "Mode", modes = {"Simple", "Advanced"})
    public String mode = "Simple";
    @Parameter(name = "Line1")
    public String line1 = "";
    @Parameter(name = "Line2")
    public String line2 = "";
    @Parameter(name = "Line3")
    public String line3 = "";
    @Parameter(name = "Line4")
    public String line4 = "";
    @Parameter(name = "AutoPlace", visible = "mode=Advanced")
    public boolean autoPlace = true;
    @Parameter(name = "PlaceMode", modes = {"Crosshair", "Range"}, visible = "mode=Advanced&&autoPlace")
    public String placeMode = "Crosshair";
    @Parameter(name = "PlaceDelay", min = 50, max = 2000, step = 50, visible = "mode=Advanced&&autoPlace")
    public double placeDelay = 500;
    @Parameter(name = "PlaceRange", min = 1, max = 6, step = 0.5, visible = "mode=Advanced&&autoPlace")
    public double placeRange = 4.0;
    @Parameter(name = "CloseDelay", min = 0, max = 1000, step = 50, visible = "mode=Advanced&&autoPlace")
    public double closeDelay = 0;
    @Parameter(name = "SwapBack", visible = "mode=Advanced&&autoPlace")
    public boolean swapBack = true;

    private long lastPlaceTime;
    private long pendingCloseAt;

    public void onEnable() {
        lastPlaceTime = 0;
        pendingCloseAt = 0;
    }

    public void onDisable() {
        pendingCloseAt = 0;
    }

    public boolean isAdvanced() {
        return "Advanced".equals(mode);
    }

    public void closeSignScreen() {
        var mc = MinecraftWrapper.getWrapper();
        if (closeDelay <= 0) {
            mc.execute(() -> ScreenUtility.closeScreen(mc));
            return;
        }
        pendingCloseAt = System.currentTimeMillis() + (long) closeDelay;
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        long now = System.currentTimeMillis();
        if (pendingCloseAt > 0 && now >= pendingCloseAt) {
            pendingCloseAt = 0;
            if (SignUtility.isSignScreenOpen()) ScreenUtility.closeScreen(mc);
        }
        if (!isAdvanced() || !autoPlace) return;
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getCurrentScreen() != null) return;
        if (now - lastPlaceTime < placeDelay) return;
        if (SignUtility.placeSign(placeMode, placeRange, swapBack)) lastPlaceTime = now;
    }
}
