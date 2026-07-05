package ravex.modules.combat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
import java.util.List;
public class KillAura extends Module {
    public static final KillAura INSTANCE = new KillAura();
    public final NumberParameter range = new NumberParameter("Range", 4.2, 3.0, 6.0, 0.1);
    public final NumberParameter cooldownThreshold = new NumberParameter("Cooldown", 0.9, 0.0, 1.0, 0.05);
    public final NumberParameter switchDelay = new NumberParameter("Switch Delay", 100, 0, 1000, 10);
    public final NumberParameter rotationSpeed = new NumberParameter("Rotate Speed", 180, 10, 180, 5);
    public final NumberParameter rotationRandomize = new NumberParameter("Rotate Randomize", 0.0, 0.0, 3.0, 0.1);
    public final NumberParameter cps = new NumberParameter("CPS", 10, 1, 20, 1);
    public final NumberParameter wallRange = new NumberParameter("Wall Range", 3.0, 1.0, 6.0, 0.1);
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter monsters = new BooleanParameter("Monsters", true);
    public final BooleanParameter passives = new BooleanParameter("Passives", false);
    public final BooleanParameter invisibles = new BooleanParameter("Invisibles", true);
    public final BooleanParameter throughWalls = new BooleanParameter("Through Walls", true);
    public final ModeParameter mode = new ModeParameter("Mode", "Single", List.of("Single", "Switch"));
    public final ModeParameter targetMode = new ModeParameter("Target", "Closest",
            List.of("Closest", "Lowest HP", "Farthest", "Most Aura", "Least Aura"));
    public final ModeParameter rotate = new ModeParameter("Rotate", "Silent",
            List.of("Silent", "Normal", "None"));
    public final ModeParameter swingMode = new ModeParameter("Swing", "Client",
            List.of("Client", "Server", "Off"));
    public static float silentYaw = 0;
    public static float silentPitch = 0;
    private static boolean hasSilentRotations = false;
    private LivingEntity currentTarget = null;
    private long lastAttackTime = 0;
    private long lastSwitchTime = 0;
    private int targetIndex = 0;
    private float prevYaw = 0;
    private float prevPitch = 0;

