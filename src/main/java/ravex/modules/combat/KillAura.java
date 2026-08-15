package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.modules.Modules;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.PotionUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.movement.MoveUtility;
import ravex.utility.network.NetworkUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.PlayerUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;

@Module(name = "KillAura", category = "Combat")
public class KillAura {
    @Parameter(name = "Mode", modes = {"Tracker", "Snap", "HvH"})
    public String mode = "Tracker";

    @Parameter(name = "Range", min = 2.0, max = 6.0, step = 0.1)
    public double range = 3.0;
    @Parameter(name = "Attack Cooldown", min = 0.0, max = 1.0, step = 0.05)
    public double cooldownThreshold = 1.0;

    @Parameter(name = "Target ESP")
    public boolean targetEsp = true;
    @Parameter(name = "ESP Mode", modes = {"RaveXV1", "Circle"}, visible = "targetEsp")
    public String targetEspMode = "Circle";
    @Parameter(name = "ESP Color", color = true, visible = "targetEsp")
    public int targetEspColor = 0xFF00FFFF;

    @Parameter(name = "Targets", options = {"Players", "Monsters", "Passives", "Invisibles"})
    public List<String> targets = new ArrayList<>(List.of("Players", "Monsters"));

    @Parameter(name = "ThroughWalls")
    public boolean throughWalls = true;
    @Parameter(name = "SmartCrits")
    public boolean smartCrits = true;
    @Parameter(name = "Sprint", modes = {"Normal", "Legit", "HvH"})
    public String sprintMode = "Normal";

    @Parameter(name = "AutoWeapon")
    public boolean autoWeapon = false;
    @Parameter(name = "SwapMode", modes = {"Normal", "Silent", "None"}, visible = "autoWeapon")
    public String swapMode = "Normal";
    @Parameter(name = "SwordsOnly", visible = "autoWeapon")
    public boolean swordsOnly = false;

    @Parameter(name = "KeepSprint")
    public boolean keepSprint = false;
    @Parameter(name = "KeepSprintSpeed", min = 0.0, max = 100.0, step = 5.0, visible = "keepSprint")
    public double keepSprintSpeed = 100.0;

    public static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    private net.minecraft.world.entity.LivingEntity currentTarget = null;
    private long lastAttackTime = 0;
    private float prevYaw = 0;
    private float prevPitch = 0;

    private int sprintResetTicks = 0;

    private static float scanProgress = 0f;
    private static float prevScanProgress = 0f;
    private static float slowRotation = 0f;
    private static float prevSlowRotation = 0f;
    private static float circleStep = 0f;
    private static float prevCircleStep = 0f;

