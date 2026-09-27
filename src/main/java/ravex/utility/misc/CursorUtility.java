package ravex.utility.misc;

import org.lwjgl.glfw.GLFW;
import ravex.mcwrapper.MinecraftWrapper;

public final class CursorUtility {
    private static long handCursor;
    private static long crosshairCursor;
    private static long activeCursor;

    private CursorUtility() {}

    public static void setHand() {
        if (handCursor == 0) handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
        apply(handCursor);
    }

    public static void setCrosshair() {
        if (crosshairCursor == 0) crosshairCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR);
        apply(crosshairCursor);
    }

    public static void reset() {
        apply(0);
    }

    private static void apply(long cursor) {
        if (cursor == activeCursor) return;
        long handle = MinecraftWrapper.getWrapper().getWindowHandle();
        if (handle == 0) return;
        GLFW.glfwSetCursor(handle, cursor);
        activeCursor = cursor;
    }
}
