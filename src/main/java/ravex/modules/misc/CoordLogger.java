package ravex.modules.misc;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.nativelib.NativeLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import java.text.SimpleDateFormat;
import java.util.Date;
public class CoordLogger extends Module {
    public static final CoordLogger INSTANCE = new CoordLogger();
    public final BooleanParameter logDeath = new BooleanParameter("LogDeath", true);
    public final BooleanParameter logJoin = new BooleanParameter("LogJoin", false);
    public final BooleanParameter chatNotify = new BooleanParameter("ChatNotify", true);
    private static final String LOG_DIR = "ravex/coordlogs";
    private String currentFile = null;

    static {
        NativeLoader.load();
    }
    @Override
    protected void onEnable() {
        try {
            nativeEnsureDir(LOG_DIR);
        } catch (UnsatisfiedLinkError ignored) {
            new java.io.File(LOG_DIR).mkdirs();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        currentFile = LOG_DIR + "/session_" + sdf.format(new Date()) + ".log";
        try {
            nativeWriteLog(currentFile, "=== CoordLogger Session Started ===\n");
        } catch (UnsatisfiedLinkError ignored) {}
        if (logJoin.getValue()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                log("JOIN", mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    mc.player.level().dimension().identifier().toString());
            }
        }
    }
    public void onDeath(double x, double y, double z, String dimension) {
        if (!getEnabled() || !logDeath.getValue()) return;
        log("DEATH", x, y, z, dimension);
    }
    private void log(String type, double x, double y, double z, String dim) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(new Date());
        String line = String.format("[%s] %s | X: %.1f Y: %.1f Z: %.1f | Dim: %s\n",
            timestamp, type, x, y, z, dim);
        if (currentFile != null) {
            try {
                nativeWriteLog(currentFile, line);
            } catch (UnsatisfiedLinkError e) {
                try {
                    java.io.FileWriter fw = new java.io.FileWriter(currentFile, true);
                    fw.write(line);
                    fw.close();
                } catch (Exception ignored) {}
            }
        }
        if (chatNotify.getValue()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("§7[§cCoordLogger§7] §f" + type + " at X=" +
                        String.format("%.1f", x) + " Y=" + String.format("%.1f", y) +
                        " Z=" + String.format("%.1f", z)),
                    false
                );
            }
        }
    }
    private native boolean nativeEnsureDir(String path);
    private native boolean nativeWriteLog(String filePath, String content);
}
