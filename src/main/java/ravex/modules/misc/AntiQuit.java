package ravex.modules.misc;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.utility.nativelib.NativeLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import java.util.List;
public class AntiQuit extends Module {
    public static final AntiQuit INSTANCE = new AntiQuit();
    public final ModeParameter mode = new ModeParameter("Mode", "Server",
        List.of("Server", "Game", "Both"));

    static {
        NativeLoader.load();
    }
    @Override
    protected void onEnable() {
        try {
            nativeBlockQuit(true);
        } catch (UnsatisfiedLinkError ignored) {}
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        if (window == null) return;
        window.setWindowCloseCallback(() -> {
            String m = mode.getValue();
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
    @Override
    protected void onDisable() {
        try {
            nativeBlockQuit(false);
        } catch (UnsatisfiedLinkError ignored) {}
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        if (window != null) {
            window.setWindowCloseCallback(null);
        }
    }
    public boolean shouldBlockDisconnect() {
        if (!getEnabled()) return false;
        String m = mode.getValue();
        return m.equals("Server") || m.equals("Both");
    }
    private native void nativeBlockQuit(boolean block);
    private native boolean nativeIsQuitBlocked();
}
