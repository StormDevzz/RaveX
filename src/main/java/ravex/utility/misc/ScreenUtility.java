package ravex.utility.misc;

import net.minecraft.client.Minecraft;
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
    public static void setScreen(Minecraft mc, Screen screen) {
        mc.setScreen(screen);
    }

    public static Screen getCurrentScreen(Minecraft mc) {
        return mc.screen;
    }

    public static boolean isScreenOpen(Minecraft mc, Class<? extends Screen> screenClass) {
        return mc.screen != null && screenClass.isInstance(mc.screen);
    }

    public static boolean isInGame(Minecraft mc) {
        return mc.screen == null;
    }

    public static boolean isPauseScreen(Minecraft mc) {
        return mc.screen instanceof PauseScreen;
    }

    public static boolean isDeathScreen(Minecraft mc) {
        return mc.screen instanceof DeathScreen;
    }

    public static boolean isTitleScreen(Minecraft mc) {
        return mc.screen instanceof TitleScreen;
    }

    public static boolean isChatScreen(Minecraft mc) {
        return mc.screen instanceof ChatScreen;
    }

    public static boolean isDisconnectedScreen(Minecraft mc) {
        return mc.screen instanceof DisconnectedScreen;
    }

    public static boolean isConfirmScreen(Minecraft mc) {
        return mc.screen instanceof ConfirmScreen;
    }

    public static String getScreenName(Minecraft mc) {
        return mc.screen != null ? mc.screen.getClass().getSimpleName() : "InGame";
    }

    public static void setToTitle(Minecraft mc) {
        mc.setScreen(new TitleScreen());
    }

    public static void setToPause(Minecraft mc) {
        mc.setScreen(new PauseScreen(true));
    }

    public static boolean isConnectScreen(Minecraft mc) {
        return mc.screen instanceof ConnectScreen;
    }

    public static void closeScreen(Minecraft mc) {
        mc.setScreen(null);
    }
}
