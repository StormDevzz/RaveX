package ravex.utility.misc;

import net.minecraft.client.Minecraft;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;

public class ScreenUtility {
    public static void setScreen(MinecraftWrapper mc, Screen screen) {
        var _mc = mc.getRaw();
        _mc.setScreen(screen);
    }

    public static Screen getCurrentScreen(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        return _mc.screen;
    }

    public static boolean isScreenOpen(MinecraftWrapper mc, Class<? extends Screen> screenClass) {
        var _mc = mc.getRaw();
        return _mc.screen != null && screenClass.isInstance(_mc.screen);
    }

    public static boolean isInGame(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        return _mc.screen == null;
    }

    public static boolean isPauseScreen(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        return _mc.screen instanceof PauseScreen;
    }

    public static boolean isDeathScreen(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        return _mc.screen instanceof DeathScreen;
    }

    public static boolean isTitleScreen(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        return _mc.screen instanceof TitleScreen;
    }

    public static boolean isChatScreen(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        return _mc.screen instanceof ChatScreen;
    }

    public static boolean isDisconnectedScreen(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        return _mc.screen instanceof DisconnectedScreen;
    }

    public static boolean isConfirmScreen(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        return _mc.screen instanceof ConfirmScreen;
    }

    public static String getScreenName(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        return _mc.screen != null ? _mc.screen.getClass().getSimpleName() : "InGame";
    }

    public static void setToTitle(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        _mc.setScreen(new TitleScreen());
    }

    public static void setToPause(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        _mc.setScreen(new PauseScreen(true));
    }

    public static boolean isConnectScreen(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        return _mc.screen instanceof ConnectScreen;
    }

    public static void closeScreen(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        _mc.setScreen(null);
    }
}
