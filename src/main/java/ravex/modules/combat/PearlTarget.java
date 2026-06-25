package ravex.modules.combat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.misc.NativeLoader;
import ravex.utility.render.Render3DUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PearlTarget extends Module {
    public static final PearlTarget INSTANCE = new PearlTarget();

    // ── Targeting ──────────────────────────────────────────────────────────
    public final ModeParameter mode = new ModeParameter("Mode", "Combat",
        List.of("Combat", "Pearl", "Follow"));
    public final NumberParameter range = new NumberParameter("Range", 16.0, 1.0, 32.0, 0.5);
    public final ModeParameter targetMode = new ModeParameter("Target Mode", "Nearest",
        List.of("Nearest", "Health", "Crosshair", "Distance"));
    public final NumberParameter switchDelay = new NumberParameter("Switch Delay", 500.0, 0.0, 2000.0, 50.0);

    // ── Chase ──────────────────────────────────────────────────────────────
    public final NumberParameter speed = new NumberParameter("Speed", 1.8, 0.5, 5.0, 0.1);
    public final NumberParameter speedSneak = new NumberParameter("Speed Sneak", 0.6, 0.1, 2.0, 0.1);
    public final BooleanParameter strafe = new BooleanParameter("Strafe", true);
    public final BooleanParameter jump = new BooleanParameter("Jump", true);
    public final NumberParameter jumpHeight = new NumberParameter("Jump Height", 0.42, 0.3, 0.6, 0.01);
    public final NumberParameter predictTicks = new NumberParameter("Predict Ticks", 100.0, 20.0, 300.0, 10.0);
    public final NumberParameter chaseTime = new NumberParameter("Chase Time", 3000.0, 500.0, 10000.0, 100.0);
    public final NumberParameter stopDistance = new NumberParameter("Stop Distance", 3.5, 1.0, 6.0, 0.5);
    public final BooleanParameter sprint = new BooleanParameter("Sprint", true);

    // ── Combat ─────────────────────────────────────────────────────────────
    public final BooleanParameter autoWeapon = new BooleanParameter("Auto Weapon", true);
    public final ModeParameter weaponMode = new ModeParameter("Weapon Mode", "Sword",
        List.of("Sword", "Axe", "Both"));
    public final BooleanParameter autoGap = new BooleanParameter("Auto Gap", false);
    public final NumberParameter gapHealth = new NumberParameter("Gap Health", 10.0, 1.0, 20.0, 1.0);
    public final BooleanParameter autoPearl = new BooleanParameter("Auto Pearl", false);
    public final NumberParameter pearlRange = new NumberParameter("Pearl Range", 20.0, 5.0, 50.0, 5.0);
    public final BooleanParameter autoTotem = new BooleanParameter("Auto Totem", false);
    public final ModeParameter totemMode = new ModeParameter("Totem Mode", "Always",
        List.of("Always", "Low HP", "After Kill"));
    public final NumberParameter totemHealth = new NumberParameter("Totem Health", 6.0, 1.0, 20.0, 1.0);
    public final BooleanParameter attack = new BooleanParameter("Attack", true);
    public final NumberParameter attackRange = new NumberParameter("Attack Range", 4.0, 2.0, 6.0, 0.1);
    public final NumberParameter attackCps = new NumberParameter("Attack CPS", 12.0, 1.0, 20.0, 1.0);
    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
    public final BooleanParameter keepRotate = new BooleanParameter("Keep Rotate", false);

    // ── Render ─────────────────────────────────────────────────────────────
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter lineColor = new ColorParameter("Line Color", 0xFFFF5500);
    public final ColorParameter landingColor = new ColorParameter("Landing Color", 0xFFFF3333);
    public final ColorParameter pearlColor = new ColorParameter("Pearl Color", 0xFFFFFF00);
    public final NumberParameter lineWidth = new NumberParameter("Line Width", 2.0, 0.5, 5.0, 0.5);
    public final BooleanParameter renderLine = new BooleanParameter("Render Line", true);
    public final BooleanParameter renderLanding = new BooleanParameter("Render Landing", true);
    public final BooleanParameter renderTrail = new BooleanParameter("Render Trail", true);
    public final BooleanParameter renderInfo = new BooleanParameter("Render Info", true);
    public final BooleanParameter renderThroughWalls = new BooleanParameter("Through Walls", false);
    public final BooleanParameter renderPredictionLine = new BooleanParameter("Prediction Line", true);

    // ── State ──────────────────────────────────────────────────────────────
    private static boolean nativeAvailable = false;
    private final Map<Integer, PearlData> trackedPearls = new HashMap<>();
    private Player target = null;
    private Vec3 targetPos = null;
    private Player lastPearlThrower = null;
    private Vec3 lastPearlLanding = null;
    private Vec3 lastPearlPos = null;
    private Vec3 lastPearlVel = null;
    private long lastPearlTime = 0;
    private long lastAttackTime = 0;
    private long lastTargetSwitchTime = 0;
    private int currentTargetIndex = 0;
    private boolean wasSprinting = false;

    // Render data for mixin
    public Vec3 renderPearlPos = null;
    public Vec3 renderLandingPos = null;
    public Vec3 renderTargetPos = null;

    static {
        try {
            nativeAvailable = NativeLoader.loadLibrary("ravex_pearltarget");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[PearlTarget JNI] " + e.getMessage());
        }
    }

    private PearlTarget() {
        super("PearlTarget", Category.COMBAT);
        addParameter(mode);
        addParameter(range);
        addParameter(targetMode);
        addParameter(switchDelay);
        addParameter(speed);
        addParameter(speedSneak);
        addParameter(strafe);
        addParameter(jump);
        addParameter(jumpHeight);
        addParameter(predictTicks);
        addParameter(chaseTime);
        addParameter(stopDistance);
        addParameter(sprint);
        addParameter(autoWeapon);
        addParameter(weaponMode);
        addParameter(autoGap);
        addParameter(gapHealth);
        addParameter(autoPearl);
        addParameter(pearlRange);
        addParameter(autoTotem);
        addParameter(totemMode);
        addParameter(totemHealth);
        addParameter(attack);
        addParameter(attackRange);
        addParameter(attackCps);
        addParameter(rotate);
        addParameter(keepRotate);
        addParameter(render);
        addParameter(lineColor);
        addParameter(landingColor);
        addParameter(pearlColor);
        addParameter(lineWidth);
        addParameter(renderLine);
        addParameter(renderLanding);
        addParameter(renderTrail);
        addParameter(renderInfo);
        addParameter(renderThroughWalls);
        addParameter(renderPredictionLine);

        weaponMode.setVisible(autoWeapon::getValue);
        gapHealth.setVisible(autoGap::getValue);
        pearlRange.setVisible(autoPearl::getValue);
        totemHealth.setVisible(() -> autoTotem.getValue() && "Low HP".equals(totemMode.getValue()));
        renderLine.setVisible(render::getValue);
        renderLanding.setVisible(render::getValue);
        renderTrail.setVisible(render::getValue);
        renderInfo.setVisible(render::getValue);
        renderPredictionLine.setVisible(render::getValue);
        renderThroughWalls.setVisible(render::getValue);
        lineColor.setVisible(() -> render.getValue() && renderLine.getValue());
        landingColor.setVisible(() -> render.getValue() && renderLanding.getValue());
        pearlColor.setVisible(() -> render.getValue() && renderTrail.getValue());
        lineWidth.setVisible(() -> render.getValue() && (renderLine.getValue() || renderPredictionLine.getValue()));
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        double r = range.getValue();
        List<ThrownEnderpearl> pearls = new ArrayList<>();
        List<Player> players = new ArrayList<>();

        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof ThrownEnderpearl pearl) {
                if (mc.player.distanceTo(pearl) <= r) {
                    pearls.add(pearl);
                }
            }
            if (e instanceof Player p && p != mc.player && !p.isSpectator()) {
                if (mc.player.distanceTo(p) <= r * 1.5) {
                    players.add(p);
                }
            }
        }

        // Track pearls
        for (ThrownEnderpearl pearl : pearls) {
            Entity owner = pearl.getOwner();
            if (owner == mc.player) continue;

            int id = pearl.getId();
            Vec3 pos = pearl.position();
            Vec3 vel = pearl.getDeltaMovement();
            Vec3 landing = predictLanding(pos, vel);

            trackedPearls.put(id, new PearlData(
                id, owner != null ? owner.getUUID() : null,
                pos, vel, landing,
                System.currentTimeMillis(), pearl
            ));

            lastPearlTime = System.currentTimeMillis();
            lastPearlPos = pos;
            lastPearlVel = vel;
            lastPearlLanding = landing;

            if (owner instanceof Player playerOwner && playerOwner != target) {
                lastPearlThrower = playerOwner;
                if (System.currentTimeMillis() - lastTargetSwitchTime > switchDelay.getValue()) {
                    target = playerOwner;
                    targetPos = landing;
                    lastTargetSwitchTime = System.currentTimeMillis();
                }
            } else if ("Pearl".equals(mode.getValue())) {
                targetPos = landing;
            }
        }

        // Clean up landed pearls
        Iterator<Map.Entry<Integer, PearlData>> it = trackedPearls.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, PearlData> entry = it.next();
            PearlData data = entry.getValue();
            boolean alive = mc.level.getEntity(data.entityId) != null;
            if (!alive || System.currentTimeMillis() - data.time > chaseTime.getValue()) {
                it.remove();
            }
        }

        // Select best target if not tracking from pearl
        if (target == null && !players.isEmpty() && !"Pearl".equals(mode.getValue())) {
            target = findBestTarget(mc, players);
            if (target != null) targetPos = target.position();
        }

        // Reset target if too far or dead
        if (target != null && (target.isRemoved() || target.isDeadOrDying()
            || mc.player.distanceTo(target) > r * 2)) {
            target = null;
            targetPos = null;
        }

        if (target == null && targetPos == null) return;

        Vec3 myPos = mc.player.position();
        Vec3 moveTarget = "Follow".equals(mode.getValue()) && target != null
            ? target.position() : targetPos;
        if (moveTarget == null) return;

        double dist = myPos.distanceTo(moveTarget);
        double attackDist = attackRange.getValue();
        boolean inRange = dist <= attackDist;

        // Update render data
        updateRenderData(moveTarget);

        if (!inRange) {
            doChaseMovement(mc, moveTarget);
        }

        if (inRange && attack.getValue() && target != null) {
            doAttack(mc, target);
        }

        // AutoTotem
        if (autoTotem.getValue()) {
            doAutoTotem(mc);
        }

        // AutoGap
        if (autoGap.getValue()) {
            doAutoGap(mc);
        }

        // AutoPearl gap-close
        if (autoPearl.getValue() && target != null && dist > pearlRange.getValue() * 0.8 && dist < pearlRange.getValue()) {
            doAutoPearl(mc);
        }
    }

    private void doChaseMovement(Minecraft mc, Vec3 moveTarget) {
        Vec3 myPos = mc.player.position();
        Vec3 diff = moveTarget.subtract(myPos);
        double dist = diff.length();
        if (dist < 0.1) return;

        Vec3 dir = new Vec3(diff.x, 0, diff.z).normalize();
        double speedVal = mc.player.onGround() ? speed.getValue() : speed.getValue() * 0.8;

        // Sprint
        if (sprint.getValue()) {
            mc.player.setSprinting(true);
            wasSprinting = true;
        }

        // Set motion
        Vec3 motion = mc.player.getDeltaMovement();
        double targetVx = dir.x * speedVal;
        double targetVz = dir.z * speedVal;

        if (strafe.getValue() && mc.player.onGround()) {
            motion = new Vec3(targetVx, motion.y, targetVz);
        } else {
            motion = new Vec3(targetVx, motion.y, targetVz);
        }

        mc.player.setDeltaMovement(motion);

        // Auto-jump
        if (jump.getValue() && mc.player.onGround() && dist > 1.5) {
            mc.player.jumpFromGround();
        }

        // Rotate
        if (rotate.getValue() && target != null) {
            Vec3 lookTarget = target.position().add(0, target.getEyeHeight() * 0.8, 0);
            Vec3 lookDiff = lookTarget.subtract(mc.player.getEyePosition());
            double yaw = Math.toDegrees(Math.atan2(-lookDiff.x, lookDiff.z));
            double pitch = Math.toDegrees(-Math.atan2(lookDiff.y, Math.sqrt(lookDiff.x * lookDiff.x + lookDiff.z * lookDiff.z)));
            mc.player.setYRot((float) yaw);
            mc.player.setXRot((float) Math.clamp(pitch, -90, 90));
        }
    }

    private void doAttack(Minecraft mc, Player target) {
        long now = System.currentTimeMillis();
        long attackDelay = (long) (1000.0 / attackCps.getValue());

        if (now - lastAttackTime < attackDelay) return;

        // Auto Weapon
        if (autoWeapon.getValue()) {
            int bestSlot = findBestWeaponSlot(mc);
            if (bestSlot != -1) {
                mc.player.getInventory().setSelectedSlot(bestSlot);
            }
        }

        // Rotate
        if (rotate.getValue()) {
            Vec3 lookTarget = target.position().add(0, target.getEyeHeight() * 0.8, 0);
            Vec3 lookDiff = lookTarget.subtract(mc.player.getEyePosition());
            double yaw = Math.toDegrees(Math.atan2(-lookDiff.x, lookDiff.z));
            double pitch = Math.toDegrees(-Math.atan2(lookDiff.y, Math.sqrt(lookDiff.x * lookDiff.x + lookDiff.z * lookDiff.z)));
            mc.player.setYRot((float) yaw);
            mc.player.setXRot((float) Math.clamp(pitch, -90, 90));
        }

        mc.gameMode.attack(mc.player, target);
        mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        lastAttackTime = now;
    }

    private void doAutoTotem(Minecraft mc) {
        String mode = totemMode.getValue();
        boolean shouldTotem = "Always".equals(mode)
            || ("Low HP".equals(mode) && mc.player.getHealth() + mc.player.getAbsorptionAmount() <= totemHealth.getValue());

        if (!shouldTotem) return;

        int totemSlot = -1;
        for (int i = 0; i < 36; i++) {
            var stack = mc.player.getInventory().getItem(i < 9 ? i + 36 : i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                totemSlot = i < 9 ? i + 36 : i;
                break;
            }
        }
        if (totemSlot == -1) return;

        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        if (!mc.player.getOffhandItem().isEmpty()) return;

        mc.gameMode.handleInventoryMouseClick(
            mc.player.containerMenu.containerId,
            totemSlot < 36 ? totemSlot : totemSlot,
            0,
            net.minecraft.world.inventory.ClickType.QUICK_MOVE,
            mc.player
        );
    }

    private void doAutoGap(Minecraft mc) {
        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() > gapHealth.getValue()) return;
        if (mc.player.isUsingItem()) return;

        int gapSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.ENCHANTED_GOLDEN_APPLE) || stack.is(Items.GOLDEN_APPLE)) {
                gapSlot = i;
                break;
            }
        }
        if (gapSlot == -1) return;

        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(gapSlot);
        mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (keepRotate.getValue()) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
        }
    }

    private void doAutoPearl(Minecraft mc) {
        if (mc.player.isUsingItem()) return;

        int pearlSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.ENDER_PEARL)) {
                pearlSlot = i;
                break;
            }
        }
        if (pearlSlot == -1) return;

        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(pearlSlot);

        if (rotate.getValue() && target != null) {
            Vec3 lookTarget = target.position();
            Vec3 diff = lookTarget.subtract(mc.player.getEyePosition());
            double yaw = Math.toDegrees(Math.atan2(-diff.x, diff.z));
            double pitch = Math.toDegrees(-Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));
            mc.player.setYRot((float) yaw);
            mc.player.setXRot((float) Math.clamp(pitch, -90, 90));
        }

        mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (!keepRotate.getValue()) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
        }
    }

    private int findBestWeaponSlot(Minecraft mc) {
        int bestSlot = -1;
        double bestDamage = -1;
        String weaponModeVal = weaponMode.getValue();

        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            double dmg = getWeaponDamage(stack);
            if (dmg <= 0) continue;

            String name = stack.getItem().toString().toLowerCase();
            boolean isAxe = name.contains("_axe");
            boolean isSword = name.contains("_sword");

            if ("Axe".equals(weaponModeVal) && !isAxe) continue;
            if ("Sword".equals(weaponModeVal) && !isSword) continue;

            if (dmg > bestDamage) {
                bestDamage = dmg;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    private double getWeaponDamage(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        String name = stack.getItem().toString().toLowerCase();
        if (name.contains("netherite_sword")) return 8.0;
        if (name.contains("diamond_sword")) return 7.0;
        if (name.contains("netherite_axe")) return 7.0;
        if (name.contains("mace")) return 6.5;
        if (name.contains("diamond_axe")) return 6.0;
        if (name.contains("iron_sword")) return 6.0;
        if (name.contains("iron_axe")) return 5.0;
        if (name.contains("stone_sword")) return 5.0;
        if (name.contains("stone_axe")) return 4.5;
        if (name.contains("golden_sword") || name.contains("wooden_sword")) return 4.0;
        if (name.contains("golden_axe") || name.contains("wooden_axe")) return 4.0;
        return 0.0;
    }

    private Player findBestTarget(Minecraft mc, List<Player> players) {
        if (players.isEmpty()) return null;
        String mode = targetMode.getValue();

        return switch (mode) {
            case "Health" -> players.stream()
                .min(java.util.Comparator.comparingDouble(p -> p.getHealth() + p.getAbsorptionAmount()))
                .orElse(null);
            case "Crosshair" -> {
                Player closest = null;
                double bestAngle = 180;
                Vec3 lookVec = mc.player.getLookAngle();
                for (Player p : players) {
                    Vec3 toTarget = p.position().add(0, p.getEyeHeight() * 0.5, 0)
                        .subtract(mc.player.getEyePosition()).normalize();
                    double angle = Math.acos(lookVec.dot(toTarget));
                    if (angle < bestAngle) {
                        bestAngle = angle;
                        closest = p;
                    }
                }
                yield closest;
            }
            case "Distance" -> players.stream()
                .min(java.util.Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
                .orElse(null);
            default -> // Nearest
                players.stream()
                    .min(java.util.Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
                    .orElse(null);
        };
    }

    private Vec3 predictLanding(Vec3 pos, Vec3 vel) {
        int ticks = predictTicks.getValue().intValue();
        if (nativeAvailable) {
            try {
                double[] out = new double[7];
                nativePredictPearl(pos.x, pos.y, pos.z, vel.x, vel.y, vel.z, ticks, out);
                return new Vec3(out[0], out[1], out[2]);
            } catch (Exception e) {
                // fallback to java
            }
        }
        return javaPredictLanding(pos, vel, ticks);
    }

    private Vec3 javaPredictLanding(Vec3 pos, Vec3 vel, int ticks) {
        double x = pos.x, y = pos.y, z = pos.z;
        double mx = vel.x, my = vel.y, mz = vel.z;
        for (int t = 0; t < ticks; t++) {
            x += mx; y += my; z += mz;
            my -= 0.03;
            mx *= 0.99; my *= 0.99; mz *= 0.99;
            if (y < -64) break;
        }
        return new Vec3(x, y, z);
    }

    private void updateRenderData(Vec3 moveTarget) {
        renderPearlPos = lastPearlPos;
        renderLandingPos = lastPearlLanding;
        renderTargetPos = moveTarget;
    }

    @Override
    protected void onDisable() {
        trackedPearls.clear();
        target = null;
        targetPos = null;
        lastPearlThrower = null;
        lastPearlLanding = null;
        lastPearlPos = null;
        renderPearlPos = null;
        renderLandingPos = null;
        renderTargetPos = null;
    }

    public void render(Matrix4f modelViewMatrix, Camera camera) {
        if (!getEnabled() || !render.getValue()) return;
        if (renderPearlPos == null && renderLandingPos == null && renderTargetPos == null) return;

        Vec3 camPos = camera.position();
        boolean throughWalls = renderThroughWalls.getValue();
        float lw = lineWidth.getValue().floatValue();

        int lc = lineColor.getValue();
        float lr = ((lc >> 16) & 0xFF) / 255.0f;
        float lg = ((lc >> 8) & 0xFF) / 255.0f;
        float lb = (lc & 0xFF) / 255.0f;

        int pc = pearlColor.getValue();
        float pr = ((pc >> 16) & 0xFF) / 255.0f;
        float pg = ((pc >> 8) & 0xFF) / 255.0f;
        float pb = (pc & 0xFF) / 255.0f;

        int ldc = landingColor.getValue();
        float ldr = ((ldc >> 16) & 0xFF) / 255.0f;
        float ldg = ((ldc >> 8) & 0xFF) / 255.0f;
        float ldb = (ldc & 0xFF) / 255.0f;

        // Line from player to pearl
        if (renderPearlPos != null && renderLine.getValue()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                Vec3 playerPos = mc.player.position();
                Render3DUtils.batchAxisLine(modelViewMatrix,
                    (float) (playerPos.x - camPos.x), (float) (playerPos.y - camPos.y), (float) (playerPos.z - camPos.z),
                    (float) (renderPearlPos.x - camPos.x), (float) (renderPearlPos.y - camPos.y), (float) (renderPearlPos.z - camPos.z),
                    lw, pr, pg, pb, 0.8f, throughWalls);
            }
        }

        // Line from pearl to landing
        if (renderPearlPos != null && renderLandingPos != null && renderPredictionLine.getValue()) {
            Render3DUtils.batchAxisLine(modelViewMatrix,
                (float) (renderPearlPos.x - camPos.x), (float) (renderPearlPos.y - camPos.y), (float) (renderPearlPos.z - camPos.z),
                (float) (renderLandingPos.x - camPos.x), (float) (renderLandingPos.y - camPos.y), (float) (renderLandingPos.z - camPos.z),
                lw, lr, lg, lb, 0.6f, throughWalls);
        }

        // Landing marker
        if (renderLandingPos != null && renderLanding.getValue()) {
            org.joml.Matrix4f landingMat = new org.joml.Matrix4f(modelViewMatrix);
            landingMat.translate(
                (float)(renderLandingPos.x - camPos.x),
                (float)(renderLandingPos.y - camPos.y),
                (float)(renderLandingPos.z - camPos.z));
            Render3DUtils.batchFilledBox(landingMat, 0.3,
                ldr, ldg, ldb, 0.3f, throughWalls);
            Render3DUtils.batchWireframe(landingMat, 0.3,
                ldr, ldg, ldb, 0.9f, lw, throughWalls);
        }

        // Line to target
        if (renderTargetPos != null && renderLine.getValue()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                Vec3 playerPos = mc.player.position();
                Render3DUtils.batchAxisLine(modelViewMatrix,
                    (float) (playerPos.x - camPos.x), (float) (playerPos.y - camPos.y), (float) (playerPos.z - camPos.z),
                    (float) (renderTargetPos.x - camPos.x), (float) (renderTargetPos.y - camPos.y), (float) (renderTargetPos.z - camPos.z),
                    lw * 0.5f, lr, lg, lb, 0.4f, throughWalls);
            }
        }
    }

    private static native void nativePredictPearl(double x, double y, double z, double mx, double my, double mz, int maxTicks, double[] out);

    private static class PearlData {
        int entityId;
        UUID ownerUUID;
        Vec3 position, velocity, landing;
        long time;
        ThrownEnderpearl pearl;

        PearlData(int id, UUID owner, Vec3 pos, Vec3 vel, Vec3 land, long t, ThrownEnderpearl p) {
            this.entityId = id;
            this.ownerUUID = owner;
            this.position = pos;
            this.velocity = vel;
            this.landing = land;
            this.time = t;
            this.pearl = p;
        }
    }
}
