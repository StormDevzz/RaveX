package ravex.scratch;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.FileWriter;
import java.io.PrintWriter;

public class DumpRenderAPI {
    public static void main(String[] args) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("render_api_dump.txt"))) {
            writer.println("Dumping NativeImage methods...");
            try {
                Class<?> clazz = Class.forName("com.mojang.blaze3d.platform.NativeImage");
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().contains("Pixel") || m.getName().contains("Color") || m.getName().contains("set")) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("  ").append(Modifier.toString(m.getModifiers())).append(" ")
                          .append(m.getReturnType().getSimpleName()).append(" ")
                          .append(m.getName()).append("(");
                        Class<?>[] params = m.getParameterTypes();
                        for (int i = 0; i < params.length; i++) {
                            sb.append(params[i].getSimpleName());
                            if (i < params.length - 1) sb.append(", ");
                        }
                        sb.append(")");
                        writer.println(sb.toString());
                    }
                }
            } catch (ClassNotFoundException e) {
                writer.println("NativeImage NOT FOUND");
            }
            writer.println("Dump complete!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
