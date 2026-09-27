package ravex.modules.hud;

public class NowPlayingHud {
    public static native String nativeGetNowPlaying();
    public static native byte[] nativeDownloadArt(String url);
    public static native boolean nativeIsAvailable();
}