    public net.minecraft.world.entity.LivingEntity getCurrentTarget() {
        return currentTarget;
    }





    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }
    public void onDisable() {
        silentRotation.hasRotation = false;
        currentTarget = null;
        prevYaw = 0;
        prevPitch = 0;
        sprintResetTicks = 0;
    }

    public static void onPreTick() {
        KillAura ka = Modules.get(KillAura.class);
        if (ka == null || !Modules.enabled(KillAura.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;

        switch (ka.sprintMode) {
            case "Legit" -> {
                if (ka.sprintResetTicks > 0) {
                    ka.sprintResetTicks--;
                    mc.getPlayer().setSprinting(false);
                } else {
                    float cooldown = mc.getPlayer().getAttackStrengthScale(0.5f);
                    if (mc.getPlayer().input.hasForwardImpulse()
                            && !PlayerUtility.isUsingItem(mc.getPlayer())
                            && !PlayerUtility.isSneaking(mc.getPlayer())
                            && cooldown >= 0.8f) {
                        mc.getPlayer().setSprinting(true);
                    }
                }
            }
            case "HvH" -> {
                if (!PlayerUtility.isUsingItem(mc.getPlayer()) && !PlayerUtility.isSneaking(mc.getPlayer()))
                    mc.getPlayer().setSprinting(true);
            }
        }

        var target = ka.currentTarget;
        if (target == null || EntityUtility.isDead(target)) return;

        var eyePos = mc.getPlayer().getEyePosition();
        var aimPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
        float[] freshAngles = RotationUtility.anglesTo(eyePos, aimPos);

        // гцд коррекция к дельте
        float rawFreshYaw   = freshAngles[0];
        float rawFreshPitch = RotationUtility.clampPitch(freshAngles[1]);
        float gcdPre = RotationUtility.getGCD();
        float ppYaw   = mc.getPlayer().getYRot();
        float ppPitch = mc.getPlayer().getXRot();
        float dYaw   = RotationUtility.normalizeYaw(rawFreshYaw - ppYaw);
        float dPitch = rawFreshPitch - ppPitch;
        if (gcdPre > 0) {
            dYaw   = Math.round(dYaw   / gcdPre) * gcdPre;
            dPitch = Math.round(dPitch / gcdPre) * gcdPre;
        }
        float freshYaw   = ppYaw   + dYaw;
        float freshPitch = ppPitch + dPitch;
        silentRotation.set(freshYaw, freshPitch);

        {
            net.minecraft.world.phys.AABB pa = mc.getPlayer().getBoundingBox();
            net.minecraft.world.phys.AABB ea = target.getBoundingBox();
            double dx = Math.max(0, Math.max(pa.minX - ea.maxX, ea.minX - pa.maxX));
            double dy = Math.max(0, Math.max(pa.minY - ea.maxY, ea.minY - pa.maxY));
            double dz = Math.max(0, Math.max(pa.minZ - ea.maxZ, ea.minZ - pa.maxZ));
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            var mobVel = target.getDeltaMovement();
            double mobSpeed = Math.sqrt(mobVel.x * mobVel.x + mobVel.z * mobVel.z);
            double buffer = 0.15 + Math.min(mobSpeed * 1.5, 0.1);
            if (dist > ka.range - buffer) return;
        }

        if (ka.smartCrits && !mc.getPlayer().onGround()) {
            double velY = mc.getPlayer().getDeltaMovement().y;
            if (velY > -0.08) return;
        }

        float scale = mc.getPlayer().getAttackStrengthScale(0.5f);
        if (scale < ka.cooldownThreshold) return;

        long now = System.currentTimeMillis();
        if (now - ka.lastAttackTime < 50) return;

        if (ka.mode.equals("Tracker")) {
            var targetAimPos = target.position().add(0, target.getBbHeight() * 0.45, 0);
            float[] desired = RotationUtility.anglesTo(eyePos, targetAimPos);
            float yawDiff   = Math.abs(RotationUtility.normalizeYaw(freshYaw - desired[0]));
            float pitchDiff = Math.abs(freshPitch - desired[1]);
            if (yawDiff > 18.0f || pitchDiff > 20.0f) return;
        }

        // легит/ сброс спринта перед ударом
        if (ka.sprintMode.equals("Legit") && PlayerUtility.isSprinting(mc.getPlayer())) {
            mc.getPlayer().setSprinting(false);
            ka.sprintResetTicks = 3;
            return; // пропуск тика
        }

        ka.attack(mc, target);
        ka.lastAttackTime = now;

        if (ka.sprintMode.equals("Legit")) {
            ka.sprintResetTicks = 2;
        }

        if (ka.keepSprint) {
            if (mc.getPlayer().hurtTime > 0) {
                mc.getPlayer().setSprinting(true);
                double multiplier = ka.keepSprintSpeed / 100.0;
                if (multiplier < 1.0) {
                    var vel = mc.getPlayer().getDeltaMovement();
                    MoveUtility.setMotion(vel.x * multiplier, vel.y, vel.z * multiplier);
                }
            }
            if (PotionUtility.hasBlindness(mc.getPlayer()) && PlayerUtility.isSprinting(mc.getPlayer())) {
                mc.getPlayer().setSprinting(false);
            }
        }
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) {
            silentRotation.hasRotation = false;
            return;
        }
        silentRotation.hasRotation = false;

        var target = findTarget(mc);
        if (target == null) {
            currentTarget = null;
            prevYaw = 0;
            prevPitch = 0;
            return;
        }
        currentTarget = target;

        float[] angles = calculateAngles(mc, target);

        // гцд коррекция
        float rawYaw = angles[0];
        float rawPitch = RotationUtility.clampPitch(angles[1]);
        float gcd = RotationUtility.getGCD();
        float prevPlayerYaw   = mc.getPlayer().getYRot();
        float prevPlayerPitch = mc.getPlayer().getXRot();
        float deltaYaw   = RotationUtility.normalizeYaw(rawYaw - prevPlayerYaw);
        float deltaPitch = rawPitch - prevPlayerPitch;
        if (gcd > 0) {
            deltaYaw   = Math.round(deltaYaw   / gcd) * gcd;
            deltaPitch = Math.round(deltaPitch / gcd) * gcd;
        }
        float yaw   = prevPlayerYaw   + deltaYaw;
        float pitch = prevPlayerPitch + deltaPitch;

        silentRotation.set(yaw, pitch);

        // обновляем голову/тело, не трогаем O поля
        mc.getPlayer().yHeadRot = yaw;
        mc.getPlayer().yBodyRot = yaw;

        prevScanProgress = scanProgress;
        scanProgress += 0.02f;
        if (scanProgress >= 2.0f) {
            scanProgress -= 2.0f;
            prevScanProgress -= 2.0f;
        }

        prevSlowRotation = slowRotation;
        slowRotation += 2.0f;
        if (slowRotation >= 360.0f) {
            slowRotation -= 360.0f;
            prevSlowRotation -= 360.0f;
        }

        prevCircleStep = circleStep;
        circleStep += 0.15f;
    }

    private net.minecraft.world.entity.LivingEntity findTarget(MinecraftWrapper mc) {

        final double BASE_BUFFER = 0.15;
        net.minecraft.world.entity.LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (var e : mc.getLevel().entitiesForRendering()) {
            if (!(e instanceof net.minecraft.world.entity.LivingEntity le)) continue;
            if (EntityUtility.isSelf(le)) continue;
            if (EntityUtility.isDead(le)) continue;
            if (!targets.contains("Invisibles") && le.isInvisible()) continue;
            if (EntityUtility.isArmorStand(le)) continue;
            if (!targets.contains("Players") && EntityUtility.isPlayer(le)) continue;
            if (!targets.contains("Monsters") && EntityUtility.isHostile(le)) continue;
            if (!targets.contains("Passives") && EntityUtility.isPassive(le)) continue;

            net.minecraft.world.phys.AABB pa = mc.getPlayer().getBoundingBox();
            net.minecraft.world.phys.AABB ea = le.getBoundingBox();
            double dx = Math.max(0, Math.max(pa.minX - ea.maxX, ea.minX - pa.maxX));
            double dy = Math.max(0, Math.max(pa.minY - ea.maxY, ea.minY - pa.maxY));
            double dz = Math.max(0, Math.max(pa.minZ - ea.maxZ, ea.minZ - pa.maxZ));
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            var mobVel = le.getDeltaMovement();
            double mobSpeed = Math.sqrt(mobVel.x * mobVel.x + mobVel.z * mobVel.z);
            double buffer = BASE_BUFFER + Math.min(mobSpeed * 1.5, 0.1);

            if (dist > range - buffer) continue;

            if (!throughWalls && !mc.getPlayer().hasLineOfSight(le)) continue;
            if (Modules.enabled(AntiBot.class)
                    && Modules.get(AntiBot.class).isBot(e)) continue;

            if (dist < closestDist) {
                closestDist = dist;
                closest = le;
            }
        }
        return closest;
    }

    private void attack(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target) {
        if (autoWeapon && !swapMode.equals("None")) {
            int bestSlot = -1;
            double bestDmg = -1.0;
            for (int i = 0; i < 9; i++) {
                var stack = InventoryUtility.getItem(mc.getPlayer(), i);
                if (swordsOnly && !isSword(stack.getItem())) continue;
                double dmg = getWeaponDamage(stack);
                if (dmg > bestDmg) {
                    bestDmg = dmg;
                    bestSlot = i;
                }
            }
            if (bestSlot != -1 && bestSlot != InventoryUtility.getSelectedSlot(mc.getPlayer()) && bestDmg > 1.0) {
                if (swapMode.equals("Silent")) {
                    NetworkUtility.sendSetCarriedItem(bestSlot);
                } else {
                    InventoryUtility.selectSlot(mc.getPlayer(), bestSlot);
                }
            }
        }
        EntityUtility.attack(mc, target);
        EntityUtility.swingHand(mc);
    }

    private boolean isSword(net.minecraft.world.item.Item item) {
        return item == net.minecraft.world.item.Items.WOODEN_SWORD ||
               item == net.minecraft.world.item.Items.STONE_SWORD ||
               item == net.minecraft.world.item.Items.IRON_SWORD ||
               item == net.minecraft.world.item.Items.GOLDEN_SWORD ||
               item == net.minecraft.world.item.Items.DIAMOND_SWORD ||
               item == net.minecraft.world.item.Items.NETHERITE_SWORD;
    }

    private double getWeaponDamage(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        String name = stack.getItem().toString().toLowerCase();
        double dmg = 0.0;
        if (name.contains("netherite_sword")) dmg = 8.0;
        else if (name.contains("diamond_sword")) dmg = 7.0;
        else if (name.contains("netherite_axe")) dmg = 7.0;
        else if (name.contains("mace")) dmg = 6.5;
        else if (name.contains("diamond_axe")) dmg = 6.0;
        else if (name.contains("iron_sword")) dmg = 6.0;
        else if (name.contains("iron_axe")) dmg = 5.0;
        else if (name.contains("stone_sword")) dmg = 5.0;
        else if (name.contains("stone_axe")) dmg = 4.0;
        else if (name.contains("golden_sword") || name.contains("wooden_sword")) dmg = 4.0;
        return dmg;
    }

    private float[] calculateAngles(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target) {
        if (prevYaw == 0f && prevPitch == 0f) {
            prevYaw = mc.getPlayer().getYRot();
            prevPitch = mc.getPlayer().getXRot();
        }

        float yaw, pitch;
        if (mode.equals("Tracker")) {
            var stomachPos = target.position().add(0, target.getBbHeight() * 0.45, 0);
            float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), stomachPos);
            float targetYaw = angles[0];
            float targetPitch = angles[1];

            float diffYaw = RotationUtility.normalizeYaw(targetYaw - prevYaw);
            float diffPitch = targetPitch - prevPitch;
            float absDiffYaw = Math.abs(diffYaw);

            float MAX_YAW_SPEED   = 30.0f;
            float MAX_PITCH_SPEED = 20.0f;

            float tYaw   = Math.min(absDiffYaw / 180.0f, 1.0f);
            float tPitch = Math.min(Math.abs(diffPitch) / 90.0f, 1.0f);
            float speedYaw   = MAX_YAW_SPEED   * (2.0f * tYaw   - tYaw   * tYaw);
            float speedPitch = MAX_PITCH_SPEED * (2.0f * tPitch - tPitch * tPitch);

            speedYaw   = Math.max(speedYaw,   0.5f);
            speedPitch = Math.max(speedPitch, 0.3f);

            float jitterYaw   = (float) (Math.random() * 0.6 - 0.3);
            float jitterPitch = (float) (Math.random() * 0.4 - 0.2);

            float stepYaw   = Math.max(-speedYaw,   Math.min(speedYaw,   diffYaw))   + jitterYaw;
            float stepPitch = Math.max(-speedPitch, Math.min(speedPitch, diffPitch)) + jitterPitch;

            yaw = prevYaw + stepYaw;
            pitch = prevPitch + stepPitch;
        } else if (mode.equals("Snap")) {
            var chestPos = target.position().add(0, target.getBbHeight() * 0.65, 0);
            float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), chestPos);
            yaw = angles[0];
            pitch = angles[1];
        } else {
            var headPos = target.position().add(0, target.getBbHeight() * 0.9, 0);
            float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), headPos);
            yaw = angles[0];
            pitch = angles[1];
        }

        prevYaw = yaw;
        prevPitch = pitch;
        return new float[]{yaw, pitch};
    }

    public void render(Matrix4f modelViewMatrix, net.minecraft.client.Camera camera, float tickDelta) {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;

        var target = currentTarget;
        if (target == null || EntityUtility.isDead(target)) return;

        if (targetEspMode.equals("RaveXV1")) {
            float progressVal = prevScanProgress + (scanProgress - prevScanProgress) * tickDelta;
            float rotation = prevSlowRotation + (slowRotation - prevSlowRotation) * tickDelta;
            ravex.utility.render.Render3DUtility.renderRaveXESP(
                modelViewMatrix,
                camera,
                target,
                targetEspColor,
                progressVal,
                rotation,
                tickDelta
            );
        } else if (targetEspMode.equals("Circle")) {
            ravex.utility.render.Render3DUtility.renderCircleESP(
                modelViewMatrix,
                camera,
                target,
                targetEspColor,
                circleStep,
                prevCircleStep,
                tickDelta
            );
        }
    }


}