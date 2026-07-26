package ravex.modules.client;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.event.EventBusHolder;
import ravex.event.Subscribe;
import ravex.event.combat.ModuleToggleEvent;

import ravex.parameter.*;
import net.minecraft.client.Minecraft;
import java.util.List;
import ravex.utility.nativelib.NativeLibraryUtility;
@ModuleInfo(name = "DesktopGui", category = "Client")
public class DesktopGui implements ModuleAccess {
private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_desktopgui");
    static {
        NATIVE.load();
    }
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (!NATIVE.isLoaded()) {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§7[§5DesktopGui§7] §cNative library not found!"), false);
            }
            ravex.manager.ModuleManager.INSTANCE.getByName("DesktopGui").setEnabled(false);
            return;
        }
        EventBusHolder.get().subscribe(this);
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
        EventBusHolder.get().unsubscribe(this);
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
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            ravex.modules.Module m = ravex.manager.ModuleManager.INSTANCE.getByName(name);
            if (m != null) {
                m.toggle();
            }
        });
    }
    public static void onNativeClose() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (ravex.manager.ModuleManager.INSTANCE.getByName("DesktopGui").getEnabled()) {
                ravex.manager.ModuleManager.INSTANCE.getByName("DesktopGui").setEnabled(false);
            }
        });
    }
    public static String getModuleParams(String name) {
        ravex.modules.Module m = ravex.manager.ModuleManager.INSTANCE.getByName(name);
        if (m == null) return "";
        List<Parameter<?>> params = m.getParameters();
        if (params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Parameter<?> p : params) {
            String pname = p.getName();
            if (p instanceof BooleanParameter) {
                sb.append("bool:").append(pname).append(":").append(p).append("|");
            } else if (p instanceof NumberParameter np) {
                sb.append("num:").append(pname).append(":").append(np)
                  .append(":").append(np.getMin()).append(":").append(np.getMax()).append(":").append(np.getStep()).append("|");
            } else if (p instanceof ModeParameter mp) {
                sb.append("mode:").append(pname).append(":").append(mp).append(":");
                for (String opt : mp.getModes()) {
                    sb.append(opt).append(",");
                }
                sb.append("|");
            } else if (p instanceof StringParameter) {
                sb.append("str:").append(pname).append(":").append(p).append("|");
            } else if (p instanceof ActionParameter) {
                sb.append("action:").append(pname).append("|");
            } else if (p instanceof ColorParameter) {
                sb.append("color:").append(pname).append(":").append(p).append("|");
            }
        }
        if (!sb.isEmpty()) sb.setLength(sb.length() - 1);
        return sb.toString();
    }
    @SuppressWarnings("unchecked")
    public static void setModuleParam(String name, String paramName, String value) {
        ravex.modules.Module m = ravex.manager.ModuleManager.INSTANCE.getByName(name);
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

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("DesktopGui").getEnabled();
    }

    public static DesktopGui itz() {
        return ravex.manager.ModuleManager.delegate(DesktopGui.class);
    }


}