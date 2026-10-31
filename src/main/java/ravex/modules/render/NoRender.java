package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.modules.Modules;
import ravex.utility.nativelib.NativeLoader;
@Module(name = "NoRender", category = "Render")
public class NoRender {
    @Parameter(name = "Particles", group = true)
    public boolean groupParticles = false;
    @Parameter(name = "BlockParticles", visible = "groupParticles")
    public boolean blockParticles = true;
    @Parameter(name = "Explosions", visible = "groupParticles")
    public boolean explosions = true;
    @Parameter(name = "Sprint", visible = "groupParticles")
    public boolean sprint = false;

    @Parameter(name = "Misc", group = true)
    public boolean groupMisc = false;
    @Parameter(name = "Weather", visible = "groupMisc")
    public boolean weather = true;
    @Parameter(name = "Portal", visible = "groupMisc")
    public boolean portal = true;
    @Parameter(name = "Fog", visible = "groupMisc")
    public boolean fog = true;
    @Parameter(name = "Fire", visible = "groupMisc")
    public boolean fire = true;

    @Parameter(name = "Blocks", group = true)
    public boolean groupBlocks = false;
    @Parameter(name = "Vines", visible = "groupBlocks")
    public boolean vines = false;
    @Parameter(name = "Cobwebs", visible = "groupBlocks")
    public boolean cobwebs = false;
    @Parameter(name = "Flowers", visible = "groupBlocks")
    public boolean flowers = false;

    @Parameter(name = "Entity", group = true)
    public boolean groupEntity = false;
    @Parameter(name = "ArmorStands", visible = "groupEntity")
    public boolean armorStands = false;
    @Parameter(name = "Frames", visible = "groupEntity")
    public boolean frames = false;

    @Parameter(name = "Items", group = true)
    public boolean groupItems = false;
    @Parameter(name = "Armor", visible = "groupItems")
    public boolean armor = false;
    @Parameter(name = "ItemsHud", visible = "groupItems")
    public boolean items = false;
    @Parameter(name = "Signs", visible = "groupItems")
    public boolean signs = false;
    @Parameter(name = "Tripwire", visible = "groupItems")
    public boolean tripwire = false;

    @Parameter(name = "HUD", group = true)
    public boolean groupHud = false;
    @Parameter(name = "BossBar", visible = "groupHud")
    public boolean bossBar = false;
    @Parameter(name = "Scoreboard", visible = "groupHud")
    public boolean scoreboard = false;
    @Parameter(name = "Crosshair", visible = "groupHud")
    public boolean crosshair = false;

    @Parameter(name = "Overlay", group = true)
    public boolean groupOverlay = false;
    @Parameter(name = "EffectIcons", visible = "groupOverlay")
    public boolean effectIcons = false;
    @Parameter(name = "Nausea", visible = "groupOverlay")
    public boolean nausea = false;
    @Parameter(name = "Vignette", visible = "groupOverlay")
    public boolean vignette = false;
    @Parameter(name = "InvBackground", visible = "groupOverlay")
    public boolean inventoryBackground = false;
    @Parameter(name = "DamageShake", visible = "groupOverlay")
    public boolean damageShake = false;

    @Parameter(name = "Sound", group = true)
    public boolean groupSound = false;
    @Parameter(name = "AmbientSound", visible = "groupSound")
    public boolean soundAmbient = false;
    @Parameter(name = "BlockSounds", visible = "groupSound")
    public boolean soundBlocks = false;
    @Parameter(name = "WeatherSound", visible = "groupSound")
    public boolean soundWeather = false;
    @Parameter(name = "HostileSound", visible = "groupSound")
    public boolean soundHostile = false;
    @Parameter(name = "NeutralSound", visible = "groupSound")
    public boolean soundNeutral = false;
    @Parameter(name = "PlayerSounds", visible = "groupSound")
    public boolean soundPlayers = false;
    @Parameter(name = "VoiceSound", visible = "groupSound")
    public boolean soundVoice = false;
    @Parameter(name = "MusicSound", visible = "groupSound")
    public boolean soundMusic = false;
    @Parameter(name = "RecordSound", visible = "groupSound")
    public boolean soundRecords = false;

    public static boolean shouldBlockSound(net.minecraft.client.resources.sounds.SoundInstance sound) {
        if (!Modules.enabled(NoRender.class)) return false;
        var source = sound.getSource();
        if (source == null) return false;
        NoRender nr = Modules.get(NoRender.class);
        if (nr == null) return false;
        return switch (source) {
            case AMBIENT -> nr.soundAmbient;
            case BLOCKS -> nr.soundBlocks;
            case WEATHER -> nr.soundWeather;
            case HOSTILE -> nr.soundHostile;
            case NEUTRAL -> nr.soundNeutral;
            case PLAYERS -> nr.soundPlayers;
            case VOICE -> nr.soundVoice;
            case MUSIC -> nr.soundMusic;
            case RECORDS -> nr.soundRecords;
            default -> false;
        };
    }
    private static boolean nativeReady() {
        return NativeLoader.isNativeAvailable();
    }

    public static native boolean nativeShouldCull(double x, double y, double z, double camX, double camY, double camZ, double maxDist);
    public static native int nativeOptimizeBudget(int activeCount, int currentFps, int minFps);
    public static native float[] nativeOptimizeFog(float envStart, float envEnd, float rdStart, float rdEnd, float skyEnd, float cloudEnd);
    public static boolean shouldCull(double x, double y, double z, double camX, double camY, double camZ, double maxDist) {
        if (nativeReady()) {
            try {
                return nativeShouldCull(x, y, z, camX, camY, camZ, maxDist);
            } catch (UnsatisfiedLinkError e) {
            }
        }
        double dx = x - camX;
        double dy = y - camY;
        double dz = z - camZ;
        return (dx * dx + dy * dy + dz * dz) > (maxDist * maxDist);
    }
    public static int optimizeBudget(int activeCount, int currentFps, int minFps) {
        if (nativeReady()) {
            try {
                return nativeOptimizeBudget(activeCount, currentFps, minFps);
            } catch (UnsatisfiedLinkError e) {
            }
        }
        if (minFps <= 0) return activeCount;
        if (currentFps < minFps) {
            double ratio = (double) currentFps / (double) minFps;
            if (ratio < 0.1) ratio = 0.1;
            return (int) (activeCount * ratio);
        }
        return activeCount;
    }
    public static float[] optimizeFog(float envStart, float envEnd, float rdStart, float rdEnd, float skyEnd, float cloudEnd) {
        if (nativeReady()) {
            try {
                return nativeOptimizeFog(envStart, envEnd, rdStart, rdEnd, skyEnd, cloudEnd);
            } catch (UnsatisfiedLinkError e) {
            }
        }
        return new float[] { 999999.0f, 999999.0f, 999999.0f, 999999.0f, 999999.0f, 999999.0f };
    }





}