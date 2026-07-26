package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import java.util.List;




@ModuleInfo(name = "SoundBlock", category = "Misc")
public class SoundBlock implements ModuleAccess {
    @Parameter(name = "Ambient")
    public boolean blockAmbient = false;
    @Parameter(name = "net.minecraft.world.level.block.Blocks")
    public boolean blockBlocks = false;
    @Parameter(name = "Weather")
    public boolean blockWeather = false;
    @Parameter(name = "Hostile")
    public boolean blockHostile = false;
    @Parameter(name = "Neutral")
    public boolean blockNeutral = false;
    @Parameter(name = "Players")
    public boolean blockPlayers = false;
    @Parameter(name = "Voice")
    public boolean blockVoice = false;
    @Parameter(name = "Music")
    public boolean blockMusic = false;
    @Parameter(name = "Records")
    public boolean blockRecords = false;

    public boolean shouldBlock(SoundInstance sound) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("SoundBlock").getEnabled()) return false;
        var source = sound.getSource();
        if (source == null) return false;
        return switch (source) {
            case AMBIENT -> blockAmbient;
            case BLOCKS -> blockBlocks;
            case WEATHER -> blockWeather;
            case HOSTILE -> blockHostile;
            case NEUTRAL -> blockNeutral;
            case PLAYERS -> blockPlayers;
            case VOICE -> blockVoice;
            case MUSIC -> blockMusic;
            case RECORDS -> blockRecords;
            default -> false;
        };
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("SoundBlock").getEnabled();
    }

    public static SoundBlock itz() {
        return ravex.manager.ModuleManager.delegate(SoundBlock.class);
    }


}