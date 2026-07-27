package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;


import ravex.utility.misc.MobUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "AntiBot", category = "Combat")
public class AntiBot {
    @Parameter(name = "OnlyWithKillAura")
    public boolean onlyOnKillAura = false;
    @Parameter(name = "OnlyWithTrigger")
    public boolean onlyOnTrigger = false;
    @Parameter(name = "RemoveInvisible")
    public boolean removeInvisible = true;
    @Parameter(name = "PingCheck")
    public boolean checkPing = true;
    @Parameter(name = "NameCheck")
    public boolean checkName = true;
    @Parameter(name = "MovementCheck")
    public boolean checkMovement = true;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_antibot");
    static {
        NATIVE.load();
    }
    private final List<net.minecraft.world.entity.Entity> botList = new ArrayList<>();
    private long lastCleanup = 0;

    public boolean isBot(net.minecraft.world.entity.Entity entity) {
        return botList.contains(entity);
    }
    public boolean shouldProtectTarget() {
        if (onlyOnKillAura && !Modules.enabled(KillAura.class)) return false;
        if (onlyOnTrigger && !Modules.enabled(Trigger.class)) return false;
        return true;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!shouldProtectTarget()) return;
        long now = System.currentTimeMillis();
        if (now - lastCleanup < 500) return;
        lastCleanup = now;
        List<net.minecraft.world.entity.Entity> newBots = new ArrayList<>();
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player) continue;
            if (!MobUtility.isPlayer(MobUtility.asLivingEntity(e)) || !e.isAlive()) continue;
            net.minecraft.world.entity.player.Player p = (net.minecraft.world.entity.player.Player) e;
            boolean suspect = false;
            if (removeInvisible && p.isInvisible()) {
                suspect = true;
            }
            if (NATIVE.isLoaded()) {
                double[] result = nativeAnalyze(
                    p.getName().getString(),
                    p.tickCount,
                    p.getX(), p.getY(), p.getZ(),
                    p.getDeltaMovement().x, p.getDeltaMovement().y, p.getDeltaMovement().z,
                    mc.player.distanceTo(p),
                    checkPing,
                    checkName,
                    checkMovement
                );
                if (result != null && result.length > 0 && result[0] > 0.5) {
                    suspect = true;
                }
            } else {
                if (checkName && isSuspiciousName(p.getName().getString())) {
                    suspect = true;
                }
                if (checkPing) {
                    try {
                        var conn = mc.getConnection();
                        if (conn != null) {
                            var info = conn.getPlayerInfo(p.getUUID());
                            if (info != null && info.getLatency() == 0) suspect = true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (checkMovement) {
                    double dx = p.getX() - p.xo;
                    double dz = p.getZ() - p.zo;
                    if (Math.abs(dx) < 0.001 && Math.abs(dz) < 0.001 && p.tickCount > 40) {
                        suspect = true;
                    }
                }
            }
            if (suspect) newBots.add(e);
        }
        botList.clear();
        botList.addAll(newBots);
    }
    private boolean isSuspiciousName(String name) {
        String lower = name.toLowerCase();
        String[] patterns = {"bot", "npc", "entity", "test", "dummy", "npc_", "bot_"};
        for (String p : patterns) {
            if (lower.contains(p)) return true;
        }
        if (name.matches("^\\d+$")) return true;
        if (name.length() > 24) return true;
        return false;
    }
    private static native double[] nativeAnalyze(
        String name, int ticks, double x, double y, double z,
        double mx, double my, double mz, double dist,
        boolean pingCheck, boolean nameCheck, boolean moveCheck
    );




}