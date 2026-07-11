package ravex.modules.combat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ravex.modules.Module;
import ravex.utility.misc.MobUtility;
import ravex.parameter.BooleanParameter;
import ravex.utility.nativelib.NativeLibrary;
import java.util.ArrayList;
import java.util.List;
import ravex.manager.ModuleManager;
public class AntiBot extends Module {
    public final BooleanParameter onlyOnKillAura = new BooleanParameter("OnlyWithKillAura", false);
    public final BooleanParameter onlyOnTrigger = new BooleanParameter("OnlyWithTrigger", false);
    public final BooleanParameter removeInvisible = new BooleanParameter("RemoveInvisible", true);
    public final BooleanParameter checkPing = new BooleanParameter("PingCheck", true);
    public final BooleanParameter checkName = new BooleanParameter("NameCheck", true);
    public final BooleanParameter checkMovement = new BooleanParameter("MovementCheck", true);
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_antibot");
    static {
        NATIVE.load();
    }
    private final List<Entity> botList = new ArrayList<>();
    private long lastCleanup = 0;

    public boolean isBot(Entity entity) {
        return botList.contains(entity);
    }
    public boolean shouldProtectTarget() {
        if (onlyOnKillAura.getValue() && !ModuleManager.get(ravex.modules.combat.KillAura.class).getEnabled()) return false;
        if (onlyOnTrigger.getValue() && !ModuleManager.get(ravex.modules.combat.Trigger.class).getEnabled()) return false;
        return true;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!shouldProtectTarget()) return;
        long now = System.currentTimeMillis();
        if (now - lastCleanup < 500) return;
        lastCleanup = now;
        List<Entity> newBots = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player) continue;
            if (!MobUtility.isPlayer(MobUtility.asLivingEntity(e)) || !e.isAlive()) continue;
            Player p = (Player) e;
            boolean suspect = false;
            if (removeInvisible.getValue() && p.isInvisible()) {
                suspect = true;
            }
            if (NATIVE.isLoaded()) {
                double[] result = nativeAnalyze(
                    p.getName().getString(),
                    p.tickCount,
                    p.getX(), p.getY(), p.getZ(),
                    p.getDeltaMovement().x, p.getDeltaMovement().y, p.getDeltaMovement().z,
                    mc.player.distanceTo(p),
                    checkPing.getValue(),
                    checkName.getValue(),
                    checkMovement.getValue()
                );
                if (result != null && result.length > 0 && result[0] > 0.5) {
                    suspect = true;
                }
            } else {
                if (checkName.getValue() && isSuspiciousName(p.getName().getString())) {
                    suspect = true;
                }
                if (checkPing.getValue()) {
                    try {
                        var conn = mc.getConnection();
                        if (conn != null) {
                            var info = conn.getPlayerInfo(p.getUUID());
                            if (info != null && info.getLatency() == 0) suspect = true;
                        }
                    } catch (Throwable ignored) {}
                }
                if (checkMovement.getValue()) {
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
    public static boolean maybeEnabled() {
        return maybeEnabled(AntiBot.class);
    }
    public static AntiBot itz() {
        return ModuleManager.get(AntiBot.class);
    }

}