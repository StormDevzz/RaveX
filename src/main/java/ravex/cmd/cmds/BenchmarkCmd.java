package ravex.cmd.cmds;
import ravex.cmd.core.Cmd;
import ravex.cmd.core.CmdReg;
public class BenchmarkCmd extends Cmd {
    public BenchmarkCmd() {
        super("benchmark", "Run system benchmarks", "bench");
    }
    @Override
    public void execute(String[] args) {
        CmdReg.print("§5[RaveX] §7════ Benchmarks ════");
        Runtime rt = Runtime.getRuntime();
        int cores = rt.availableProcessors();
        long maxMem = rt.maxMemory() / (1024L * 1024L);
        long totalMem = rt.totalMemory() / (1024L * 1024L);
        long freeMem = rt.freeMemory() / (1024L * 1024L);
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        CmdReg.print(" §7OS: §e" + os + " §7(" + arch + ")");
        CmdReg.print(" §7CPU cores: §e" + cores);
        CmdReg.print(" §7Memory: §e" + (maxMem == Long.MAX_VALUE ? "unlimited" : maxMem + " MB") + " §7(total: §e" + totalMem + " MB§7, free: §e" + freeMem + " MB§7)");
        boolean hasNative = false;
        try { Class.forName("ravex.benchmark.BenchmarkBridge"); hasNative = true; } catch (ClassNotFoundException ignored) {}
        if (hasNative && args.length > 1 && args[1].equals("native")) {
            CmdReg.print("§7Running native C++ benchmarks...");
            try {
                Class<?> bridge = Class.forName("ravex.benchmark.BenchmarkBridge");
                java.lang.reflect.Method cpu = bridge.getMethod("runCPUBenchmark");
                java.lang.reflect.Method mem = bridge.getMethod("runMemoryBenchmark");
                String cpuRes = (String) cpu.invoke(null);
                String memRes = (String) mem.invoke(null);
                CmdReg.print(" §7" + cpuRes);
                CmdReg.print(" §7" + memRes);
            } catch (Exception e) {
                CmdReg.print("§c[RaveX] Native benchmark error: §e" + e.getMessage());
            }
        } else {
            CmdReg.print("§7Running Java CPU benchmark...");
            long start = System.nanoTime();
            int iterations = 2000000;
            double pi = 0;
            for (int i = 1; i <= iterations; i++) pi += (i % 2 == 1 ? 1.0 : -1.0) / (2 * i - 1);
            pi *= 4;
            long elapsed = System.nanoTime() - start;
            double ms = elapsed / 1_000_000.0;
            CmdReg.print(" §7π calc (2M iter): §e" + String.format("%.2f", ms) + " ms §7(result: §e" + String.format("%.6f", pi) + "§7)");
            CmdReg.print("§7Running Java memory benchmark...");
            int allocSize = 1024 * 1024;
            int blocks = Math.min(100, (int) (freeMem / 2));
            start = System.nanoTime();
            java.util.ArrayList<byte[]> list = new java.util.ArrayList<>();
            for (int i = 0; i < blocks; i++) list.add(new byte[allocSize]);
            for (byte[] b : list) java.util.Arrays.fill(b, (byte) 0xFF);
            list.clear();
            elapsed = System.nanoTime() - start;
            ms = elapsed / 1_000_000.0;
            CmdReg.print(" §7alloc/write/free " + blocks + " MB: §e" + String.format("%.2f", ms) + " ms");
            if (hasNative) CmdReg.print("§7Tip: add §e.native §7to use C++ benchmarks");
        }
    }
}
