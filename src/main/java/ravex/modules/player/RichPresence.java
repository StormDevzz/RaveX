package ravex.modules.player;

/**
 * JNI bridge to libravex_dc.so.
 *
 * The C++ natives were compiled against the old package layout, so the exported
 * symbols live under ravex.modules.player:
 *   Java_ravex_modules_player_RichPresence_nativeInitialize
 *   Java_ravex_modules_player_RichPresence_nativeShutdown
 *   Java_ravex_modules_player_RichPresence_nativeUpdatePresence
 *   Java_ravex_modules_player_RichPresence_nativeIsConnected
 *
 * The active module class (ravex.modules.client.RichPresence) delegates here.
 * Do not move or rename this class without rebuilding libravex_dc.so.
 */
public final class RichPresence {
    private RichPresence() {}

    public static native void nativeInitialize(String clientId);

    public static native void nativeShutdown();

    public static native void nativeUpdatePresence(String state, String details, long startTimestamp,
                                                   boolean showOS, boolean showButton, String largeImageKey);

    public static native boolean nativeIsConnected();
}
