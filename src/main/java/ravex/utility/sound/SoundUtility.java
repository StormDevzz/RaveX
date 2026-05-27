package ravex.utility.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class SoundUtility {

    private static final Identifier ENABLE        = id("enable");
    private static final Identifier DISABLE       = id("disable");
    private static final Identifier SETTINGS_OPEN  = id("settings_open");
    private static final Identifier SETTINGS_CLOSE = id("settings_close");

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("ravex", name);
    }

    public static void register() {
        ravex.RaveX.LOGGER.info("[RaveX] SoundUtility ready");
    }

    public static void playEnable()       { play(ENABLE,        1.0f); }
    public static void playDisable()      { play(DISABLE,       1.0f); }
    public static void playSettingsOpen() { play(SETTINGS_OPEN,  0.8f); }
    public static void playSettingsClose(){ play(SETTINGS_CLOSE, 0.8f); }

    private static void play(Identifier loc, float volume) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getSoundManager() == null) return;

            SimpleSoundInstance sound = new SimpleSoundInstance(
                loc,
                SoundSource.MASTER,
                volume,
                1.0f,
                RandomSource.create(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0, 0.0, 0.0,
                true
            );
            mc.getSoundManager().play(sound);
        } catch (Exception e) {
            ravex.RaveX.LOGGER.warn("[RaveX] Sound error: " + e.getMessage());
        }
    }
}
