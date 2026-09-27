package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.modules.Modules;
import ravex.utility.misc.CombatUtility;
import ravex.utility.misc.GameModeUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.PotionUtility;
import ravex.utility.movement.MoveUtility;
import ravex.modules.movement.Speed;
import ravex.utility.network.NetworkUtility;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.PlayerUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;

@Module(name = "KillAura", category = "Combat")
public class KillAura {
    @Parameter(name = "Mode", modes = {"Tracker", "Snap", "Snap2", "HvH"})
    public String mode = "Snap2";

    @Parameter(name = "Range", min = 2.0, max = 6.0, step = 0.1)
    public double range = 3.0;
    @Parameter(name = "Attack Cooldown", min = 0.0, max = 1.0, step = 0.05)
    public double cooldownThreshold = 0.5;

    @Parameter(name = "Targets", options = {"Players", "Monsters", "Passives", "Invisibles", "Creative"})
    public List<String> targets = new ArrayList<>(List.of("Players", "Monsters"));

    @Parameter(name = "SmartCrits")
    public boolean smartCrits = true;
    @Parameter(name = "OnlyCritz", visible = "smartCrits")
    public boolean onlyCritz = true;
    @Parameter(name = "AutoJump", visible = "smartCrits")
    public boolean autoJump = false;

    @Parameter(name = "Sprint", modes = {"Normal", "Legit", "HvH"})
    public String sprintMode = "Normal";

    @Parameter(name = "AutoWeapon")
    public boolean autoWeapon = false;

    @Parameter(name = "Advanced", group = true)
    public boolean advanced = false;

    @Parameter(name = "MinFall", min = 0.0, max = 1.0, step = 0.05, visible = "advanced")
    public double minFall = 0.0;

    @Parameter(name = "Target ESP", visible = "advanced")
    public boolean targetEsp = true;
    @Parameter(name = "ESP Mode", modes = {"RaveXV1", "Circle", "Square"}, visible = "advanced")
    public String targetEspMode = "Circle";
    @Parameter(name = "ESP Color", color = true, visible = "advanced")
    public int targetEspColor = 0xFF00FFFF;

    @Parameter(name = "ThroughWalls", visible = "advanced")
    public boolean throughWalls = true;

    @Parameter(name = "SwapMode", modes = {"Normal", "Silent", "None"}, visible = "advanced")
    public String swapMode = "Normal";
    @Parameter(name = "SwordsOnly", visible = "advanced")
    public boolean swordsOnly = false;

    @Parameter(name = "FreeLook", visible = "advanced")
    public boolean freeLook = true;
    @Parameter(name = "KeepSprint", visible = "advanced")
    public boolean keepSprint = false;
    @Parameter(name = "KeepSprint Speed", min = 0.0, max = 100.0, step = 5.0, visible = "advanced")
    public double keepSprintSpeed = 100.0;
    @Parameter(name = "FOV", min = 30.0, max = 180.0, step = 5.0, visible = "advanced")
    public double fov = 75.0;
    @Parameter(name = "AttackTiming", modes = {"Pre", "Post"}, visible = "advanced")
    public String attackTiming = "Pre";
    @Parameter(name = "AimRange", min = 0.0, max = 3.0, step = 0.1, visible = "advanced")
    public double aimRange = 0.8;
    @Parameter(name = "InteractTicks", min = 1, max = 5, step = 1, visible = "advanced")
    public int interactTicks = 2;
    @Parameter(name = "MinYawStep", min = 5.0, max = 90.0, step = 1.0, visible = "advanced")
    public double minYawStep = 22.0;
    @Parameter(name = "MaxYawStep", min = 10.0, max = 120.0, step = 1.0, visible = "advanced")
    public double maxYawStep = 38.0;
    @Parameter(name = "MaxPitchStep", min = 5.0, max = 60.0, step = 1.0, visible = "advanced")
    public double maxPitchStep = 18.0;
    @Parameter(name = "HitboxAim", visible = "advanced")
    public boolean hitboxAim = true;
    @Parameter(name = "ShieldBreaker", visible = "advanced")
    public boolean shieldBreaker = true;
    @Parameter(name = "PauseWhileEating", visible = "advanced")
    public boolean pauseWhileEating = false;
    @Parameter(name = "RayTrace", modes = {"Off", "Hitbox", "Alternative"}, visible = "advanced")
    public String rayTrace = "Hitbox";
    public static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    private net.minecraft.world.entity.LivingEntity currentTarget = null;
    private long lastAttackTime = 0;
    private float prevYaw = 0;
    private float prevPitch = 0;
    private int airTicks = 0;
    private double predictedFall = 0.0;
    private int snap2Ticks = 0;
    private net.minecraft.world.phys.Vec3 hitboxPoint = net.minecraft.world.phys.Vec3.ZERO;
    private net.minecraft.world.phys.Vec3 hitboxMotion = net.minecraft.world.phys.Vec3.ZERO;

