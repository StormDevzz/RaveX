package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.nativelib.NativeLoader;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "AntiQuit", category = "Misc")
public class AntiQuit {
    // guard leaving the server and/or closing the game
    @Parameter(name = "Server")
    public boolean server = true;
    @Parameter(name = "Game")
    public boolean game = true;
    // confirm asks first, Strict never quits
    @Parameter(name = "Mode", modes = {"Confirm", "Strict"})
    public String mode = "Confirm";

    private static boolean bypassDisconnect;
    private static boolean confirmOpen;

    static {
        NativeLoader.load();
        NativeLoader.loadLibrary("ravex_antiquit");
    }
    public void onEnable() {
        try {
            // strict guard also blocks paths outside the window callback
            nativeBlockQuit(game && "Strict".equals(mode));
        } catch (UnsatisfiedLinkError ignored) {}
        var mc = MinecraftWrapper.getWrapper();
        Window window = mc.getWindow();
        if (window == null) return;
        window.setWindowCloseCallback(() -> {
            long handle = window.handle();
            GLFW.glfwSetWindowShouldClose(handle, false);
            if (!game) {
                window.setWindowCloseCallback(null);
                GLFW.glfwSetWindowShouldClose(handle, true);
                return;
            }
            if ("Strict".equals(mode)) return;
            mc.execute(() -> mc.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        window.setWindowCloseCallback(null);
                        GLFW.glfwSetWindowShouldClose(handle, true);
                    } else {
                        mc.setScreen(new PauseScreen(true));
                    }
                },
                Component.literal("Are you sure bro?"),
                Component.literal("Do you really want to quit the game?"),
                Component.literal("Yep"),
                Component.literal("Nah")
            )));
        });
    }
    public void onDisable() {
        try {
            nativeBlockQuit(false);
        } catch (UnsatisfiedLinkError ignored) {}
        var mc = MinecraftWrapper.getWrapper();
        Window window = mc.getWindow();
        if (window != null) {
            window.setWindowCloseCallback(null);
        }
    }

    // true when this disconnect attempt must be swallowed.
    public static boolean handleDisconnect(Component reason) {
        AntiQuit $ = Modules.get(AntiQuit.class);
        if ($ == null || !Modules.enabled(AntiQuit.class) || !$.server || bypassDisconnect) return false;
        if ("Strict".equals($.mode)) return true;
        if (confirmOpen) return true;
        var mc = MinecraftWrapper.getWrapper();
        confirmOpen = true;
        mc.execute(() -> mc.setScreen(new ConfirmScreen(
            confirmed -> {
                confirmOpen = false;
                if (confirmed) {
                    bypassDisconnect = true;
                    try {
                        mc.getRaw().disconnectFromWorld(reason);
                    } finally {
                        bypassDisconnect = false;
                    }
                } else if (mc.getLevel() != null) {
                    mc.setScreen(new PauseScreen(true));
                }
            },
            Component.literal("Hold on..."),
            Component.literal("Do you really want to leave the server?"),
            Component.literal("Yep"),
            Component.literal("Nah")
        )));
        return true;
    }
    private native void nativeBlockQuit(boolean block);
    private native boolean nativeIsQuitBlocked();
}
