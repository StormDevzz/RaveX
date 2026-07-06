package ravex.cmd.cmds;
import org.lwjgl.glfw.GLFW;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import java.util.Locale;
public class BindCmd extends Cmd {
    public BindCmd() {
        super("bind", "Bind a key to a module");
    }
    @Override
    public void execute(String[] args) {
        if (args.length < 3) { CmdReg.print("§c[RaveX] Usage: .bind <module> <key>  e.g. .bind KillAura K"); return; }
        Module m = ModuleManager.INSTANCE.getByName(args[1]);
        if (m == null) { CmdReg.print("§c[RaveX] Module not found: §e" + args[1]); return; }
        String keyName = args[2].toUpperCase(Locale.ROOT);
        int glfwKey = glfwKeyFromName(keyName);
        if (glfwKey == GLFW.GLFW_KEY_UNKNOWN) {
            CmdReg.print("§c[RaveX] Unknown key: §e" + keyName + "  §7(use A-Z, F1-F12, etc.)");
            return;
        }
        m.setKeyBind(glfwKey);
        CmdReg.print("§a[RaveX] Bound §e" + m.getName() + " §7→ §e" + keyName);
    }
    private int glfwKeyFromName(String key) {
        switch (key) {
            case "A": return GLFW.GLFW_KEY_A; case "B": return GLFW.GLFW_KEY_B;
            case "C": return GLFW.GLFW_KEY_C; case "D": return GLFW.GLFW_KEY_D;
            case "E": return GLFW.GLFW_KEY_E; case "F": return GLFW.GLFW_KEY_F;
            case "G": return GLFW.GLFW_KEY_G; case "H": return GLFW.GLFW_KEY_H;
            case "I": return GLFW.GLFW_KEY_I; case "J": return GLFW.GLFW_KEY_J;
            case "K": return GLFW.GLFW_KEY_K; case "L": return GLFW.GLFW_KEY_L;
            case "M": return GLFW.GLFW_KEY_M; case "N": return GLFW.GLFW_KEY_N;
            case "O": return GLFW.GLFW_KEY_O; case "P": return GLFW.GLFW_KEY_P;
            case "Q": return GLFW.GLFW_KEY_Q; case "R": return GLFW.GLFW_KEY_R;
            case "S": return GLFW.GLFW_KEY_S; case "T": return GLFW.GLFW_KEY_T;
            case "U": return GLFW.GLFW_KEY_U; case "V": return GLFW.GLFW_KEY_V;
            case "W": return GLFW.GLFW_KEY_W; case "X": return GLFW.GLFW_KEY_X;
            case "Y": return GLFW.GLFW_KEY_Y; case "Z": return GLFW.GLFW_KEY_Z;
            case "F1": return GLFW.GLFW_KEY_F1; case "F2": return GLFW.GLFW_KEY_F2;
            case "F3": return GLFW.GLFW_KEY_F3; case "F4": return GLFW.GLFW_KEY_F4;
            case "F5": return GLFW.GLFW_KEY_F5; case "F6": return GLFW.GLFW_KEY_F6;
            case "F7": return GLFW.GLFW_KEY_F7; case "F8": return GLFW.GLFW_KEY_F8;
            case "F9": return GLFW.GLFW_KEY_F9; case "F10": return GLFW.GLFW_KEY_F10;
            case "F11": return GLFW.GLFW_KEY_F11; case "F12": return GLFW.GLFW_KEY_F12;
            case "HOME": return GLFW.GLFW_KEY_HOME; case "END": return GLFW.GLFW_KEY_END;
            case "INSERT": return GLFW.GLFW_KEY_INSERT; case "DELETE": return GLFW.GLFW_KEY_DELETE;
            case "PAGEUP": return GLFW.GLFW_KEY_PAGE_UP; case "PAGEDOWN": return GLFW.GLFW_KEY_PAGE_DOWN;
            default: return GLFW.GLFW_KEY_UNKNOWN;
        }
    }
}
