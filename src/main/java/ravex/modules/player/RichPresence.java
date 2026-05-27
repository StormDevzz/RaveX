package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.lua.LuaManager;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.util.List;

public class RichPresence extends Module {
    public static final RichPresence INSTANCE = new RichPresence();

    public final BooleanParameter showHP     = new BooleanParameter("Show HP",     true);
    public final BooleanParameter showCoords = new BooleanParameter("Show Coords", false);

    private RichPresence() {
        super("RichPresence", Category.PLAYER);
        addParameter(showHP);
        addParameter(showCoords);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        File scriptFile = ensureScriptFile(mc);

        if (scriptFile != null && scriptFile.exists()) {
            try {
                LuaManager.INSTANCE.getGlobals().loadfile(scriptFile.getAbsolutePath()).call();
            } catch (Exception e) {
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§7[§cRaveX§7] §cRichPresence Lua error: " + e.getMessage()), false);
                }
            }
        } else {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                        "§7[§cRaveX§7] §eRichPresence: скопируй rich_presence.lua в ravex/scripts/"), false);
            }
        }
    }

    @Override
    protected void onDisable() {
        LuaManager.INSTANCE.onDisableRichPresence();
    }

    private static File ensureScriptFile(Minecraft mc) {
        File scriptsDir = new File(mc.gameDirectory, "ravex/scripts");
        scriptsDir.mkdirs();
        File target = new File(scriptsDir, "rich_presence.lua");
        if (target.exists()) return target;

        try (InputStream in = RichPresence.class.getResourceAsStream("/lua/rich_presence.lua")) {
            if (in != null) {
                try (OutputStream out = new FileOutputStream(target)) {
                    in.transferTo(out);
                }
                return target;
            }
        } catch (Exception ignored) {}
        return target.exists() ? target : null;
    }
}
