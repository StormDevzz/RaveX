package ravex.utility.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import ravex.manager.ModuleManager;

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
        } catch (Exception e) {
            ravex.RaveX.LOGGER.error("[RaveX] SoundUtility: Failed to register sound events: " + e.getMessage());
        } finally {
            if (wasUnfrozen) {
                try { BuiltInRegistries.SOUND_EVENT.freeze(); } catch (Exception ignored) {}
            }
        }
    }


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
                    return true;
                }
            }
        } catch (Exception e) {
            ravex.RaveX.LOGGER.warn("[RaveX] SoundUtility: Could not unfreeze registry: " + e.getMessage());
        }
        return false;
    }





    public static void playEnable()        { play(ENABLE,         1.0f); }
    public static void playDisable()       { play(DISABLE,        1.0f); }
    public static void playSettingsOpen()  { play(SETTINGS_OPEN,  0.8f); }
    public static void playSettingsClose() { play(SETTINGS_CLOSE, 0.8f); }
    public static void playGuiOpen()       { play(GUI_OPEN,       1.0f); }
    public static void playGuiClose()      { play(GUI_CLOSE,      1.0f); }
    public static void playFailure()       { play(FAILURE,        1.0f); }


    private static void play(SoundEvent soundEvent, float volume) {
        if (soundEvent == null) {
            ravex.RaveX.LOGGER.warn("[Sound] Skipped: SoundEvent is null (not registered yet?)");
            return;
        }

<<<<<<< HEAD
        ravex.modules.render.Sounds sounds = ModuleManager.get(ravex.modules.render.Sounds.class);
=======
        ravex.modules.render.Sounds sounds = ravex.modules.render.Sounds.INSTANCE;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (sounds != null && !sounds.getEnabled()) {
            return;
        }


        float multiplier = (sounds != null) ? sounds.volume.getValue().floatValue() : 1.0f;
        float finalVolume = volume * multiplier;
        if (finalVolume <= 0.0f) return;


        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;


        try {
            if (mc.options != null) {
                var masterOpt = mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER);
                if (masterOpt != null && masterOpt.get() <= 0.0) {
                    masterOpt.set(1.0);
                    ravex.RaveX.LOGGER.info("[Sound] Master Volume was 0.0, auto-set to 1.0 to enable client sounds.");
                    mc.options.save();
                }
            }
        } catch (Exception e) {
            ravex.RaveX.LOGGER.warn("[Sound] Failed to check/set Master Volume: " + e.getMessage());
        }

        final float fv = finalVolume;
        final SoundEvent se = soundEvent;

        if (mc.isSameThread()) {

            dispatchSound(mc, se, fv);
        } else {

            mc.execute(() -> dispatchSound(mc, se, fv));
        }
    }

    private static void dispatchSound(Minecraft mc, SoundEvent soundEvent, float volume) {
        try {
            if (mc.getSoundManager() == null) return;
            if (mc.player != null) {
                mc.player.playSound(soundEvent, volume, 1.0f);
            } else {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, 1.0f, volume));
            }
        } catch (Exception e) {
            ravex.RaveX.LOGGER.warn("[Sound] Playback error: " + e.getMessage());
        }
    }
}
