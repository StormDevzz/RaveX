package ravex.utility.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class SoundUtility {

    private static final Identifier ENABLE_ID        = id("enable");
    private static final Identifier DISABLE_ID       = id("disable");
    private static final Identifier SETTINGS_OPEN_ID  = id("settings_open");
    private static final Identifier SETTINGS_CLOSE_ID = id("settings_close");
    private static final Identifier GUI_OPEN_ID       = id("gui_open");
    private static final Identifier GUI_CLOSE_ID      = id("gui_close");
    private static final Identifier FAILURE_ID        = id("failure");

    public static SoundEvent ENABLE;
    public static SoundEvent DISABLE;
    public static SoundEvent SETTINGS_OPEN;
    public static SoundEvent SETTINGS_CLOSE;
    public static SoundEvent GUI_OPEN;
    public static SoundEvent GUI_CLOSE;
    public static SoundEvent FAILURE;

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("ravex", name);
    }

    public static void register() {
        boolean wasUnfrozen = false;
        try {
            wasUnfrozen = unfreezeIfNeeded(BuiltInRegistries.SOUND_EVENT);

            ENABLE = Registry.register(BuiltInRegistries.SOUND_EVENT, ENABLE_ID, SoundEvent.createVariableRangeEvent(ENABLE_ID));
            DISABLE = Registry.register(BuiltInRegistries.SOUND_EVENT, DISABLE_ID, SoundEvent.createVariableRangeEvent(DISABLE_ID));
            SETTINGS_OPEN = Registry.register(BuiltInRegistries.SOUND_EVENT, SETTINGS_OPEN_ID, SoundEvent.createVariableRangeEvent(SETTINGS_OPEN_ID));
            SETTINGS_CLOSE = Registry.register(BuiltInRegistries.SOUND_EVENT, SETTINGS_CLOSE_ID, SoundEvent.createVariableRangeEvent(SETTINGS_CLOSE_ID));
            GUI_OPEN      = Registry.register(BuiltInRegistries.SOUND_EVENT, GUI_OPEN_ID,      SoundEvent.createVariableRangeEvent(GUI_OPEN_ID));
            GUI_CLOSE     = Registry.register(BuiltInRegistries.SOUND_EVENT, GUI_CLOSE_ID,     SoundEvent.createVariableRangeEvent(GUI_CLOSE_ID));
            FAILURE       = Registry.register(BuiltInRegistries.SOUND_EVENT, FAILURE_ID,       SoundEvent.createVariableRangeEvent(FAILURE_ID));

            ravex.RaveX.LOGGER.info("[RaveX] SoundUtility: Custom sound events successfully registered!");
        } catch (Exception e) {
            ravex.RaveX.LOGGER.error("[RaveX] SoundUtility: Failed to register custom sound events: " + e.getMessage());
        } finally {
            if (wasUnfrozen) {
                try {
                    BuiltInRegistries.SOUND_EVENT.freeze();
                } catch (Exception ignored) {}
            }
        }
    }

    /** Temporarily unfreeze a MappedRegistry via reflection if it is frozen.
     *  Returns true if the registry WAS frozen (and was unfrozen). */
    @SuppressWarnings("unchecked")
    private static boolean unfreezeIfNeeded(Registry<?> registry) {
        try {
            java.lang.reflect.Field frozenField = null;
            for (Class<?> c = registry.getClass(); c != null; c = c.getSuperclass()) {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    if (f.getName().equals("frozen") && f.getType() == boolean.class) {
                        frozenField = f;
                        break;
                    }
                }
                if (frozenField != null) break;
            }
            if (frozenField != null) {
                frozenField.setAccessible(true);
                if ((boolean) frozenField.get(registry)) {
                    frozenField.set(registry, false);
                    ravex.RaveX.LOGGER.warn("[RaveX] SoundUtility: Unfroze SOUND_EVENT registry for registration.");
                    return true;
                }
            }
        } catch (Exception e) {
            ravex.RaveX.LOGGER.warn("[RaveX] SoundUtility: Could not unfreeze registry: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Playback helpers
    // ─────────────────────────────────────────────────────────────────────────

    public static void playEnable()        { play(ENABLE,         1.0f); }
    public static void playDisable()       { play(DISABLE,        1.0f); }
    public static void playSettingsOpen()  { play(SETTINGS_OPEN,  0.8f); }
    public static void playSettingsClose() { play(SETTINGS_CLOSE, 0.8f); }
    public static void playGuiOpen()       { play(GUI_OPEN,       1.0f); }
    public static void playGuiClose()      { play(GUI_CLOSE,      1.0f); }
    public static void playFailure()       { play(FAILURE,        1.2f); }

    private static void play(SoundEvent soundEvent, float volume) {
        ravex.RaveX.LOGGER.info("[DEBUG-Sound] Attempting to play sound event: " + (soundEvent != null ? soundEvent.location() : "null") + " with base volume: " + volume);
        if (soundEvent == null) {
            ravex.RaveX.LOGGER.warn("[DEBUG-Sound] Play failed: soundEvent is null!");
            return;
        }
        try {
            // 1. Check if Sounds module is active
            if (ravex.modules.render.Sounds.INSTANCE != null && !ravex.modules.render.Sounds.INSTANCE.getEnabled()) {
                ravex.RaveX.LOGGER.info("[DEBUG-Sound] Play cancelled: Sounds module is disabled.");
                return;
            }

            // 2. Retrieve and apply volume multiplier from Sounds module
            float multiplier = 1.0f;
            if (ravex.modules.render.Sounds.INSTANCE != null) {
                multiplier = ravex.modules.render.Sounds.INSTANCE.volume.getValue().floatValue();
            }
            float finalVolume = volume * multiplier;
            ravex.RaveX.LOGGER.info("[DEBUG-Sound] Applied volume multiplier: " + multiplier + " -> final volume: " + finalVolume);
            if (finalVolume <= 0.0f) {
                ravex.RaveX.LOGGER.info("[DEBUG-Sound] Play cancelled: final volume is <= 0.");
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                ravex.RaveX.LOGGER.warn("[DEBUG-Sound] Play failed: Minecraft instance is null!");
                return;
            }
            if (mc.getSoundManager() == null) {
                ravex.RaveX.LOGGER.warn("[DEBUG-Sound] Play failed: SoundManager is null!");
                return;
            }

            SimpleSoundInstance sound = SimpleSoundInstance.forUI(soundEvent, 1.0f, finalVolume);
            ravex.RaveX.LOGGER.info("[DEBUG-Sound] Created SimpleSoundInstance for sound event: " + soundEvent.location());
            mc.getSoundManager().play(sound);
            ravex.RaveX.LOGGER.info("[DEBUG-Sound] Sound successfully dispatched to Minecraft SoundManager.");
        } catch (Exception e) {
            ravex.RaveX.LOGGER.warn("[RaveX] Sound play error: " + e.getMessage());
        }
    }
}
