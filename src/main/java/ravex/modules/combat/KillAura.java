package ravex.modules.combat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import ravex.modules.Module;
import ravex.utility.misc.MobUtility;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.MultiSelectParameter;
import ravex.parameter.ColorParameter;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotation;
import java.util.List;
import ravex.manager.ModuleManager;

public class KillAura extends Module {
    // Mode is now the very first parameter
    public final ModeParameter mode = new ModeParameter("Mode", "Tracker", List.of("Tracker", "Snap"));

    public final NumberParameter range = new NumberParameter("Range", 3.0, 2.0, 6.0, 0.1);
    public final NumberParameter cooldownThreshold = new NumberParameter("Attack Cooldown", 1.0, 0.0, 1.0, 0.05);

    // Integrated Target ESP parameters
    public final BooleanParameter targetEsp = new BooleanParameter("Target ESP", true);
    public final ModeParameter targetEspMode = new ModeParameter("ESP Mode", "RaveXV1", List.of("RaveXV1", "Souls"));
    public final ColorParameter targetEspColor = new ColorParameter("ESP Color", 0xFF00FFFF);
    public final NumberParameter soulsLength = new NumberParameter("Length", 18, 5, 40, 1);
    public final NumberParameter soulsFactor = new NumberParameter("Factor", 2, 1, 5, 1);
    public final NumberParameter soulsShaking = new NumberParameter("Shaking", 4.0, 1.0, 10.0, 0.5);
    public final NumberParameter soulsAmplitude = new NumberParameter("Amplitude", 2.0, 0.5, 5.0, 0.5);

    // MultiSelect dropdown parameter grouping all target filters
    public final MultiSelectParameter targets = new MultiSelectParameter(
        "Targets",
        List.of("Players", "Monsters"),
        List.of("Players", "Monsters", "Passives", "Invisibles")
    );

    public final BooleanParameter throughWalls = new BooleanParameter("ThroughWalls", true);
    public final BooleanParameter smartCrits = new BooleanParameter("SmartCrits", true);
    public final ModeParameter sprintMode = new ModeParameter("Sprint", "Normal", List.of("Normal", "Legit", "HvH"));

    public static final SilentRotation silentRotation = new SilentRotation();
    private LivingEntity currentTarget = null;
    private long lastAttackTime = 0;
    private float prevYaw = 0;
    private float prevPitch = 0;
    // Ticks remaining where sprint should be suppressed after an attack (Legit mode)
    private int sprintResetTicks = 0;

    private static float scanProgress = 0f;
    private static float prevScanProgress = 0f;
    private static float slowRotation = 0f;
    private static float prevSlowRotation = 0f;