    private int sprintResetTicks = 0;
    public static boolean serverSprinting = false;

    public static void onCritSound(long now) {
    }

    private static float scanProgress = 0f;
    private static float prevScanProgress = 0f;
    private static float slowRotation = 0f;
    private static float prevSlowRotation = 0f;
    private static float circleStep = 0f;
    private static float prevCircleStep = 0f;
    private static float squareAngle = 0f;
    private static float prevSquareAngle = 0f;
    private static float squareSpeed = 0f;
    private static boolean squareFlipSpeed = false;

    public net.minecraft.world.entity.LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }

    public void onEnable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() != null) serverSprinting = PlayerUtility.isSprinting(mc.getPlayer());
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!event.isSend()) return;
        if (event.getPacket() instanceof net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket cmd) {
            var action = cmd.getAction();
            if (action == net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_SPRINTING)
                serverSprinting = true;
            else if (action == net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.STOP_SPRINTING)
                serverSprinting = false;
        }
    }

    public void onDisable() {
        silentRotation.hasRotation = false;
        currentTarget = null;
        prevYaw = 0;
        prevPitch = 0;
        sprintResetTicks = 0;
        airTicks = 0;
        predictedFall = 0.0;
        snap2Ticks = 0;
        hitboxPoint = net.minecraft.world.phys.Vec3.ZERO;
        hitboxMotion = net.minecraft.world.phys.Vec3.ZERO;
    }

    public static void onPreTick() {
        KillAura ka = Modules.get(KillAura.class);
        if (ka == null || !Modules.enabled(KillAura.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;

        if (ka.sprintResetTicks > 0) {
            ka.sprintResetTicks--;
            mc.getPlayer().setSprinting(false);
            if (ka.sprintResetTicks == 0 && canStartSprint(mc)) {
                mc.getPlayer().setSprinting(true);
                NetworkUtility.sendPlayerCommand(net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_SPRINTING);
            }
        } else {
            switch (ka.sprintMode) {
                case "Legit" -> {
                    float cooldown = mc.getPlayer().getAttackStrengthScale(0.5f);
                    if (canStartSprint(mc) && cooldown >= 0.8f) {
                        mc.getPlayer().setSprinting(true);
                    }
                }
                case "HvH" -> {
                    if (canStartSprint(mc)) {
                        mc.getPlayer().setSprinting(true);
                    }
                }
            }
        }

        var player = mc.getPlayer();
        if (player.onGround()) {
            ka.airTicks = 0;
            ka.predictedFall = 0.0;
        } else {
            ka.airTicks++;
            double vY = player.getDeltaMovement().y;
            if (vY < 0) {
                ka.predictedFall += -vY;
            }
        }

        var target = ka.currentTarget;
        if (target == null || EntityUtility.isDead(target)) return;

        double distToTarget = Math.sqrt(target.getBoundingBox().distanceToSqr(player.getEyePosition()));
        if (distToTarget > ka.range + ka.aimRange) return;

        if (ka.autoJump && ka.smartCrits && player.onGround()
                && !mc.getOptions().keyJump.isDown() && !PlayerUtility.isUsingItem(player)) {
            player.jumpFromGround();
        }

        if (ka.mode.startsWith("Snap2")) {
            boolean imminent = player.getAttackStrengthScale(0.5f) >= 0.85f || canPerformCrit(ka, player);
            if (imminent) {
                ka.snap2Ticks = Math.max(ka.snap2Ticks, ka.interactTicks);
            }
        }

        float[] freshAngles = ka.calculateAngles(mc, target);
        float freshYaw   = freshAngles[0];
        float freshPitch = freshAngles[1];

        if (ka.mode.startsWith("Snap2") && ka.snap2Ticks == 0) {
            silentRotation.hasRotation = false;
        } else {
            silentRotation.set(freshYaw, freshPitch);
        }

        if (ka.attackTiming.equals("Pre")) {
            tryAttack(ka, mc, target, freshYaw, freshPitch);
        }
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) {
            silentRotation.hasRotation = false;
            return;
        }
        silentRotation.hasRotation = false;

        var locked = currentTarget;
        if (locked == null || EntityUtility.isDead(locked) || !isValidTarget(mc, locked)) {
            locked = null;
        }
        var best = findTarget(mc);
        if (best != null && (locked == null || (best != locked
                && eyeDistanceTo(mc, best) + 0.5 < eyeDistanceTo(mc, locked)))) {
            locked = best;
        }
        if (locked == null) {
            currentTarget = null;
            if (mode.startsWith("Snap2")) {
                snap2Ticks = 0;
                prevYaw = mc.getPlayer().getYRot();
                prevPitch = mc.getPlayer().getXRot();
            }
            return;
        }
        currentTarget = locked;
        var target = locked;

        if (mode.startsWith("Snap2")) {
            boolean imminent = mc.getPlayer().getAttackStrengthScale(0.5f) >= 0.85f || canPerformCrit(this, mc.getPlayer());
            if (imminent) {
                snap2Ticks = Math.max(snap2Ticks, interactTicks);
            }
        }

        float[] angles = calculateAngles(mc, target);

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

        if (mode.startsWith("Snap2") && snap2Ticks == 0) {
            prevYaw = mc.getPlayer().getYRot();
            prevPitch = mc.getPlayer().getXRot();
            silentRotation.hasRotation = false;
        } else {
            silentRotation.set(yaw, pitch);
            if (freeLook) {
                mc.getPlayer().yHeadRot = yaw;
                mc.getPlayer().yBodyRot = yaw;
            } else {
                mc.getPlayer().setYRot(yaw);
                mc.getPlayer().setXRot(pitch);
                mc.getPlayer().yHeadRot = yaw;
                mc.getPlayer().yBodyRot = yaw;
            }
        }

        if (autoJump && smartCrits && mc.getPlayer().onGround()
                && !mc.getOptions().keyJump.isDown() && currentTarget != null && !PlayerUtility.isUsingItem(mc.getPlayer()))
            mc.getPlayer().jumpFromGround();

        if (currentTarget != null) {
            tryAttack(this, mc, currentTarget, yaw, pitch);
        }

        if (snap2Ticks > 0) {
            snap2Ticks--;
        }

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

        prevSquareAngle = squareAngle;
        squareAngle += squareSpeed;
        if (squareSpeed > 25) squareFlipSpeed = true;
        if (squareSpeed < -25) squareFlipSpeed = false;
        squareSpeed = squareFlipSpeed ? squareSpeed - 0.5f : squareSpeed + 0.5f;
    }

    private static boolean isHvH(KillAura ka) {
        return "HvH".equals(ka.mode);
    }

    private static boolean canPerformCrit(KillAura ka, net.minecraft.client.player.LocalPlayer player) {
        if (player.getAbilities().flying
                || player.isFallFlying()
                || player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING)
                || player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
                || player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)
                || player.isInWater() || player.isInLava()
                || player.onClimbable() || player.isPassenger()) {
            return false;
        }
        if (player.onGround()) return false;

        double velY = player.getDeltaMovement().y;
        double fall = Math.max(player.fallDistance, ka.predictedFall);
        if (ka.minFall > 0.0) {
            return velY < 0.0 && fall >= ka.minFall;
        }
        return velY < 0.0 || fall > 0.0;
    }

    private static void tryAttack(KillAura ka, MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target, float yaw, float pitch) {
        var player = mc.getPlayer();
        if (player == null || target == null || EntityUtility.isDead(target)) return;

        if (ka.pauseWhileEating && PlayerUtility.isUsingItem(player)) {
            var stack = player.getUseItem();
            if (InventoryUtility.getFoodProperties(stack) != null || InventoryUtility.isPotion(stack) || InventoryUtility.isGoldenApple(stack) || InventoryUtility.isEnchantedGoldenApple(stack)) {
                return;
            }
        }

        var eyePos = player.getEyePosition();
        double dist = Math.sqrt(target.getBoundingBox().distanceToSqr(eyePos));
        if (dist > ka.range) return;

        if (!checkRayTrace(mc, target, yaw, pitch, ka.rayTrace, ka.range, isHvH(ka))) return;

        boolean hvh = isHvH(ka);
        boolean isJumping = mc.getOptions().keyJump.isDown() || ka.autoJump || Modules.enabled(Speed.class);
        boolean inAir = !player.onGround();
        boolean wantCrit = ka.smartCrits && (ka.onlyCritz || inAir || isJumping);

        if (wantCrit && !canPerformCrit(ka, player)) {
            return;
        }

        float attackScale = player.getAttackStrengthScale(0.5f);
        if (wantCrit) {
            if (attackScale < 0.92f) return;
        } else {
            if (attackScale < (float) ka.cooldownThreshold) return;
        }

        long now = System.currentTimeMillis();
        if (now - ka.lastAttackTime < 50) return;

        if (ka.mode.equals("Tracker")) {
            var targetAimPos = target.position().add(0, target.getBbHeight() * 0.45, 0);
            float[] desired = RotationUtility.anglesTo(eyePos, targetAimPos);
            float yawDiff   = Math.abs(RotationUtility.normalizeYaw(yaw - desired[0]));
            float pitchDiff = Math.abs(pitch - desired[1]);
            if (yawDiff > 18.0f || pitchDiff > 20.0f) return;
        }

        if (ka.mode.equals("Snap2") && !hvh) {
            var aimPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
            float[] desired = RotationUtility.anglesTo(eyePos, aimPos);
            float yawDiff = Math.abs(RotationUtility.normalizeYaw(player.getYRot() - desired[0]));
            float pitchDiff = Math.abs(player.getXRot() - desired[1]);
            if (yawDiff > ka.fov / 2.0 || pitchDiff > 40.0f) return;
        }

        if (ka.shieldBreaker && target instanceof net.minecraft.world.entity.player.Player targetPlayer && targetPlayer.isBlocking()) {
            int axeSlot = -1;
            for (int i = 0; i < 9; i++) {
                var stack = InventoryUtility.getItem(player, i);
                if (InventoryUtility.isAxeItem(stack)) {
                    axeSlot = i;
                    break;
                }
            }
            if (axeSlot != -1) {
                int currentSlot = InventoryUtility.getSelectedSlot(player);
                NetworkUtility.sendSetCarriedItem(axeSlot);
                boolean wasSprinting = player.isSprinting() || serverSprinting;
                if (wasSprinting) {
                    player.setSprinting(false);
                    NetworkUtility.sendPlayerCommand(net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.STOP_SPRINTING);
                }
                EntityUtility.attack(mc, target);
                EntityUtility.swingHand(mc);
                if (wasSprinting) {
                    player.setSprinting(true);
                    NetworkUtility.sendPlayerCommand(net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_SPRINTING);
                }
                NetworkUtility.sendSetCarriedItem(currentSlot);
                ka.lastAttackTime = now;
                if (ka.mode.startsWith("Snap2")) ka.snap2Ticks = Math.max(ka.snap2Ticks, 1);
                return;
            }
        }

        boolean wasSprinting = player.isSprinting() || serverSprinting;
        if (wasSprinting) {
            player.setSprinting(false);
            NetworkUtility.sendPlayerCommand(net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.STOP_SPRINTING);
        }

        ka.attack(mc, target);
        ka.lastAttackTime = now;

        if (wasSprinting) {
            player.setSprinting(true);
            NetworkUtility.sendPlayerCommand(net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_SPRINTING);
        }

        if (ka.mode.startsWith("Snap2")) {
            ka.snap2Ticks = Math.max(ka.snap2Ticks, 1);
        }
        if (ka.sprintMode.equals("Legit")) {
            ka.sprintResetTicks = 1;
        }

        if (ka.keepSprint) {
            if (player.hurtTime > 0) {
                player.setSprinting(true);
                double multiplier = ka.keepSprintSpeed / 100.0;
                if (multiplier < 1.0) {
                    var vel = player.getDeltaMovement();
                    MoveUtility.setMotion(vel.x * multiplier, vel.y, vel.z * multiplier);
                }
            }
            if (PotionUtility.hasBlindness(player) && PlayerUtility.isSprinting(player)) {
                player.setSprinting(false);
            }
        }
    }

    private static boolean canStartSprint(MinecraftWrapper mc) {
        var p = mc.getPlayer();
        if (p == null) return false;
        return p.input.hasForwardImpulse()
            && !PotionUtility.hasBlindness(p)
            && !p.isFallFlying()
            && !PlayerUtility.isUsingItem(p)
            && !p.horizontalCollision
            && p.getFoodData().getFoodLevel() > 6
            && !PlayerUtility.isSneaking(p);
    }

    public static boolean shouldBlockSprint() {
        KillAura ka = Modules.get(KillAura.class);
        return ka != null && Modules.enabled(KillAura.class) && "Legit".equals(ka.sprintMode) && ka.sprintResetTicks > 0;
    }

    private static double eyeDistanceTo(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity le) {
        return Math.sqrt(le.getBoundingBox().distanceToSqr(mc.getPlayer().getEyePosition()));
    }

    private boolean isValidTarget(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity le) {
        if (EntityUtility.isSelf(le)) return false;
        if (EntityUtility.isDead(le)) return false;
        if (!targets.contains("Invisibles") && le.isInvisible()) return false;
        if (EntityUtility.isArmorStand(le)) return false;
        if (!targets.contains("Players") && EntityUtility.isPlayer(le)) return false;
        if (!targets.contains("Monsters") && EntityUtility.isHostile(le)) return false;
        if (!targets.contains("Passives") && EntityUtility.isPassive(le)) return false;
        if (!targets.contains("Creative") && le instanceof net.minecraft.world.entity.player.Player creativeCheck && GameModeUtility.isCreative(creativeCheck)) return false;
        if (eyeDistanceTo(mc, le) > range + aimRange + 0.3) return false;
        if (!throughWalls && !mc.getPlayer().hasLineOfSight(le)) return false;
        if (AntiBot.isBotCheck(le)) return false;
        if (mode.equals("Snap2")) {
            float angleToTarget = Math.abs(RotationUtility.normalizeYaw(RotationUtility.yawTo(mc.getPlayer().getEyePosition(), le.position().add(0, le.getBbHeight() * 0.5, 0)) - mc.getPlayer().getYRot()));
            if (angleToTarget > fov / 2.0) return false;
        }
        return true;
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
            if (!targets.contains("Creative") && le instanceof net.minecraft.world.entity.player.Player creativeCheck && GameModeUtility.isCreative(creativeCheck)) continue;

            double dist = Math.sqrt(le.getBoundingBox().distanceToSqr(mc.getPlayer().getEyePosition()));

            var mobVel = le.getDeltaMovement();
            double mobSpeed = Math.sqrt(mobVel.x * mobVel.x + mobVel.z * mobVel.z);
            double buffer = BASE_BUFFER + Math.min(mobSpeed * 1.5, 0.1);

            if (dist > range + aimRange - buffer) continue;

            if (!throughWalls && !mc.getPlayer().hasLineOfSight(le)) continue;
            if (AntiBot.isBotCheck(e)) continue;
            if (mode.equals("Snap2")) {
                float angleToTarget = Math.abs(RotationUtility.normalizeYaw(RotationUtility.yawTo(mc.getPlayer().getEyePosition(), le.position().add(0, le.getBbHeight() * 0.5, 0)) - mc.getPlayer().getYRot()));
                if (angleToTarget > fov / 2.0) continue;
            }

            if (dist < closestDist) {
                closestDist = dist;
                closest = le;
            }
        }
        return closest;
    }

    private void attack(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target) {
        int originalSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        int bestSlot = -1;
        if (autoWeapon && !swapMode.equals("None")) {
            double bestDmg = -1.0;
            for (int i = 0; i < 9; i++) {
                var stack = InventoryUtility.getItem(mc.getPlayer(), i);
                if (swordsOnly && !InventoryUtility.isSwordItem(stack)) continue;
                double dmg = CombatUtility.getWeaponDamage(stack);
                if (dmg > bestDmg) {
                    bestDmg = dmg;
                    bestSlot = i;
                }
            }
            if (bestSlot != -1 && bestSlot != originalSlot && bestDmg > 1.0) {
                if (swapMode.equals("Silent")) {
                    NetworkUtility.sendSetCarriedItem(bestSlot);
                } else {
                    InventoryUtility.selectSlot(mc.getPlayer(), bestSlot);
                }
            }
        }
        EntityUtility.attack(mc, target);
        EntityUtility.swingHand(mc);
        if (autoWeapon && swapMode.equals("Silent") && bestSlot != -1 && bestSlot != originalSlot) {
            NetworkUtility.sendSetCarriedItem(originalSlot);
        }
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
        } else if (mode.startsWith("Snap2")) {
            var aimPos = getLegitHitboxPoint(target);
            float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), aimPos);
            float targetYaw = angles[0];
            float targetPitch = angles[1];

            float diffYaw = RotationUtility.normalizeYaw(targetYaw - prevYaw);
            float diffPitch = targetPitch - prevPitch;

            float absDiffYaw = Math.abs(diffYaw);
            float absDiffPitch = Math.abs(diffPitch);

            float tYaw = Math.min(absDiffYaw / 60.0f, 1.0f);
            float tPitch = Math.min(absDiffPitch / 40.0f, 1.0f);

            double currentMaxYaw = minYawStep + (maxYawStep - minYawStep) * (tYaw * (2.0 - tYaw));
            double currentMaxPitch = Math.max(4.0, maxPitchStep * (tPitch * (2.0 - tPitch)));

            float clampedYawDiff = Math.clamp(diffYaw, (float) -currentMaxYaw, (float) currentMaxYaw);
            float clampedPitchDiff = Math.clamp(diffPitch, (float) -currentMaxPitch, (float) currentMaxPitch);

            float jitterYaw = (float) (Math.random() * 0.2 - 0.1);
            float jitterPitch = (float) (Math.random() * 0.15 - 0.075);

            float rawYaw = prevYaw + clampedYawDiff + jitterYaw;
            float rawPitch = Math.clamp(prevPitch + clampedPitchDiff + jitterPitch, -90.0f, 90.0f);

            float gcd = RotationUtility.getGCD();
            if (gcd > 0) {
                float dYaw = RotationUtility.normalizeYaw(rawYaw - prevYaw);
                float dPitch = rawPitch - prevPitch;
                dYaw = Math.round(dYaw / gcd) * gcd;
                dPitch = Math.round(dPitch / gcd) * gcd;
                yaw = prevYaw + dYaw;
                pitch = Math.clamp(prevPitch + dPitch, -90.0f, 90.0f);
            } else {
                yaw = rawYaw;
                pitch = rawPitch;
            }
        } else {
            var headPos = target.getEyePosition();
            float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), headPos);
            yaw = angles[0];
            pitch = angles[1];
        }

        prevYaw = yaw;
        prevPitch = pitch;
        return new float[]{yaw, pitch};
    }

    private net.minecraft.world.phys.Vec3 getLegitHitboxPoint(net.minecraft.world.entity.LivingEntity target) {
        if (!hitboxAim) {
            return target.position().add(0, target.getBbHeight() * 0.5, 0);
        }
        double width = target.getBbWidth();
        double height = target.getBbHeight();
        double halfW = Math.max((width - 0.08) / 2.0, 0.05);

        if (hitboxMotion.equals(net.minecraft.world.phys.Vec3.ZERO)) {
            hitboxMotion = new net.minecraft.world.phys.Vec3(
                (Math.random() - 0.5) * 0.04,
                (Math.random() - 0.5) * 0.04,
                (Math.random() - 0.5) * 0.04
            );
        }

        hitboxPoint = hitboxPoint.add(hitboxMotion);

        double minMotionXZ = 0.005;
        double maxMotionXZ = 0.025;
        double minMotionY = 0.003;
        double maxMotionY = 0.02;

        double mx = hitboxMotion.x;
        double my = hitboxMotion.y;
        double mz = hitboxMotion.z;

        if (hitboxPoint.x >= halfW) {
            mx = -(minMotionXZ + Math.random() * (maxMotionXZ - minMotionXZ));
        } else if (hitboxPoint.x <= -halfW) {
            mx = (minMotionXZ + Math.random() * (maxMotionXZ - minMotionXZ));
        }

        if (hitboxPoint.y >= height - 0.1) {
            my = -(minMotionY + Math.random() * (maxMotionY - minMotionY));
        } else if (hitboxPoint.y <= 0.2) {
            my = (minMotionY + Math.random() * (maxMotionY - minMotionY));
        }

        if (hitboxPoint.z >= halfW) {
            mz = -(minMotionXZ + Math.random() * (maxMotionXZ - minMotionXZ));
        } else if (hitboxPoint.z <= -halfW) {
            mz = (minMotionXZ + Math.random() * (maxMotionXZ - minMotionXZ));
        }

        hitboxMotion = new net.minecraft.world.phys.Vec3(mx, my, mz);
        hitboxPoint = hitboxPoint.add((Math.random() - 0.5) * 0.015, 0, (Math.random() - 0.5) * 0.015);

        double clampedX = Math.clamp(hitboxPoint.x, -halfW, halfW);
        double clampedY = Math.clamp(hitboxPoint.y, 0.15, Math.max(height - 0.05, 0.2));
        double clampedZ = Math.clamp(hitboxPoint.z, -halfW, halfW);
        hitboxPoint = new net.minecraft.world.phys.Vec3(clampedX, clampedY, clampedZ);

        return target.position().add(hitboxPoint);
    }

    private static boolean checkRayTrace(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target, float yaw, float pitch, String rayTraceMode, double reach, boolean lenient) {
        if ("Off".equals(rayTraceMode) || lenient) return true;
        var player = mc.getPlayer();
        if (player == null || target == null) return false;

        var eyePos = player.getEyePosition();
        var lookVec = RotationUtility.getLookVector(yaw, pitch);
        var endPos = eyePos.add(lookVec.scale(reach + 0.5));
        double inflate = lenient ? 0.35 : 0.1;

        var currentBox = target.getBoundingBox().inflate(inflate);
        if (currentBox.contains(eyePos)) return true;
        if (currentBox.clip(eyePos, endPos).isPresent()) return true;

        if (lenient || "Alternative".equals(rayTraceMode)) {
            var prevBox = target.getBoundingBox().move(target.xOld - target.getX(), target.yOld - target.getY(), target.zOld - target.getZ()).inflate(inflate + 0.05);
            if (prevBox.contains(eyePos) || prevBox.clip(eyePos, endPos).isPresent()) return true;
        }

        if (lenient) {
            var center = target.position().add(0, target.getBbHeight() * 0.5, 0);
            float[] toCenter = RotationUtility.anglesTo(eyePos, center);
            float dyaw = Math.abs(RotationUtility.normalizeYaw(yaw - toCenter[0]));
            float dpitch = Math.abs(pitch - toCenter[1]);
            if (dyaw <= 60.0f && dpitch <= 60.0f) return true;
        }

        return false;
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
        } else if (targetEspMode.equals("Square")) {
            float squareRot = prevSquareAngle + (squareAngle - prevSquareAngle) * tickDelta;
            ravex.utility.render.Render3DUtility.renderSquareESP(
                modelViewMatrix,
                camera,
                target,
                targetEspColor,
                squareRot,
                tickDelta
            );
        }
    }


}