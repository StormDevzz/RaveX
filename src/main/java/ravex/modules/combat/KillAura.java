package ravex.modules.combat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
<<<<<<< HEAD
import net.minecraft.world.phys.Vec3;
import ravex.modules.Module;
import ravex.utility.misc.MobUtility;
=======
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Module;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
<<<<<<< HEAD
import ravex.utility.player.rotation.SilentRotation;
import java.util.List;
import ravex.manager.ModuleManager;
public class KillAura extends Module {
=======
import java.util.List;
public class KillAura extends Module {
    public static final KillAura INSTANCE = new KillAura();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter range = new NumberParameter("Range", 4.2, 3.0, 6.0, 0.1);
    public final NumberParameter cooldownThreshold = new NumberParameter("Cooldown", 0.9, 0.0, 1.0, 0.05);
    public final NumberParameter switchDelay = new NumberParameter("SwitchDelay", 100, 0, 1000, 10);
    public final NumberParameter rotationSpeed = new NumberParameter("RotatSpeed", 180, 10, 180, 5);
    public final NumberParameter rotationRandomize = new NumberParameter("RotateRandomize", 0.0, 0.0, 3.0, 0.1);
    public final NumberParameter cps = new NumberParameter("CPS", 10, 1, 20, 1);
    public final NumberParameter wallRange = new NumberParameter("WallRange", 3.0, 1.0, 6.0, 0.1);
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter monsters = new BooleanParameter("Monsters", true);
    public final BooleanParameter passives = new BooleanParameter("Passives", false);
    public final BooleanParameter invisibles = new BooleanParameter("Invisibles", true);
    public final BooleanParameter throughWalls = new BooleanParameter("ThroughWalls", true);
    public final ModeParameter mode = new ModeParameter("Mode", "Single", List.of("Single", "Switch"));
    public final ModeParameter targetMode = new ModeParameter("Target", "Closest",
            List.of("Closest", "LowestHP", "Farthest", "MostAura", "LeastAura"));
    public final ModeParameter rotate = new ModeParameter("Rotate", "Silent",
            List.of("Silent", "Normal", "None"));
    public final ModeParameter swingMode = new ModeParameter("Swing", "Client",
            List.of("Client", "Server", "Off"));
<<<<<<< HEAD
    public static final SilentRotation silentRotation = new SilentRotation();
=======
    public static float silentYaw = 0;
    public static float silentPitch = 0;
    private static boolean hasSilentRotations = false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private LivingEntity currentTarget = null;
    private long lastAttackTime = 0;
    private long lastSwitchTime = 0;
    private int targetIndex = 0;
    private float prevYaw = 0;
    private float prevPitch = 0;

<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(KillAura.class);
    }
    public static KillAura itz() {
        return ModuleManager.get(KillAura.class);
    }
    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }
    @Override
    protected void onDisable() {
        silentRotation.hasRotation = false;
=======
    public static boolean hasSilentRotations() {
        return hasSilentRotations;
    }
    @Override
    protected void onDisable() {
        hasSilentRotations = false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        currentTarget = null;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
<<<<<<< HEAD
            silentRotation.hasRotation = false;
            return;
        }
        silentRotation.hasRotation = false;
=======
            hasSilentRotations = false;
            return;
        }
        hasSilentRotations = false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
                silentRotation.set(yaw, pitch);
=======
                silentYaw = yaw;
                silentPitch = pitch;
                hasSilentRotations = true;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
                        silentRotation.set(angles[0], RotationUtility.clampPitch(angles[1]));
=======
                        silentYaw = angles[0];
                        silentPitch = RotationUtility.clampPitch(angles[1]);
                        hasSilentRotations = true;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
                silentRotation.set(angles[0], Math.max(-90, Math.min(90, angles[1])));
=======
                silentYaw = angles[0];
                silentPitch = Math.max(-90, Math.min(90, angles[1]));
                hasSilentRotations = true;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
            if (MobUtility.isSelf(le)) continue;
            if (MobUtility.isDead(le)) continue;
            if (!invisibles.getValue() && le.isInvisible()) continue;
            if (MobUtility.isArmorStand(le)) continue;
            if (!players.getValue() && MobUtility.isPlayer(le)) continue;
            if (!monsters.getValue() && MobUtility.isHostile(le)) continue;
            if (!passives.getValue() && MobUtility.isPassive(le)) continue;
            double dist = MobUtility.distanceToPlayer(le);
            if (dist > maxDist) continue;
            if (!throughWalls.getValue() && !mc.player.hasLineOfSight(le)) continue;
            if (throughWalls.getValue() && !mc.player.hasLineOfSight(le) && dist > wallDist) continue;
            if (ModuleManager.get(ravex.modules.combat.AntiBot.class).getEnabled() && ModuleManager.get(ravex.modules.combat.AntiBot.class).isBot(e)) continue;
=======
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
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            list.add(le);
        }
        String mode = targetMode.getValue();
        list.sort((a, b) -> {
            double ma = switch (mode) {
<<<<<<< HEAD
                case "Closest" -> MobUtility.distanceToPlayer(a);
                case "LowestHP" -> MobUtility.getHealth(a);
                case "Farthest" -> -MobUtility.distanceToPlayer(a);
                case "MostAura" -> -(mc.player.getArmorValue() + a.getArmorValue());
                case "LeastAura" -> mc.player.getArmorValue() + a.getArmorValue();
                default -> MobUtility.distanceToPlayer(a);
            };
            double mb = switch (mode) {
                case "Closest" -> MobUtility.distanceToPlayer(b);
                case "LowestHP" -> MobUtility.getHealth(b);
                case "Farthest" -> -MobUtility.distanceToPlayer(b);
                case "MostAura" -> -(mc.player.getArmorValue() + b.getArmorValue());
                case "LeastAura" -> mc.player.getArmorValue() + b.getArmorValue();
                default -> MobUtility.distanceToPlayer(b);
=======
                case "Closest" -> mc.player.distanceTo(a);
                case "LowestHP" -> a.getHealth();
                case "Farthest" -> -mc.player.distanceTo(a);
                case "MostAura" -> -(mc.player.getArmorValue() + a.getArmorValue());
                case "LeastAura" -> mc.player.getArmorValue() + a.getArmorValue();
                default -> mc.player.distanceTo(a);
            };
            double mb = switch (mode) {
                case "Closest" -> mc.player.distanceTo(b);
                case "LowestHP" -> b.getHealth();
                case "Farthest" -> -mc.player.distanceTo(b);
                case "MostAura" -> -(mc.player.getArmorValue() + b.getArmorValue());
                case "LeastAura" -> mc.player.getArmorValue() + b.getArmorValue();
                default -> mc.player.distanceTo(b);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            };
            return Double.compare(ma, mb);
        });
        return list;
    }
<<<<<<< HEAD
    private void attack(Minecraft mc, LivingEntity target) {
        MobUtility.attack(mc, target);
        String sMode = swingMode.getValue();
        if (sMode.equals("Client")) {
            MobUtility.swingHand(mc);
=======
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
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
