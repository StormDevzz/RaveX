package ravex.modules.client;

import net.minecraft.client.gui.screens.Screen;
import ravex.gui.calculator.CalculatorScreen;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.modules.annotations.Module;

/**
 * Calculator module. The GUI is a regular Minecraft screen backed by the
 * Rust expression engine (CalcUtility); the old GTK C++ native is no longer
 * loaded because opening its window crashed the client (SIGSEGV in
 * libstdc++/codecvt on a native thread).
 */
@Module(name = "Calculator", category = "Client")
public class Calculator {
    private static boolean closingSilently;

    public void onEnable() {
        MinecraftWrapper mc = MinecraftWrapper.getWrapper();
        if (mc == null) {
            return;
        }
        mc.execute(() -> {
            if (!Modules.enabled(Calculator.class)) {
                return;
            }
            Screen parent = mc.getScreen();
            mc.setScreen(new CalculatorScreen(parent));
        });
    }

    public void onDisable() {
        if (closingSilently) {
            return;
        }
        MinecraftWrapper mc = MinecraftWrapper.getWrapper();
        if (mc == null) {
            return;
        }
        mc.execute(() -> {
            if (mc.getScreen() instanceof CalculatorScreen) {
                mc.setScreen(null);
            }
        });
    }

    /** Called by the screen when the user closes it (Esc / parent restore). */
    public static void onScreenClosed() {
        closingSilently = true;
        try {
            if (Modules.enabled(Calculator.class)) {
                Modules.setEnabled(Calculator.class, false);
            }
        } finally {
            closingSilently = false;
        }
    }
}
