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
    @Parameter(name = "Mode", modes = {"Server", "Game", "Both"})
    public String mode = "Server";

    static {
        NativeLoader.load();
    }
    public void onEnable() {
        try {
            nativeBlockQuit(true);
        } catch (UnsatisfiedLinkError ignored) {}
        var mc = MinecraftWrapper.getWrapper();
        Window window = mc.getWindow();
        if (window == null) return;
        window.setWindowCloseCallback(() -> {
            String m = mode;
            if (m.equals("Game") || m.equals("Both")) {
                long handle = window.handle();
                GLFW.glfwSetWindowShouldClose(handle, false);
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
                    Component.literal("Do you really want to quit?"),
                    Component.literal("Yep"),
                    Component.literal("Nah")
                )));
            } else {
                long handle = window.handle();
                GLFW.glfwSetWindowShouldClose(handle, false);
            }
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
    public static boolean shouldBlockDisconnect() {
        AntiQuit $ = Modules.get(AntiQuit.class);
        if ($ == null || !Modules.enabled(AntiQuit.class)) return false;
        String m = $.mode;
        return m.equals("Server") || m.equals("Both");
    }
    private native void nativeBlockQuit(boolean block);
    private native boolean nativeIsQuitBlocked();
}