    public static boolean hasSilentRotations() {
        return hasSilentRotations;
    }
    @Override
    protected void onDisable() {
        hasSilentRotations = false;
        currentTarget = null;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            hasSilentRotations = false;
            return;
        }
        hasSilentRotations = false;
        String rotMode = rotate.getValue();
        boolean doRotate = !rotMode.equals("None");
        long now = System.currentTimeMillis();
        long attackInterval = 1000L / (long) cps.getValue().doubleValue();
        if (mode.getValue().equals("Switch")) {
            handleSwitchMode(mc, now, attackInterval, doRotate, rotMode);
        } else {
            handleSingleMode(mc, now, attackInterval, doRotate, rotMode);
        }
    }
    private void handleSingleMode(Minecraft mc, long now, long attackInterval, boolean doRotate, String rotMode) {
        LivingEntity target = findTarget(mc);
        if (target == null) {
            currentTarget = null;
            return;
        }
        currentTarget = target;
        if (now - lastAttackTime < attackInterval) return;
        if (mc.player.getAttackStrengthScale(0.0f) < cooldownThreshold.getValue().floatValue()) return;
        if (doRotate) {
            float[] angles = calculateAngles(mc, target.position());
            float yaw = angles[0];
            float pitch = angles[1];
            pitch = RotationUtility.clampPitch(pitch);
            if (rotMode.equals("Silent")) {
                silentYaw = yaw;
                silentPitch = pitch;
                hasSilentRotations = true;
            } else {
                mc.player.setYRot(yaw);
                mc.player.setXRot(pitch);
            }
        }
        attack(mc, target);
        lastAttackTime = now;
    }
    private void handleSwitchMode(Minecraft mc, long now, long attackInterval, boolean doRotate, String rotMode) {
        List<LivingEntity> targets = findTargets(mc);
        if (targets.isEmpty()) {
            currentTarget = null;
            return;
        }
        if (now - lastSwitchTime < switchDelay.getValue().longValue()) {
            if (currentTarget != null && currentTarget.isAlive() && mc.player.distanceTo(currentTarget) <= range.getValue()) {
                if (now - lastAttackTime < attackInterval) return;
                if (mc.player.getAttackStrengthScale(0.0f) < cooldownThreshold.getValue().floatValue()) return;
                if (doRotate) {
                    float[] angles = calculateAngles(mc, currentTarget.position());
                    if (rotMode.equals("Silent")) {
                        silentYaw = angles[0];
                        silentPitch = RotationUtility.clampPitch(angles[1]);
                        hasSilentRotations = true;
                    } else {
                        mc.player.setYRot(angles[0]);
                        mc.player.setXRot(RotationUtility.clampPitch(angles[1]));
                    }
                }
                attack(mc, currentTarget);
                lastAttackTime = now;
            }
            return;
        }
        if (targetIndex >= targets.size()) targetIndex = 0;
        currentTarget = targets.get(targetIndex);
        targetIndex = (targetIndex + 1) % targets.size();
        if (now - lastAttackTime < attackInterval) return;
        if (mc.player.getAttackStrengthScale(0.0f) < cooldownThreshold.getValue().floatValue()) return;
        if (doRotate) {
            float[] angles = calculateAngles(mc, currentTarget.position());
            if (rotMode.equals("Silent")) {
                silentYaw = angles[0];
                silentPitch = Math.max(-90, Math.min(90, angles[1]));
                hasSilentRotations = true;
            } else {
                mc.player.setYRot(angles[0]);
                mc.player.setXRot(Math.max(-90, Math.min(90, angles[1])));
            }
        }
        attack(mc, currentTarget);
        lastAttackTime = now;
        lastSwitchTime = now;
    }
    private LivingEntity findTarget(Minecraft mc) {
        List<LivingEntity> targets = findTargets(mc);
        if (targets.isEmpty()) return null;
        return targets.get(0);
    }
    private List<LivingEntity> findTargets(Minecraft mc) {
        java.util.ArrayList<LivingEntity> list = new java.util.ArrayList<>();
        double maxDist = range.getValue();
        double wallDist = throughWalls.getValue() ? wallRange.getValue() : maxDist;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le == mc.player) continue;
            if (le.isDeadOrDying()) continue;
            if (!invisibles.getValue() && le.isInvisible()) continue;
            if (le instanceof net.minecraft.world.entity.decoration.ArmorStand) continue;
            if (!players.getValue() && le instanceof Player) continue;
            if (!monsters.getValue() && (le instanceof Monster || le instanceof EnderDragon || le instanceof WitherBoss)) continue;
            if (!passives.getValue() && isPassive(le)) continue;
            double dist = mc.player.distanceTo(le);
            if (dist > maxDist) continue;
            if (!throughWalls.getValue() && !mc.player.hasLineOfSight(le)) continue;
            if (throughWalls.getValue() && !mc.player.hasLineOfSight(le) && dist > wallDist) continue;
            if (AntiBot.INSTANCE.getEnabled() && AntiBot.INSTANCE.isBot(e)) continue;
            list.add(le);
        }
        String mode = targetMode.getValue();
        list.sort((a, b) -> {
            double ma = switch (mode) {
                case "Closest" -> mc.player.distanceTo(a);
                case "Lowest HP" -> a.getHealth();
                case "Farthest" -> -mc.player.distanceTo(a);
                case "Most Aura" -> -(mc.player.getArmorValue() + a.getArmorValue());
                case "Least Aura" -> mc.player.getArmorValue() + a.getArmorValue();
                default -> mc.player.distanceTo(a);
            };
            double mb = switch (mode) {
                case "Closest" -> mc.player.distanceTo(b);
                case "Lowest HP" -> b.getHealth();
                case "Farthest" -> -mc.player.distanceTo(b);
                case "Most Aura" -> -(mc.player.getArmorValue() + b.getArmorValue());
                case "Least Aura" -> mc.player.getArmorValue() + b.getArmorValue();
                default -> mc.player.distanceTo(b);
            };
            return Double.compare(ma, mb);
        });
        return list;
    }
    private boolean isPassive(LivingEntity e) {
        if (e instanceof Player || e instanceof Monster || e instanceof EnderDragon || e instanceof WitherBoss) return false;
        if (e instanceof net.minecraft.world.entity.animal.Animal) return true;
        if (e instanceof net.minecraft.world.entity.npc.villager.AbstractVillager) return true;
        if (e instanceof net.minecraft.world.entity.ambient.AmbientCreature) return true;
        return false;
    }
    private void attack(Minecraft mc, LivingEntity target) {
        mc.gameMode.attack(mc.player, target);
        String sMode = swingMode.getValue();
        if (sMode.equals("Client")) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        } else if (sMode.equals("Server")) {
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        }
    }
    private float[] calculateAngles(Minecraft mc, Vec3 targetPos) {
        float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), targetPos.add(0, 0.25, 0));
        float yaw = angles[0], pitch = angles[1];
        double r = rotationRandomize.getValue();
        if (r > 0.01) { float[] rnd = AimUtility.randomize(yaw, pitch, r); yaw = rnd[0]; pitch = rnd[1]; }
        float speed = rotationSpeed.getValue().floatValue();
        if (speed < 180.0f) {
            float[] limited = AimUtility.limitAngles(prevYaw, yaw, prevPitch, pitch, speed / 20f);
            yaw = limited[0]; pitch = limited[1];
        }
        prevYaw = yaw; prevPitch = pitch;
        return new float[]{yaw, pitch};
    }
}
