package ravex.manager;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ravex.modules.misc.Commands;
import ravex.modules.Module;
import ravex.modules.ModuleManager;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

public class CommandProcessor {
    public static final CommandProcessor INSTANCE = new CommandProcessor();

    private CommandProcessor() {}

    
    public boolean processCommand(String message) {
        if (!Commands.INSTANCE.getEnabled()) return false;

        String pref = Commands.INSTANCE.prefix.getValue();
        if (!message.startsWith(pref)) return false;

        String rawCmd = message.substring(pref.length()).trim();
        if (rawCmd.isEmpty()) return true;

        String[] args = rawCmd.split("\\s+");
        String cmd = args[0].toLowerCase(Locale.ROOT);

        switch (cmd) {
            case "cfg":
            case "config":
                handleConfigCommand(args);
                break;
            case "help":
                handleHelp();
                break;
            case "calc":
                handleCalc(args);
                break;
            case "plugins":
                handlePlugins();
                break;
            case "toggle":
                handleToggle(args);
                break;
            case "bind":
                handleBind(args);
                break;
            case "panic":
                handlePanic();
                break;
            case "say":
                handleSay(args);
                break;
            case "time":
                handleTime();
                break;
            case "modules":
                handleModules();
                break;
            case "coords":
                handleCoords();
                break;
            case "waypoint":
            case "wp":
                handleWaypointCommand(args);
                break;
            case "benchmark":
            case "bench":
                handleBenchmark(args);
                break;
            case "friend":
            case "f":
                handleFriendCommand(args);
                break;
            default:
                printMessage("§c[RaveX] Unknown command. Type §e" + pref + "help §cfor a list of commands.");
        }
        return true;
    }

    

    private void handleConfigCommand(String[] args) {
        if (args.length < 2) { printHelp(); return; }
        String sub = args[1].toLowerCase(Locale.ROOT);
        String pref = Commands.INSTANCE.prefix.getValue();
        switch (sub) {
            case "save":
                if (args.length < 3) { printMessage("§c[RaveX] Usage: " + pref + "config save <name>"); return; }
                if (ConfigManager.INSTANCE.save(args[2]))
                    printMessage("§a[RaveX] Saved config: §e" + args[2]);
                else
                    printMessage("§c[RaveX] Failed to save config: §e" + args[2]);
                break;
            case "load":
                if (args.length < 3) { printMessage("§c[RaveX] Usage: " + pref + "config load <name>"); return; }
                if (ConfigManager.INSTANCE.load(args[2]))
                    printMessage("§a[RaveX] Loaded config: §e" + args[2]);
                else
                    printMessage("§c[RaveX] Failed to load config (does it exist?): §e" + args[2]);
                break;
            case "list":
                List<String> configs = ConfigManager.INSTANCE.list();
                if (configs.isEmpty()) { printMessage("§e[RaveX] No configurations saved."); return; }
                printMessage("§5[RaveX] Configs: §7" + String.join("§r, §e", configs));
                break;
            case "delete":
                if (args.length < 3) { printMessage("§c[RaveX] Usage: " + pref + "config delete <name>"); return; }
                if (ConfigManager.INSTANCE.delete(args[2]))
                    printMessage("§a[RaveX] Deleted config: §e" + args[2]);
                else
                    printMessage("§c[RaveX] Failed to delete config: §e" + args[2]);
                break;
            default:
                printHelp();
        }
    }

    private void printHelp() {
        String p = Commands.INSTANCE.prefix.getValue();
        printMessage("§5[RaveX] Config commands: §e" + p + "config save/load/list/delete <name>");
    }

    

    private void handleHelp() {
        String p = Commands.INSTANCE.prefix.getValue();
        printMessage("§5[RaveX] §7════ Command List ════");
        printMessage(" §e" + p + "help            §7- Show this list");
        printMessage(" §e" + p + "calc <expr>     §7- Calculate a math expression");
        printMessage(" §e" + p + "plugins         §7- Request server plugin list");
        printMessage(" §e" + p + "toggle <module> §7- Toggle a module on/off");
        printMessage(" §e" + p + "bind <mod> <key>§7- Bind a key to a module");
        printMessage(" §e" + p + "panic           §7- Disable ALL modules");
        printMessage(" §e" + p + "say <message>   §7- Send a raw chat message");
        printMessage(" §e" + p + "time            §7- Show current local time");
        printMessage(" §e" + p + "modules         §7- List all enabled modules");
        printMessage(" §e" + p + "coords          §7- Show your coordinates");
        printMessage(" §e" + p + "config save/load/list/delete <name>");
        printMessage(" §e" + p + "waypoint add/remove/list/clear [name]");
        printMessage(" §e" + p + "benchmark      §7- Run system benchmarks");
    }

    

