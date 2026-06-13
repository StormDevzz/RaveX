package ravex.modules.client;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.modules.ModuleManager;
import ravex.parameter.*;
import net.minecraft.client.Minecraft;
import java.util.List;

public class DesktopGui extends Module {
    public static final DesktopGui INSTANCE = new DesktopGui();

    private static boolean nativeAvailable = false;

    static {
        nativeAvailable = ravex.utility.misc.NativeLoader.loadLibrary("ravex_desktopgui");
    }

    private DesktopGui() {
        super("DesktopGui", Category.CLIENT);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (!nativeAvailable) {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§7[§5DesktopGui§7] §cNative library not found!"), false);
            }
            setEnabled(false);
            return;
        }

        List<Module> modules = ModuleManager.INSTANCE.getModules();
        String[] names = new String[modules.size()];
        boolean[] states = new boolean[modules.size()];
        for (int i = 0; i < modules.size(); i++) {
            names[i] = modules.get(i).getName();
            states[i] = modules.get(i).getEnabled();
        }

        openDesktopGui(names, states);
    }

    @Override
    protected void onDisable() {
        if (nativeAvailable) {
            closeDesktopGui();
        }
    }

    public static void onModuleToggle(String name, boolean enabled) {
        if (INSTANCE.getEnabled() && nativeAvailable) {
            updateModuleState(name, enabled);
        }
    }

    public static void toggleModuleFromNative(String name) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            Module m = ModuleManager.INSTANCE.getByName(name);
            if (m != null) {
                m.toggle();
            }
        });
    }

    public static void onNativeClose() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (INSTANCE.getEnabled()) {
                INSTANCE.setEnabled(false);
            }
        });
    }

    public static String getModuleParams(String name) {
        Module m = ModuleManager.INSTANCE.getByName(name);
        if (m == null) return "";

        List<Parameter<?>> params = m.getParameters();
        if (params.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (Parameter<?> p : params) {
            String pname = p.getName();
            if (p instanceof BooleanParameter) {
                sb.append("bool:").append(pname).append(":").append(p.getValue()).append("|");
            } else if (p instanceof NumberParameter np) {
                sb.append("num:").append(pname).append(":").append(np.getValue())
                  .append(":").append(np.getMin()).append(":").append(np.getMax()).append(":").append(np.getStep()).append("|");
            } else if (p instanceof ModeParameter mp) {
                sb.append("mode:").append(pname).append(":").append(mp.getValue()).append(":");
                for (String opt : mp.getModes()) {
                    sb.append(opt).append(",");
                }
                sb.append("|");
            } else if (p instanceof StringParameter) {
                sb.append("str:").append(pname).append(":").append(p.getValue()).append("|");
            } else if (p instanceof ActionParameter) {
                sb.append("action:").append(pname).append("|");
            } else if (p instanceof ColorParameter) {
                sb.append("color:").append(pname).append(":").append(p.getValue()).append("|");
            }
        }
        if (!sb.isEmpty()) sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static void setModuleParam(String name, String paramName, String value) {
        Module m = ModuleManager.INSTANCE.getByName(name);
        if (m == null) return;

        for (Parameter<?> p : m.getParameters()) {
            if (!p.getName().equals(paramName)) continue;

            if (p instanceof BooleanParameter bp) {
                bp.setValue(Boolean.parseBoolean(value));
            } else if (p instanceof NumberParameter np) {
                np.setValue(Double.parseDouble(value));
            } else if (p instanceof ModeParameter mp) {
                mp.setValue(value);
            } else if (p instanceof StringParameter sp) {
                sp.setValue(value);
            } else if (p instanceof ActionParameter ap) {
                ap.getValue().run();
            } else if (p instanceof ColorParameter cp) {
                cp.setValue(Integer.parseInt(value));
            }
            break;
        }
    }

    private static native void openDesktopGui(String[] names, boolean[] states);
    private static native void updateModuleState(String name, boolean enabled);
    private static native void closeDesktopGui();
}
