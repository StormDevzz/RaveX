package ravex.utility.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class SoundUtility {

    private static final Identifier ENABLE_ID         = id("enable");
    private static final Identifier DISABLE_ID        = id("disable");
    private static final Identifier SETTINGS_OPEN_ID  = id("settings_open");
    private static final Identifier SETTINGS_CLOSE_ID = id("settings_close");

    public static SoundEvent ENABLE;
    public static SoundEvent DISABLE;
    public static SoundEvent SETTINGS_OPEN;
    public static SoundEvent SETTINGS_CLOSE;

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("ravex", name);
    }

    /**
     * Register custom sound events.
     * Called from RaveXCommon.onInitialize() — which runs before Minecraft's
     * Bootstrap freezes BuiltInRegistries.  If somehow the registry is already
     * frozen (e.g. in a dev-run ordering quirk) we unfreeze it temporarily
     * via reflection so the sounds always get registered.
     */
    public static void register() {
        boolean wasUnfrozen = false;
        try {
            wasUnfrozen = unfreezeIfNeeded(BuiltInRegistries.SOUND_EVENT);

            ENABLE        = Registry.register(BuiltInRegistries.SOUND_EVENT, ENABLE_ID,        SoundEvent.createVariableRangeEvent(ENABLE_ID));
            DISABLE       = Registry.register(BuiltInRegistries.SOUND_EVENT, DISABLE_ID,       SoundEvent.createVariableRangeEvent(DISABLE_ID));
            SETTINGS_OPEN = Registry.register(BuiltInRegistries.SOUND_EVENT, SETTINGS_OPEN_ID, SoundEvent.createVariableRangeEvent(SETTINGS_OPEN_ID));
            SETTINGS_CLOSE= Registry.register(BuiltInRegistries.SOUND_EVENT, SETTINGS_CLOSE_ID,SoundEvent.createVariableRangeEvent(SETTINGS_CLOSE_ID));

            ravex.RaveX.LOGGER.info("[RaveX] SoundUtility: Sound events registered successfully.");
        } catch (Exception e) {
            ravex.RaveX.LOGGER.error("[RaveX] SoundUtility: Failed to register sound events: " + e.getMessage());
        } finally {
            // IMPORTANT: always re-freeze the registry after we're done —
            // otherwise Minecraft's tag system crashes with 'Tags already present before freezing'.
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
                    return true;  // was frozen
                }
            }
        } catch (Exception e) {
            ravex.RaveX.LOGGER.warn("[RaveX] SoundUtility: Could not unfreeze registry: " + e.getMessage());
        }
        return false;  // was not frozen
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Playback helpers
    // ─────────────────────────────────────────────────────────────────────────

    public static void playEnable()        { play(ENABLE_ID,         1.0f); }
    public static void playDisable()       { play(DISABLE_ID,        1.0f); }
    public static void playSettingsOpen()  { play(SETTINGS_OPEN_ID,  0.8f); }
    public static void playSettingsClose() { play(SETTINGS_CLOSE_ID, 0.8f); }

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
            ravex.RaveX.LOGGER.warn("[RaveX] Sound play error: " + e.getMessage());
        }
    }
}