    public LivingEntity getCurrentTarget() {
        return currentTarget;
    }

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
        currentTarget = null;
        prevYaw = 0;
        prevPitch = 0;
        sprintResetTicks = 0;
    }

    // -------------------------------------------------------------------------
    // PRE-TICK: called at HEAD of Minecraft.tick(), BEFORE LocalPlayer.tick()
    // → ServerboundInteractPacket goes out before ServerboundMovePlayerPacket
    // → GrimAC "Post" check passes
    // -------------------------------------------------------------------------
    public static void onPreTick() {
        KillAura ka = ModuleManager.get(KillAura.class);
        if (ka == null || !ka.getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // --- Sprint ---
        switch (ka.sprintMode.getValue()) {
            case "Legit" -> {
                // Tick down the post-attack sprint suppression window
                if (ka.sprintResetTicks > 0) {
                    ka.sprintResetTicks--;
                    mc.player.setSprinting(false); // hold sprint off during reset window
                } else {
                    // Re-enable sprint only when moving forward AND cooldown is sufficiently ready
                    // (no burst of speed right after an attack — GrimAC detects that)
                    float cooldown = mc.player.getAttackStrengthScale(0.5f);
                    if (mc.player.input.hasForwardImpulse()
                            && !mc.player.isUsingItem()
                            && !mc.player.isShiftKeyDown()
                            && cooldown >= 0.8f) {
                        mc.player.setSprinting(true);
                    }
                }
            }
            case "HvH" -> {
                if (!mc.player.isUsingItem() && !mc.player.isShiftKeyDown())
                    mc.player.setSprinting(true);
            }
        }

        LivingEntity target = ka.currentTarget;
        if (target == null || MobUtility.isDead(target)) return;

        // --- Refresh silentRotation RIGHT before the attack ---
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 aimPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
        float[] freshAngles = RotationUtility.anglesTo(eyePos, aimPos);
        // Apply GCD quantization so yaw/pitch are valid mouse-sensitivity-aligned increments
        // → bypasses GrimAC AimModulo360 check
        float freshYaw   = RotationUtility.fixAngle(freshAngles[0]);
        float freshPitch = RotationUtility.fixAngle(RotationUtility.clampPitch(freshAngles[1]));
        silentRotation.set(freshYaw, freshPitch);

        // --- Re-verify reach with AABB (entity may have moved since previous tick) ---
        // This prevents Reach flags when currentTarget was in range last tick but moved away.
        {
            net.minecraft.world.phys.AABB pa = mc.player.getBoundingBox();
            net.minecraft.world.phys.AABB ea = target.getBoundingBox();
            double dx = Math.max(0, Math.max(pa.minX - ea.maxX, ea.minX - pa.maxX));
            double dy = Math.max(0, Math.max(pa.minY - ea.maxY, ea.minY - pa.maxY));
            double dz = Math.max(0, Math.max(pa.minZ - ea.maxZ, ea.minZ - pa.maxZ));
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            Vec3 mobVel = target.getDeltaMovement();
            double mobSpeed = Math.sqrt(mobVel.x * mobVel.x + mobVel.z * mobVel.z);
            double buffer = 0.15 + Math.min(mobSpeed * 1.5, 0.1);
            if (dist > ka.range.getValue() - buffer) return;
        }

        // --- SmartCrits timing ---
        if (ka.smartCrits.getValue() && !mc.player.onGround()) {
            double velY = mc.player.getDeltaMovement().y;
            if (velY > -0.08) return;
        }

        // --- Cooldown check ---
        float scale = mc.player.getAttackStrengthScale(0.5f);
        if (scale < ka.cooldownThreshold.getValue().floatValue()) return;

        long now = System.currentTimeMillis();
        if (now - ka.lastAttackTime < 50) return;

        // --- Tracker: only attack when aim is on target ---
        if (ka.mode.getValue().equals("Tracker")) {
            Vec3 targetAimPos = target.position().add(0, target.getBbHeight() * 0.45, 0);
            float[] desired = RotationUtility.anglesTo(eyePos, targetAimPos);
            float yawDiff   = Math.abs(RotationUtility.normalizeYaw(freshYaw - desired[0]));
            float pitchDiff = Math.abs(freshPitch - desired[1]);
            if (yawDiff > 18.0f || pitchDiff > 20.0f) return;
        }

        ka.attack(mc, target);
        ka.lastAttackTime = now;
        // Legit: suppress sprint for 2 ticks after attack (vanilla resets sprint on hit)
        if (ka.sprintMode.getValue().equals("Legit")) {
            mc.player.setSprinting(false);
            ka.sprintResetTicks = 2;
        }
    }

    // -------------------------------------------------------------------------
    // TICK: rotation + target selection only (no attack here)
    // -------------------------------------------------------------------------
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            silentRotation.hasRotation = false;
            return;
        }
        silentRotation.hasRotation = false;

        LivingEntity target = findTarget(mc);
        if (target == null) {
            currentTarget = null;
            prevYaw = 0;
            prevPitch = 0;
            return;
        }
        currentTarget = target;

        float[] angles = calculateAngles(mc, target);
        // Apply GCD quantization → AimModulo360 bypass
        float yaw   = RotationUtility.fixAngle(angles[0]);
        float pitch = RotationUtility.fixAngle(RotationUtility.clampPitch(angles[1]));

        // Silent rotation: MixinLivingEntityTravel applies this during travel() so
        // movement and packet yaw match → no GrimAC Simulation flags.
        silentRotation.set(yaw, pitch);

        // Turn body/head toward target (visible in F5 / 3rd person)
        mc.player.yHeadRot = yaw;
        mc.player.yBodyRot = yaw;
        mc.player.yHeadRotO = yaw;
        mc.player.yBodyRotO = yaw;

        // Update target ESP scanning & rotation progress
        prevScanProgress = scanProgress;
        scanProgress += 0.02f; // Faster, smooth scan speed (50 ticks / 2.5 seconds per round-trip)
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
    }

    private LivingEntity findTarget(Minecraft mc) {
        // Base latency buffer: ~1 tick entity movement during packet travel
        final double BASE_BUFFER = 0.15;
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le)) continue;
            if (MobUtility.isSelf(le)) continue;
            if (MobUtility.isDead(le)) continue;
            if (!targets.isSelected("Invisibles") && le.isInvisible()) continue;
            if (MobUtility.isArmorStand(le)) continue;
            if (!targets.isSelected("Players") && MobUtility.isPlayer(le)) continue;
            if (!targets.isSelected("Monsters") && MobUtility.isHostile(le)) continue;
            if (!targets.isSelected("Passives") && MobUtility.isPassive(le)) continue;

            // Hitbox-to-hitbox distance (matches GrimAC Reach check logic)
            net.minecraft.world.phys.AABB pa = mc.player.getBoundingBox();
            net.minecraft.world.phys.AABB ea = le.getBoundingBox();
            double dx = Math.max(0, Math.max(pa.minX - ea.maxX, ea.minX - pa.maxX));
            double dy = Math.max(0, Math.max(pa.minY - ea.maxY, ea.minY - pa.maxY));
            double dz = Math.max(0, Math.max(pa.minZ - ea.maxZ, ea.minZ - pa.maxZ));
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // Extra buffer for fast-moving entities (circling / mob walking toward player)
            // GrimAC predicts entity position 1 tick ahead using its velocity
            Vec3 mobVel = le.getDeltaMovement();
            double mobSpeed = Math.sqrt(mobVel.x * mobVel.x + mobVel.z * mobVel.z);
            double buffer = BASE_BUFFER + Math.min(mobSpeed * 1.5, 0.1);

            if (dist > range.getValue() - buffer) continue;

            if (!throughWalls.getValue() && !mc.player.hasLineOfSight(le)) continue;
            if (ModuleManager.get(ravex.modules.combat.AntiBot.class).getEnabled()
                    && ModuleManager.get(ravex.modules.combat.AntiBot.class).isBot(e)) continue;

            if (dist < closestDist) {
                closestDist = dist;
                closest = le;
            }
        }
        return closest;
    }

    private void attack(Minecraft mc, LivingEntity target) {
        MobUtility.attack(mc, target);
        MobUtility.swingHand(mc); // swing client hand by default
    }

    private float[] calculateAngles(Minecraft mc, LivingEntity target) {
        if (prevYaw == 0f && prevPitch == 0f) {
            prevYaw = mc.player.getYRot();
            prevPitch = mc.player.getXRot();
        }

        float yaw, pitch;
        if (mode.getValue().equals("Tracker")) {
            Vec3 stomachPos = target.position().add(0, target.getBbHeight() * 0.45, 0);
            float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), stomachPos);
            float targetYaw = angles[0];
            float targetPitch = angles[1];

            float diffYaw = RotationUtility.normalizeYaw(targetYaw - prevYaw);
            float diffPitch = targetPitch - prevPitch;
            float absDiffYaw = Math.abs(diffYaw);

            // Smooth ease-out rotation: speed is proportional to remaining angle
            // (accelerates when far, decelerates naturally when close — like a real mouse)
            // Max cap at ~30 deg/tick to stay under GrimAC thresholds
            float MAX_YAW_SPEED   = 30.0f;
            float MAX_PITCH_SPEED = 20.0f;

            // Quadratic ease-out: t = remaining_angle / 180, speed = max * (2t - t^2)
            float tYaw   = Math.min(absDiffYaw / 180.0f, 1.0f);
            float tPitch = Math.min(Math.abs(diffPitch) / 90.0f, 1.0f);
            float speedYaw   = MAX_YAW_SPEED   * (2.0f * tYaw   - tYaw   * tYaw);
            float speedPitch = MAX_PITCH_SPEED * (2.0f * tPitch - tPitch * tPitch);

            // Enforce a tiny minimum movement so we don't freeze when very close
            speedYaw   = Math.max(speedYaw,   0.5f);
            speedPitch = Math.max(speedPitch, 0.3f);

            // Small random jitter to simulate mouse noise (breaks pattern detection)
            float jitterYaw   = (float) (Math.random() * 0.6 - 0.3);
            float jitterPitch = (float) (Math.random() * 0.4 - 0.2);

            float stepYaw   = Math.max(-speedYaw,   Math.min(speedYaw,   diffYaw))   + jitterYaw;
            float stepPitch = Math.max(-speedPitch, Math.min(speedPitch, diffPitch)) + jitterPitch;

            yaw = prevYaw + stepYaw;
            pitch = prevPitch + stepPitch;
        } else {
            Vec3 chestPos = target.position().add(0, target.getBbHeight() * 0.65, 0);
            float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), chestPos);
            yaw = angles[0];
            pitch = angles[1];
        }

        prevYaw = yaw;
        prevPitch = pitch;
        return new float[]{yaw, pitch};
    }

    public void render(Matrix4f modelViewMatrix, Camera camera, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        LivingEntity target = currentTarget;
        if (target == null || target.isDeadOrDying()) return;

        if (targetEspMode.getValue().equals("RaveXV1")) {
            float progressVal = prevScanProgress + (scanProgress - prevScanProgress) * tickDelta;
            float rotation = prevSlowRotation + (slowRotation - prevSlowRotation) * tickDelta;
            ravex.utility.render.Render3DEngine.renderRaveXESP(
                modelViewMatrix,
                camera,
                target,
                targetEspColor.getValue(),
                progressVal,
                rotation,
                tickDelta
            );
        } else {
            ravex.utility.render.Render3DEngine.renderSoulsESP(
                modelViewMatrix,
                camera,
                target,
                targetEspColor.getValue(),
                soulsLength.getValue().intValue(),
                soulsFactor.getValue().intValue(),
                soulsShaking.getValue().floatValue(),
                soulsAmplitude.getValue().floatValue(),
                tickDelta
            );
        }
    }
}
