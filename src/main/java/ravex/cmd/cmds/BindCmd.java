package ravex.cmd.cmds;
import org.lwjgl.glfw.GLFW;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.modules.client.Commands;
import java.util.Locale;
public class BindCmd extends Cmd {
    public BindCmd() {
        super("bind", "Bind a key to a module");
    }
    @Override
    public void execute(String[] args) {
        String pref = ModuleManager.get(Commands.class).prefix.getValue();
        if (args.length < 2) { CmdReg.print("§c[RaveX] Usage: " + pref + "bind <module/list/clear> [key]"); return; }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list": {
                boolean any = false;
                for (Module m : ModuleManager.INSTANCE.getModules()) {
                    if (m.getKeyBind() != -1) {
                        if (!any) { CmdReg.print("§7Bound modules:"); any = true; }
                        CmdReg.print(" §e" + m.getName() + " §7→ §e" + glfwNameFromKey(m.getKeyBind()));
                    }
                }
                if (!any) CmdReg.print("§e[RaveX] No modules are bound.");
                break;
            }
            case "clear": {
                int count = 0;
                for (Module m : ModuleManager.INSTANCE.getModules()) {
                    if (m.getKeyBind() != -1) { m.setKeyBind(-1); count++; }
                }
                CmdReg.print("§a[RaveX] Cleared §e" + count + " §abind(s).");
                break;
            }
            default: {
                Module m = ModuleManager.INSTANCE.getByName(args[1]);
                if (m == null) { CmdReg.print("§c[RaveX] Module not found: §e" + args[1]); return; }
                if (args.length < 3) { CmdReg.print("§c[RaveX] Usage: " + pref + "bind <module> <key>"); return; }
                String keyName = args[2].toUpperCase(Locale.ROOT);
                int glfwKey = glfwKeyFromName(keyName);
                if (glfwKey == GLFW.GLFW_KEY_UNKNOWN) {
                    CmdReg.print("§c[RaveX] Unknown key: §e" + keyName + "  §7(use A-Z, F1-F12, etc.)");
                    return;
                }
                m.setKeyBind(glfwKey);
                CmdReg.print("§a[RaveX] Bound §e" + m.getName() + " §7→ §e" + keyName);
            }
        }
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
    private String glfwNameFromKey(int key) {
        switch (key) {
            case GLFW.GLFW_KEY_A: return "A"; case GLFW.GLFW_KEY_B: return "B";
            case GLFW.GLFW_KEY_C: return "C"; case GLFW.GLFW_KEY_D: return "D";
            case GLFW.GLFW_KEY_E: return "E"; case GLFW.GLFW_KEY_F: return "F";
            case GLFW.GLFW_KEY_G: return "G"; case GLFW.GLFW_KEY_H: return "H";
            case GLFW.GLFW_KEY_I: return "I"; case GLFW.GLFW_KEY_J: return "J";
            case GLFW.GLFW_KEY_K: return "K"; case GLFW.GLFW_KEY_L: return "L";
            case GLFW.GLFW_KEY_M: return "M"; case GLFW.GLFW_KEY_N: return "N";
            case GLFW.GLFW_KEY_O: return "O"; case GLFW.GLFW_KEY_P: return "P";
            case GLFW.GLFW_KEY_Q: return "Q"; case GLFW.GLFW_KEY_R: return "R";
            case GLFW.GLFW_KEY_S: return "S"; case GLFW.GLFW_KEY_T: return "T";
            case GLFW.GLFW_KEY_U: return "U"; case GLFW.GLFW_KEY_V: return "V";
            case GLFW.GLFW_KEY_W: return "W"; case GLFW.GLFW_KEY_X: return "X";
            case GLFW.GLFW_KEY_Y: return "Y"; case GLFW.GLFW_KEY_Z: return "Z";
            case GLFW.GLFW_KEY_F1: return "F1"; case GLFW.GLFW_KEY_F2: return "F2";
            case GLFW.GLFW_KEY_F3: return "F3"; case GLFW.GLFW_KEY_F4: return "F4";
            case GLFW.GLFW_KEY_F5: return "F5"; case GLFW.GLFW_KEY_F6: return "F6";
            case GLFW.GLFW_KEY_F7: return "F7"; case GLFW.GLFW_KEY_F8: return "F8";
            case GLFW.GLFW_KEY_F9: return "F9"; case GLFW.GLFW_KEY_F10: return "F10";
            case GLFW.GLFW_KEY_F11: return "F11"; case GLFW.GLFW_KEY_F12: return "F12";
            case GLFW.GLFW_KEY_HOME: return "HOME"; case GLFW.GLFW_KEY_END: return "END";
            case GLFW.GLFW_KEY_INSERT: return "INSERT"; case GLFW.GLFW_KEY_DELETE: return "DELETE";
            case GLFW.GLFW_KEY_PAGE_UP: return "PAGEUP"; case GLFW.GLFW_KEY_PAGE_DOWN: return "PAGEDOWN";
            default: return "NONE";
        }
    }
}
