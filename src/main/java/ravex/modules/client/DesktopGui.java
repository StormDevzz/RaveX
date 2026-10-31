package ravex.modules.client;
import ravex.modules.annotations.Module;
import ravex.event.Subscribe;
import ravex.event.combat.ModuleToggleEvent;

import ravex.parameter.*;
import java.util.List;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.utility.client.ClientAlertUtility;
@Module(name = "DesktopGui", category = "Client")
public class DesktopGui {
private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_desktopgui");
    public void onEnable() {
        var mc = MinecraftWrapper.getWrapper();
        if (!NATIVE.isLoaded()) {
            ClientAlertUtility.alert(ravex.utility.misc.LanguageUtility.t("desktopgui_nolib"), 0xFFFF5555);
            Modules.setEnabled(DesktopGui.class, false);
            return;
        }
        List<ravex.modules.Module> modules = ravex.manager.ModuleManager.INSTANCE.getModules();
        String[] names = new String[modules.size()];
        boolean[] states = new boolean[modules.size()];
        for (int i = 0; i < modules.size(); i++) {
            names[i] = modules.get(i).getName();
            states[i] = modules.get(i).getEnabled();
        }
        openDesktopGui(names, states);
    }
    public void onDisable() {
        if (NATIVE.isLoaded()) {
            closeDesktopGui();
        }
    }

    @Subscribe
    public void onModuleToggle(ModuleToggleEvent event) {
        if (NATIVE.isLoaded()) {
            updateModuleState(event.getModule().getName(), event.isEnabled());
        }
    }
    public static void toggleModuleFromNative(String name) {
        var mc = MinecraftWrapper.getWrapper();
        mc.execute(() -> {
            ravex.modules.Module m = ravex.manager.ModuleManager.INSTANCE.getByName(name);
            if (m != null) {
                m.toggle();
            }
        });
    }
    public static void onNativeClose() {
        var mc = MinecraftWrapper.getWrapper();
        mc.execute(() -> {
            if (Modules.enabled(DesktopGui.class)) {
                Modules.setEnabled(DesktopGui.class, false);
            }
        });
    }
    public static String getModuleParams(String name) {
        try {
            ravex.modules.Module m = ravex.manager.ModuleManager.INSTANCE.getByName(name);
            if (m == null) return "";
            StringBuilder sb = new StringBuilder();
            for (Parameter<?> p : m.getParameters()) {
                String pname = safeParam(p.getName());
                // clickgui group headers become section titles
                if (p instanceof GroupParameter) {
                    sb.append("group:").append(pname).append('|');
                    continue;
                }
                if (!p.isVisible()) continue;
                if (p instanceof BooleanParameter) {
                    sb.append("bool:").append(pname).append(':')
                      .append(Boolean.TRUE.equals(p.getValue())).append('|');
                } else if (p instanceof NumberParameter np) {
                    sb.append("num:").append(pname).append(':')
                      .append(numText(np.getValue())).append(':')
                      .append(numText(np.getMin())).append(':')
                      .append(numText(np.getMax())).append(':')
                      .append(numText(np.getStep())).append('|');
                } else if (p instanceof ModeParameter mp) {
                    sb.append("mode:").append(pname).append(':')
                      .append(safeParam(String.valueOf(mp.getValue()))).append(':');
                    for (String opt : mp.getModes()) {
                        sb.append(safeParam(opt)).append(',');
                    }
                    sb.append('|');
                } else if (p instanceof StringParameter) {
                    sb.append("str:").append(pname).append(':')
                      .append(safeParam(String.valueOf(p.getValue()))).append('|');
                } else if (p instanceof MultiSelectParameter msp) {
                    sb.append("multi:").append(pname).append(':');
                    for (String selected : msp.getValue()) {
                        sb.append(safeParam(selected)).append(',');
                    }
                    sb.append(':');
                    for (String opt : msp.getOptions()) {
                        sb.append(safeParam(opt)).append(',');
                    }
                    sb.append('|');
                } else if (p instanceof ActionParameter) {
                    sb.append("action:").append(pname).append('|');
                } else if (p instanceof ColorParameter) {
                    sb.append("color:").append(pname).append(':')
                      .append(p.getValue()).append('|');
                }
            }
            if (sb.isEmpty()) return "";
            sb.setLength(sb.length() - 1);
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
    private static String safeParam(String text) {
        if (text == null) return "";
        return text.replace('|', '/').replace(':', ';').replace('\n', ' ').replace('\r', ' ');
    }
    private static String numText(Double value) {
        if (value == null || !Double.isFinite(value)) return "0";
        return Double.toString(value);
    }
    @SuppressWarnings("unchecked")
    public static void setModuleParam(String name, String paramName, String value) {
        try {
            ravex.modules.Module m = ravex.manager.ModuleManager.INSTANCE.getByName(name);
            if (m == null) return;
            for (Parameter<?> p : m.getParameters()) {
                if (!p.getName().equals(paramName)) continue;
                if (p instanceof BooleanParameter bp) {
                    bp.setValue(Boolean.parseBoolean(value));
                } else if (p instanceof NumberParameter np) {
                    double v = Double.parseDouble(value);
                    if (Double.isFinite(v)) np.setValue(v);
                } else if (p instanceof ModeParameter mp) {
                    mp.setValue(value);
                } else if (p instanceof StringParameter sp) {
                    sp.setValue(value);
                } else if (p instanceof MultiSelectParameter msp) {
                    List<String> want = java.util.Arrays.asList(value.split(","));
                    for (String opt : msp.getOptions()) {
                        if (want.contains(opt)) {
                            msp.select(opt);
                        } else {
                            msp.deselect(opt);
                        }
                    }
                } else if (p instanceof ActionParameter ap) {
                    MinecraftWrapper.getWrapper().execute(ap.getValue());
                } else if (p instanceof ColorParameter cp) {
                    cp.setValue((int) Long.parseLong(value));
                }
                break;
            }
        } catch (Exception ignored) {
        }
    }
    private static native void openDesktopGui(String[] names, boolean[] states);
    private static native void updateModuleState(String name, boolean enabled);
    private static native void closeDesktopGui();






}