package ravex.utility.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class SoundUtility {

    private static final Identifier ENABLE_ID        = id("enable");
    private static final Identifier DISABLE_ID       = id("disable");
    private static final Identifier SETTINGS_OPEN_ID  = id("settings_open");
    private static final Identifier SETTINGS_CLOSE_ID = id("settings_close");

    public static SoundEvent ENABLE;
    public static SoundEvent DISABLE;
    public static SoundEvent SETTINGS_OPEN;
    public static SoundEvent SETTINGS_CLOSE;

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("ravex", name);
    }

    public static void register() {
        try {
            ENABLE = Registry.register(BuiltInRegistries.SOUND_EVENT, ENABLE_ID, SoundEvent.createVariableRangeEvent(ENABLE_ID));
            DISABLE = Registry.register(BuiltInRegistries.SOUND_EVENT, DISABLE_ID, SoundEvent.createVariableRangeEvent(DISABLE_ID));
            SETTINGS_OPEN = Registry.register(BuiltInRegistries.SOUND_EVENT, SETTINGS_OPEN_ID, SoundEvent.createVariableRangeEvent(SETTINGS_OPEN_ID));
            SETTINGS_CLOSE = Registry.register(BuiltInRegistries.SOUND_EVENT, SETTINGS_CLOSE_ID, SoundEvent.createVariableRangeEvent(SETTINGS_CLOSE_ID));
            
            ravex.RaveX.LOGGER.info("[RaveX] SoundUtility: Custom sound events successfully registered!");
        } catch (Exception e) {
            ravex.RaveX.LOGGER.error("[RaveX] SoundUtility: Failed to register custom sound events: " + e.getMessage());
        }
    }

    public static void playEnable()       { play(ENABLE_ID,        1.0f); }
    public static void playDisable()      { play(DISABLE_ID,       1.0f); }
    public static void playSettingsOpen() { play(SETTINGS_OPEN_ID,  0.8f); }
    public static void playSettingsClose(){ play(SETTINGS_CLOSE_ID, 0.8f); }

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
