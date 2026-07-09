package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.player.LocalPlayer;
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import java.util.List;
public class SoundBlock extends Module {
    public final BooleanParameter blockAmbient   = new BooleanParameter("Ambient", false);
    public final BooleanParameter blockBlocks    = new BooleanParameter("Blocks", false);
    public final BooleanParameter blockWeather   = new BooleanParameter("Weather", false);
    public final BooleanParameter blockHostile   = new BooleanParameter("Hostile", false);
    public final BooleanParameter blockNeutral   = new BooleanParameter("Neutral", false);
    public final BooleanParameter blockPlayers   = new BooleanParameter("Players", false);
    public final BooleanParameter blockVoice     = new BooleanParameter("Voice", false);
    public final BooleanParameter blockMusic     = new BooleanParameter("Music", false);
    public final BooleanParameter blockRecords   = new BooleanParameter("Records", false);

    public boolean shouldBlock(SoundInstance sound) {
        if (!getEnabled()) return false;
        var source = sound.getSource();
        if (source == null) return false;
        return switch (source) {
            case AMBIENT -> blockAmbient.getValue();
            case BLOCKS -> blockBlocks.getValue();
            case WEATHER -> blockWeather.getValue();
            case HOSTILE -> blockHostile.getValue();
            case NEUTRAL -> blockNeutral.getValue();
            case PLAYERS -> blockPlayers.getValue();
            case VOICE -> blockVoice.getValue();
            case MUSIC -> blockMusic.getValue();
            case RECORDS -> blockRecords.getValue();
            default -> false;
        };
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(SoundBlock.class);
    }

    public static SoundBlock itz() {
        return ModuleManager.get(SoundBlock.class);
    }
}
