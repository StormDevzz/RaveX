package ravex.utility.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class SoundUtility {

    private static final Identifier ENABLE_ID         = id("enable");
    private static final Identifier DISABLE_ID        = id("disable");
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

    /**
     * Registers all custom RaveX sound events into BuiltInRegistries.
     * Called from RaveXCommon.onInitialize() BEFORE registries are frozen.
     */
    public static void register() {
        boolean wasUnfrozen = false;
        try {
            wasUnfrozen = unfreezeIfNeeded(BuiltInRegistries.SOUND_EVENT);

            ENABLE        = Registry.register(BuiltInRegistries.SOUND_EVENT, ENABLE_ID,        SoundEvent.createVariableRangeEvent(ENABLE_ID));
            DISABLE       = Registry.register(BuiltInRegistries.SOUND_EVENT, DISABLE_ID,       SoundEvent.createVariableRangeEvent(DISABLE_ID));
            SETTINGS_OPEN = Registry.register(BuiltInRegistries.SOUND_EVENT, SETTINGS_OPEN_ID, SoundEvent.createVariableRangeEvent(SETTINGS_OPEN_ID));
            SETTINGS_CLOSE= Registry.register(BuiltInRegistries.SOUND_EVENT, SETTINGS_CLOSE_ID,SoundEvent.createVariableRangeEvent(SETTINGS_CLOSE_ID));
            GUI_OPEN      = Registry.register(BuiltInRegistries.SOUND_EVENT, GUI_OPEN_ID,      SoundEvent.createVariableRangeEvent(GUI_OPEN_ID));
            GUI_CLOSE     = Registry.register(BuiltInRegistries.SOUND_EVENT, GUI_CLOSE_ID,     SoundEvent.createVariableRangeEvent(GUI_CLOSE_ID));
            FAILURE       = Registry.register(BuiltInRegistries.SOUND_EVENT, FAILURE_ID,       SoundEvent.createVariableRangeEvent(FAILURE_ID));

            ravex.RaveX.LOGGER.info("[RaveX] SoundUtility: All 7 sound events registered successfully.");
        } catch (Exception e) {
            ravex.RaveX.LOGGER.error("[RaveX] SoundUtility: Failed to register sound events: " + e.getMessage());
        } finally {
            if (wasUnfrozen) {
                try { BuiltInRegistries.SOUND_EVENT.freeze(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Temporarily unfreeze a MappedRegistry via reflection if it is frozen.
     * Returns true if the registry WAS frozen (and was unfrozen by us).
     */
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
                    ravex.RaveX.LOGGER.info("[RaveX] SoundUtility: Unfroze SOUND_EVENT registry for registration.");
                    return true;
                }
            }
        } catch (Exception e) {
            ravex.RaveX.LOGGER.warn("[RaveX] SoundUtility: Could not unfreeze registry: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public playback helpers
    // ─────────────────────────────────────────────────────────────────────────

    public static void playEnable()        { play(ENABLE,         1.0f); }
    public static void playDisable()       { play(DISABLE,        1.0f); }
    public static void playSettingsOpen()  { play(SETTINGS_OPEN,  0.8f); }
    public static void playSettingsClose() { play(SETTINGS_CLOSE, 0.8f); }
    public static void playGuiOpen()       { play(GUI_OPEN,       1.0f); }
    public static void playGuiClose()      { play(GUI_CLOSE,      1.0f); }
    public static void playFailure()       { play(FAILURE,        1.0f); }

    /**
     * Thread-safe sound playback.
     * Always dispatches on the Minecraft main thread to avoid SoundEngine crashes.
     */
    private static void play(SoundEvent soundEvent, float volume) {
        if (soundEvent == null) {
            ravex.RaveX.LOGGER.warn("[Sound] Skipped: SoundEvent is null (not registered yet?)");
            return;
        }

        // Check Sounds module toggle — but only if INSTANCE is already initialized
        // to avoid blocking sounds during module bootstrap.
        ravex.modules.render.Sounds sounds = ravex.modules.render.Sounds.INSTANCE;
        if (sounds != null && !sounds.getEnabled()) {
            return;
        }

        // Apply volume multiplier
        float multiplier = (sounds != null) ? sounds.volume.getValue().floatValue() : 1.0f;
        float finalVolume = volume * multiplier;
        if (finalVolume <= 0.0f) return;

        // Always dispatch on render thread — SoundManager is NOT thread-safe
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        final float fv = finalVolume;
        final SoundEvent se = soundEvent;

        if (mc.isSameThread()) {
            // Already on render thread — play immediately
            dispatchSound(mc, se, fv);
        } else {
            // Schedule on main thread
            mc.execute(() -> dispatchSound(mc, se, fv));
        }
    }

    private static void dispatchSound(Minecraft mc, SoundEvent soundEvent, float volume) {
        try {
            if (mc.getSoundManager() == null) return;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, 1.0f, volume));
        } catch (Exception e) {
            ravex.RaveX.LOGGER.warn("[Sound] Playback error: " + e.getMessage());
        }
    }
}
