package ravex.modules.misc;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.io.*;

public class Optimizer extends Module {
    public static final Optimizer INSTANCE = new Optimizer();

    public final ModeParameter gcMode    = new ModeParameter("GC Mode", "Soft",
            List.of("Soft", "Normal", "Aggressive"));
    public final BooleanParameter notify = new BooleanParameter("Chat Notify", false);
    public final BooleanParameter useNative = new BooleanParameter("Native C++", true);

    private Optimizer() {
        super("Optimizer", Category.MISC);
        addParameter(gcMode);
        addParameter(notify);
        addParameter(useNative);
    }

    @Override
    protected void onEnable() {
        runOptimization();
        setEnabled(false);
    }

    private void runOptimization() {
        Minecraft mc = Minecraft.getInstance();
        String mode = gcMode.getValue();

        if (useNative.getValue()) {
            String result = runNativeOptimizer(mode);
            if (result != null && notify.getValue() && mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("§7[§cRaveX§7] §a" + result), false);
            }
            return;
        }

        switch (mode) {
            case "Aggressive" -> { System.gc(); System.gc(); System.gc(); }
            case "Normal" -> System.gc();
            case "Soft" -> {
                Runtime rt = Runtime.getRuntime();
                long used = rt.totalMemory() - rt.freeMemory();
                long max  = rt.maxMemory();
                if ((double) used / max > 0.65) System.gc();
            }
        }

        if (notify.getValue() && mc.player != null) {
            Runtime rt = Runtime.getRuntime();
            long freeMb = (rt.maxMemory() - (rt.totalMemory() - rt.freeMemory())) / (1024L * 1024L);
            mc.player.displayClientMessage(
                Component.literal("§7[§cRaveX§7] §aOptimizer: ~" + freeMb + " MB free after " + mode + " GC."),
                false);
        }
    }

    private String runNativeOptimizer(String mode) {
        try {
            File bin = findBinary();
            if (bin == null || !bin.exists()) return null;

            Process proc = new ProcessBuilder(bin.getAbsolutePath())
                .redirectErrorStream(true).start();

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(proc.getOutputStream()))) {
                writer.write("optimize " + mode);
                writer.newLine();
                writer.flush();
                writer.write("exit");
                writer.newLine();
                writer.flush();
            }

            String line;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()))) {
                line = reader.readLine();
            }

            proc.waitFor();
            if (line == null) return null;

            if (line.contains("\"message\"")) {
                int start = line.indexOf("\"message\":\"") + 11;
                int end = line.indexOf("\"", start);
                return line.substring(start, end).replace("\\\"", "\"");
            }
            return "Native optimizer: OK";
        } catch (Exception e) {
            return null;
        }
    }

    private File findBinary() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;

        File gameDir = mc.gameDirectory;
        File[] candidates = {
            new File("src/main/resources/optimizer"),
            new File(gameDir, "ravex/optimizer"),
            new File(System.getProperty("user.dir"), "optimizer"),
            new File("/usr/local/bin/ravex-optimizer")
        };
        for (File f : candidates) {
            if (f.exists()) return f;
        }
        return null;
    }
}