    private void handleCalc(String[] args) {
        if (args.length < 2) {
            printMessage("§c[RaveX] Usage: .calc <expression>   e.g. .calc 3*(4+2)/6");
            return;
        }
        String expr = String.join("", java.util.Arrays.copyOfRange(args, 1, args.length))
                           .replace(",", ".");
        try {
            double result = evalExpr(expr, new int[]{0});
            
            String resultStr = (result == Math.floor(result) && !Double.isInfinite(result))
                ? String.valueOf((long) result)
                : String.format("%.6f", result).replaceAll("0+$", "").replaceAll("\\.$", "");
            printMessage("§a[RaveX] §e" + expr + " §7= §a" + resultStr);
        } catch (Exception e) {
            printMessage("§c[RaveX] Invalid expression: §e" + expr);
        }
    }

    
    private double evalExpr(String expr, int[] pos) {
        double left = evalTerm(expr, pos);
        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op == '+') { pos[0]++; left += evalTerm(expr, pos); }
            else if (op == '-') { pos[0]++; left -= evalTerm(expr, pos); }
            else break;
        }
        return left;
    }

    private double evalTerm(String expr, int[] pos) {
        double left = evalPow(expr, pos);
        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op == '*') { pos[0]++; left *= evalPow(expr, pos); }
            else if (op == '/') { pos[0]++; left /= evalPow(expr, pos); }
            else break;
        }
        return left;
    }

    private double evalPow(String expr, int[] pos) {
        double base = evalUnary(expr, pos);
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '^') {
            pos[0]++;
            return Math.pow(base, evalPow(expr, pos));
        }
        return base;
    }

    private double evalUnary(String expr, int[] pos) {
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '-') {
            pos[0]++;
            return -evalAtom(expr, pos);
        }
        return evalAtom(expr, pos);
    }

    private double evalAtom(String expr, int[] pos) {
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '(') {
            pos[0]++;
            double v = evalExpr(expr, pos);
            if (pos[0] < expr.length() && expr.charAt(pos[0]) == ')') pos[0]++;
            return v;
        }
        int start = pos[0];
        while (pos[0] < expr.length() && (Character.isDigit(expr.charAt(pos[0])) || expr.charAt(pos[0]) == '.')) {
            pos[0]++;
        }
        return Double.parseDouble(expr.substring(start, pos[0]));
    }

    

    private void handlePlugins() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        mc.player.connection.sendChat("/plugins");
        printMessage("§7[RaveX] Sent §e/plugins §7to server — check server response in chat.");
    }

    

    private void handleToggle(String[] args) {
        if (args.length < 2) {
            printMessage("§c[RaveX] Usage: .toggle <module>");
            return;
        }
        String name = args[1];
        Module m = ModuleManager.INSTANCE.getByName(name);
        if (m == null) {
            printMessage("§c[RaveX] Module not found: §e" + name);
            return;
        }
        m.toggle();
        printMessage("§a[RaveX] §e" + m.getName() + " §7→ " + (m.getEnabled() ? "§aON" : "§cOFF"));
    }

    

    private void handleBind(String[] args) {
        if (args.length < 3) {
            printMessage("§c[RaveX] Usage: .bind <module> <key>  e.g. .bind KillAura K");
            return;
        }
        Module m = ModuleManager.INSTANCE.getByName(args[1]);
        if (m == null) { printMessage("§c[RaveX] Module not found: §e" + args[1]); return; }
        String keyName = args[2].toUpperCase(Locale.ROOT);
        int glfwKey = glfwKeyFromName(keyName);
        if (glfwKey == GLFW.GLFW_KEY_UNKNOWN) {
            printMessage("§c[RaveX] Unknown key: §e" + keyName + "  §7(use A-Z, F1-F12, etc.)");
            return;
        }
        m.setKeyBind(glfwKey);
        printMessage("§a[RaveX] Bound §e" + m.getName() + " §7→ §e" + keyName);
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

    

    private void handlePanic() {
        int count = 0;
        for (Module m : ModuleManager.INSTANCE.getModules()) {
            if (m.getEnabled()) { m.setEnabled(false); count++; }
        }
        printMessage("§c[RaveX] §ePanic! §cDisabled §e" + count + " §cmodules.");
    }

    

    private void handleSay(String[] args) {
        if (args.length < 2) { printMessage("§c[RaveX] Usage: .say <message>"); return; }
        String msg = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.connection.sendChat(msg);
    }

    

    private void handleTime() {
        java.time.LocalTime time = java.time.LocalTime.now();
        String timeStr = String.format("%02d:%02d:%02d", time.getHour(), time.getMinute(), time.getSecond());
        printMessage("§5[RaveX] §7Local time: §e" + timeStr);
    }

    

    private void handleModules() {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Module m : ModuleManager.INSTANCE.getModules()) {
            if (m.getEnabled()) {
                if (count > 0) sb.append("§7, ");
                sb.append("§e").append(m.getName());
                count++;
            }
        }
        if (count == 0) {
            printMessage("§7[RaveX] No modules are enabled.");
        } else {
            printMessage("§5[RaveX] §7Active (§e" + count + "§7): " + sb);
        }
    }

    

    private void handleCoords() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var p = mc.player;
        printMessage(String.format("§5[RaveX] §7XYZ: §e%.1f §7/ §e%.1f §7/ §e%.1f", p.getX(), p.getY(), p.getZ()));
        if (mc.level != null) {
            var dim = mc.level.dimensionType();
            
            if (mc.level.dimension().equals(net.minecraft.world.level.Level.NETHER)) {
                printMessage(String.format("§7→ Overworld: §e%.1f §7/ §7- §7/ §e%.1f", p.getX() * 8, p.getZ() * 8));
            } else if (mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
                printMessage(String.format("§7→ Nether: §e%.1f §7/ §7- §7/ §e%.1f", p.getX() / 8, p.getZ() / 8));
            }
        }
    }

    

    private void handleWaypointCommand(String[] args) {
        String pref = Commands.INSTANCE.prefix.getValue();
        if (args.length < 2) {
            printMessage("§c[RaveX] Usage: " + pref + "waypoint <add/remove/list/clear> [name]");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        Minecraft mc = Minecraft.getInstance();
        switch (sub) {
            case "add": {
                if (args.length < 3) {
                    printMessage("§c[RaveX] Usage: " + pref + "waypoint add <name>");
                    return;
                }
                String name = args[2];
                if (mc.player == null || mc.level == null) return;
                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();
                String dim = mc.level.dimension().identifier().toString();
                ravex.modules.render.Waypoint.INSTANCE.addWaypoint(name, x, y, z, dim);
                printMessage("§a[RaveX] Added waypoint: §e" + name + " §7(" + String.format("%.1f", x) + ", " + String.format("%.1f", y) + ", " + String.format("%.1f", z) + ")");
                break;
            }
            case "remove":
            case "del": {
                if (args.length < 3) {
                    printMessage("§c[RaveX] Usage: " + pref + "waypoint remove <name>");
                    return;
                }
                if (ravex.modules.render.Waypoint.INSTANCE.removeWaypoint(args[2])) {
                    printMessage("§a[RaveX] Removed waypoint: §e" + args[2]);
                } else {
                    printMessage("§c[RaveX] Waypoint not found: §e" + args[2]);
                }
                break;
            }
            case "list": {
                var waypoints = ravex.modules.render.Waypoint.getWaypoints();
                if (waypoints.isEmpty()) {
                    printMessage("§e[RaveX] No waypoints saved.");
                    return;
                }
                printMessage("§5[RaveX] Waypoints: §7(" + waypoints.size() + ")");
                for (var wp : waypoints) {
                    String dim = wp.dimension();
                    if (dim.startsWith("minecraft:")) dim = dim.substring(10);
                    double dist = 0;
                    if (mc.player != null) {
                        dist = Math.sqrt(
                            (wp.x() + 0.5 - mc.player.getX()) * (wp.x() + 0.5 - mc.player.getX()) +
                            (wp.y() + 0.5 - mc.player.getY()) * (wp.y() + 0.5 - mc.player.getY()) +
                            (wp.z() + 0.5 - mc.player.getZ()) * (wp.z() + 0.5 - mc.player.getZ())
                        );
                    }
                    printMessage(" §e" + wp.name() + " §7- " + String.format("%.1f", wp.x()) + " " + String.format("%.1f", wp.y()) + " " + String.format("%.1f", wp.z()) + " §8[" + dim + "] §7(" + String.format("%.0f", dist) + "m)");
                }
                break;
            }
            case "clear": {
                int count = ravex.modules.render.Waypoint.INSTANCE.waypoints.size();
                ravex.modules.render.Waypoint.INSTANCE.clearWaypoints();
                printMessage("§a[RaveX] Cleared §e" + count + " §awaypoints.");
                break;
            }
            default:
                printMessage("§c[RaveX] Unknown subcommand. Use add, remove, list, or clear.");
        }
    }

    private void handleBenchmark(String[] args) {
        printMessage("§5[RaveX] §7════ Benchmarks ════");
        Runtime rt = Runtime.getRuntime();
        int cores = rt.availableProcessors();
        long maxMem = rt.maxMemory() / (1024L * 1024L);
        long totalMem = rt.totalMemory() / (1024L * 1024L);
        long freeMem = rt.freeMemory() / (1024L * 1024L);
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        printMessage(" §7OS: §e" + os + " §7(" + arch + ")");
        printMessage(" §7CPU cores: §e" + cores);
        printMessage(" §7Memory: §e" + (maxMem == Long.MAX_VALUE ? "unlimited" : maxMem + " MB") + " §7(total: §e" + totalMem + " MB§7, free: §e" + freeMem + " MB§7)");

        boolean hasNative = false;
        try {
            Class<?> bridge = Class.forName("ravex.benchmark.BenchmarkBridge");
            hasNative = true;
        } catch (ClassNotFoundException e) {
        }

        if (hasNative && args.length > 1 && args[1].equals("native")) {
            printMessage("§7Running native C++ benchmarks...");
            try {
                Class<?> bridge = Class.forName("ravex.benchmark.BenchmarkBridge");
                java.lang.reflect.Method cpu = bridge.getMethod("runCPUBenchmark");
                java.lang.reflect.Method mem = bridge.getMethod("runMemoryBenchmark");
                String cpuRes = (String) cpu.invoke(null);
                String memRes = (String) mem.invoke(null);
                printMessage(" §7" + cpuRes);
                printMessage(" §7" + memRes);
            } catch (Exception e) {
                printMessage("§c[RaveX] Native benchmark error: §e" + e.getMessage());
            }
        } else {
            printMessage("§7Running Java CPU benchmark...");
            long start = System.nanoTime();
            int iterations = 2000000;
            double pi = 0;
            for (int i = 1; i <= iterations; i++) {
                pi += (i % 2 == 1 ? 1.0 : -1.0) / (2 * i - 1);
            }
            pi *= 4;
            long elapsed = System.nanoTime() - start;
            double ms = elapsed / 1_000_000.0;
            printMessage(" §7π calc (2M iter): §e" + String.format("%.2f", ms) + " ms §7(result: §e" + String.format("%.6f", pi) + "§7)");

            printMessage("§7Running Java memory benchmark...");
            int allocSize = 1024 * 1024;
            int blocks = Math.min(100, (int) (freeMem / 2));
            start = System.nanoTime();
            java.util.ArrayList<byte[]> list = new java.util.ArrayList<>();
            for (int i = 0; i < blocks; i++) {
                list.add(new byte[allocSize]);
            }
            for (byte[] b : list) {
                java.util.Arrays.fill(b, (byte) 0xFF);
            }
            list.clear();
            elapsed = System.nanoTime() - start;
            ms = elapsed / 1_000_000.0;
            printMessage(" §7alloc/write/free " + blocks + " MB: §e" + String.format("%.2f", ms) + " ms");

            if (hasNative) {
                printMessage("§7Tip: add §e.native §7to use C++ benchmarks");
            }
        }
    }

    private void handleFriendCommand(String[] args) {
        String pref = Commands.INSTANCE.prefix.getValue();
        if (args.length < 2) {
            printMessage("§c[RaveX] Usage: " + pref + "friend <add/remove/list> [name]");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add":
                if (args.length < 3) {
                    printMessage("§c[RaveX] Usage: " + pref + "friend add <name>");
                    return;
                }
                ravex.manager.FriendManager.INSTANCE.addFriend(args[2]);
                printMessage("§a[RaveX] Added friend: §e" + args[2]);
                break;
            case "remove":
            case "del":
                if (args.length < 3) {
                    printMessage("§c[RaveX] Usage: " + pref + "friend remove <name>");
                    return;
                }
                ravex.manager.FriendManager.INSTANCE.removeFriend(args[2]);
                printMessage("§a[RaveX] Removed friend: §e" + args[2]);
                break;
            case "list":
                java.util.Set<String> friends = ravex.manager.FriendManager.INSTANCE.getFriends();
                if (friends.isEmpty()) {
                    printMessage("§e[RaveX] Your friend list is empty.");
                } else {
                    printMessage("§5[RaveX] Friends: §e" + String.join("§r, §e", friends));
                }
                break;
            default:
                printMessage("§c[RaveX] Unknown subcommand. Use add, remove, or list.");
        }
    }

    

    private void printMessage(String text) {
        if (!Commands.INSTANCE.showFeedback.getValue()) {
            if (!text.contains("§c") && !text.contains("§5") && !text.contains("Help")) return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(text), false);
        }
    }
}
