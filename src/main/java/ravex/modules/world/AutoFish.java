package ravex.modules.world;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.FishUtility;

@Module(name = "AutoFish", category = "World")
public class AutoFish {
    @Parameter(name = "AutoCast")
    public boolean autoCast = true;
    @Parameter(name = "CastDelay", min = 200, max = 3000, step = 100)
    public double castDelay = 800;
    @Parameter(name = "ReelDelay", min = 0, max = 1500, step = 50)
    public double reelDelay = 200;
    @Parameter(name = "Timeout", min = 5, max = 90, step = 5)
    public double timeout = 30;
    @Parameter(name = "AntiBreak")
    public boolean antiBreak = true;
    @Parameter(name = "SilentSwap")
    public boolean silent = true;

    private long biteTime = 0;
    private long nextCastTime = 0;

    public void onEnable() {
        biteTime = 0;
        nextCastTime = System.currentTimeMillis();
        FishUtility.reset();
    }

    public void onDisable() {
        biteTime = 0;
        FishUtility.reset();
    }

    public void onTick() {
        if (MinecraftWrapper.getWrapper().getPlayer() == null) return;
        long now = System.currentTimeMillis();

        if (FishUtility.hasHook()) {
            if (FishUtility.isBiting() && biteTime == 0) biteTime = now;
            boolean reelNow = biteTime != 0 && now - biteTime >= reelDelay;
            boolean stuck = biteTime == 0 && FishUtility.hookAgeMs() >= (long) (timeout * 1000L);
            if (reelNow || stuck) {
                FishUtility.useRod(antiBreak, silent);
                biteTime = 0;
                FishUtility.reset();
                nextCastTime = now + (long) castDelay;
            }
            return;
        }

        FishUtility.reset();
        biteTime = 0;
        if (!autoCast || now < nextCastTime) return;
        if (FishUtility.useRod(antiBreak, silent)) nextCastTime = now + (long) castDelay;
    }
}
