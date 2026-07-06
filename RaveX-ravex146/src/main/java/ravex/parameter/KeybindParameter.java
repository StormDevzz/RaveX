package ravex.parameter;

public class KeybindParameter extends Parameter<Integer> {
    public KeybindParameter(String name, int defaultValue) {
        super(name, defaultValue);
    }

    public static String getKeyName(int key) {
        if (key <= 0) return "None";
        String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase();
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) return "RSHIFT";
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) return "LSHIFT";
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) return "SPACE";
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) return "ESC";
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) return "ENTER";
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) return "TAB";
        return "KEY_" + key;
    }
}
