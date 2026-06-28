package ravex.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;

import java.util.ArrayList;
import java.util.List;

/**
 * AutoCrystal — автоматически ставит и подрывает End Crystals.
 *
 * Архитектура:
 *  - Java сторона: сбор данных из мира, вызов native JNI математики, отправка пакетов.
 *  - C++ сторона: точные математические расчёты урона взрыва (см. hooks/autocrystal/).
 *
 * Когда native библиотека не загружена — используется Java fallback.
 */
public class AutoCrystal extends Module {
    public static final AutoCrystal INSTANCE = new AutoCrystal();

    // ── Параметры ─────────────────────────────────────────────────────────────
    public final NumberParameter  placeRange     = new NumberParameter("Place Range",    4.5, 1.0, 6.0, 0.1);
    public final NumberParameter  breakRange     = new NumberParameter("Break Range",    4.5, 1.0, 6.0, 0.1);
    public final NumberParameter  placeDelay     = new NumberParameter("Place Delay",   100, 0, 500, 10);
    public final NumberParameter  breakDelay     = new NumberParameter("Break Delay",    50, 0, 500, 10);
    public final NumberParameter  minDamage      = new NumberParameter("Min Damage",     4.0, 1.0, 20.0, 0.5);
    public final NumberParameter  maxSelfDmg     = new NumberParameter("Max Self Dmg",   8.0, 1.0, 20.0, 0.5);
    public final BooleanParameter antiSuicide    = new BooleanParameter("Anti Suicide",  true);
    public final NumberParameter  antiSuicideMinHp = new NumberParameter("Anti Suicide Min HP", 6.0, 1.0, 20.0, 0.5);
    public final ModeParameter    rotate         = new ModeParameter("Rotate Mode", "Silent",
            java.util.List.of("Silent", "Normal", "Packet", "None"));
    public final ModeParameter    swapMode       = new ModeParameter("Swap Mode", "Silent",
            java.util.List.of("Silent", "Normal", "None"));
    public final NumberParameter  swapDelay      = new NumberParameter("Swap Delay", 0.0, 0.0, 500.0, 10.0);
    public final BooleanParameter onlyInRender   = new BooleanParameter("Only Render",   false);
    public final ModeParameter    targetMode     = new ModeParameter("Target", "Closest",
            java.util.List.of("Closest", "Lowest HP", "Highest Damage"));
    public final ModeParameter    targetType     = new ModeParameter("Target Type", "Players",
            java.util.List.of("Players", "Monsters", "Passives", "All"));
    public final BooleanParameter renderPlacement = new BooleanParameter("Render Placement", true);
    public final BooleanParameter renderDamage    = new BooleanParameter("Render Damage", true);
    public final BooleanParameter armorBreaker    = new BooleanParameter("Armor Breaker", true);
    public final NumberParameter  armorPercent    = new NumberParameter("Armor Percent", 15.0, 1.0, 50.0, 1.0);
    public final NumberParameter  predictTicks    = new NumberParameter("Predict Ticks", 1.0, 0.0, 4.0, 0.1);
    public final BooleanParameter totemDetection  = new BooleanParameter("Totem Detection", true);
    public final NumberParameter  totemMinDamage  = new NumberParameter("Totem Min Damage", 1.5, 0.5, 10.0, 0.5);
    public final NumberParameter  totemSelfMinHp  = new NumberParameter("Totem Self Min HP", 8.0, 2.0, 20.0, 0.5);
    public final ModeParameter    placeMode      = new ModeParameter("Place Mode", "Normal",
            java.util.List.of("Normal", "Aggressive", "Smart"));

    // Rotate sub-settings
    public final NumberParameter  rotateSpeed     = new NumberParameter("Rotate Speed", 180.0, 10.0, 180.0, 5.0);
    public final NumberParameter  rotateRandomize = new NumberParameter("Rotate Randomize", 0.0, 0.0, 3.0, 0.1);

    // Anti Suicide sub-settings
    public final BooleanParameter antiSuicideCheckBreaking = new BooleanParameter("AntiSuicide Break", true);
    public final BooleanParameter antiSuicideIgnoreWithTotem = new BooleanParameter("AntiSuicide Ignore Totem", false);

    // Totem Detection sub-settings
    public final BooleanParameter totemCheckTarget = new BooleanParameter("Totem Check Target", true);
    public final BooleanParameter totemPopSwap     = new BooleanParameter("Totem Pop Swap", false);
    public final NumberParameter  totemPopHp       = new NumberParameter("Totem Pop HP", 6.0, 1.0, 20.0, 0.5);

    // Place sub-settings
    public final NumberParameter  placeWallRange  = new NumberParameter("Place Wall Range", 3.5, 1.0, 6.0, 0.1);
    public final NumberParameter  breakWallRange  = new NumberParameter("Break Wall Range", 3.5, 1.0, 6.0, 0.1);
    public final BooleanParameter placeAirPlace   = new BooleanParameter("Air Place", false);
    public final NumberParameter  placeUnderHp     = new NumberParameter("Place Under HP", 10.0, 0.0, 36.0, 0.5);
    public final BooleanParameter placeMultiPlace  = new BooleanParameter("Multi Place", false);

    // Swaps sub-settings
    public final BooleanParameter swapSwitchBack   = new BooleanParameter("Swap Switch Back", true);
    public final BooleanParameter swapNoGap        = new BooleanParameter("Swap No Gap", true);
    public final BooleanParameter swapInventory   = new BooleanParameter("Swap Inventory", false);

    // Speed & Timings settings
    public final ModeParameter    speedMode      = new ModeParameter("Speed Mode", "Normal",
            java.util.List.of("Legit", "Normal", "Aggressive"));
    public final NumberParameter  jitterDelay    = new NumberParameter("Jitter Delay", 0.0, 0.0, 100.0, 5.0);
    public final BooleanParameter strictRotation = new BooleanParameter("Strict Rotation", false);
    public final NumberParameter  maxRate        = new NumberParameter("Max Rate", 2.0, 1.0, 5.0, 1.0);
    public final BooleanParameter suicide        = new BooleanParameter("Suicide", false);

    // ── Состояние ─────────────────────────────────────────────────────────────
    public static BlockPos currentPlacementBlock = null;
    public static double currentTargetDamage = 0.0;
    public static double currentSelfDamage = 0.0;
    public static int currentTargetTotems = 0;

    // ── Состояние ─────────────────────────────────────────────────────────────
    private long lastPlaceTime = 0;
    private long lastBreakTime = 0;
    private int  lastBreakId   = -1;

    // Native JNI
    private static boolean nativeAvailable = false;

    static {
        try {
            nativeAvailable = ravex.utility.misc.NativeLoader.loadLibrary("ravex_autocrystal");
        } catch (UnsatisfiedLinkError e) {
            // Fallback handled
        }
    }

    // ── JNI методы ────────────────────────────────────────────────────────────
    /**
     * Главный расчёт — возвращает double[16] с результатами.
     * Описание полей см. в autocrystal_jni.h
     */
    private static native double[] nativeTick(
            double pX, double pY, double pZ,
            double pHp, double pAbs,
            double[] pStats,
            double tX, double tY, double tZ,
            double tHp, double tAbs,
            double[] tStats,
            double[] blockData,
            double[] crystalData,
            double placeRange, double placeWallRange,
            double breakRange, double breakWallRange,
            double minTargetDmg, double maxSelfDmg,
            double selfDmgWeight, boolean antiSuicide,
            boolean antiSuicideCheckBreaking, boolean antiSuicideIgnoreWithTotem,
            boolean armorBreaker, double armorPercent,
            double predictTicks, boolean totemDetection,
            boolean totemCheckTarget, boolean placeAirPlace,
            boolean placeMultiPlace, boolean suicide
    );

    /** Расчёт урона одного кристалла (для preview/debug) */
    public static native double[] nativeCalcDamage(
            double expX, double expY, double expZ,
            double entityX, double entityY, double entityZ,
            double entityHp, double entityAbs,
            double[] stats,
            double[] blockData
    );

    private AutoCrystal() {
        super("AutoCrystal", Category.COMBAT);
        addParameter(placeRange);
        addParameter(breakRange);
        addParameter(placeDelay);
        addParameter(breakDelay);
        addParameter(minDamage);
        addParameter(maxSelfDmg);
        addParameter(antiSuicide);
        addParameter(antiSuicideMinHp);
        addParameter(rotate);
        addParameter(swapMode);
        addParameter(swapDelay);
        addParameter(onlyInRender);
        addParameter(targetMode);
        addParameter(targetType);
        addParameter(renderPlacement);
        addParameter(renderDamage);
        addParameter(armorBreaker);
        addParameter(armorPercent);
        addParameter(predictTicks);
        addParameter(totemDetection);
        addParameter(totemMinDamage);
        addParameter(totemSelfMinHp);
        addParameter(placeMode);

        addParameter(rotateSpeed);
        addParameter(rotateRandomize);
        addParameter(antiSuicideCheckBreaking);
        addParameter(antiSuicideIgnoreWithTotem);
        addParameter(totemCheckTarget);
        addParameter(totemPopSwap);
        addParameter(totemPopHp);
        addParameter(placeWallRange);
        addParameter(breakWallRange);
        addParameter(placeAirPlace);
        addParameter(placeUnderHp);
        addParameter(placeMultiPlace);
        addParameter(swapSwitchBack);
        addParameter(swapNoGap);
        addParameter(swapInventory);
        addParameter(speedMode);
        addParameter(jitterDelay);
        addParameter(strictRotation);
        addParameter(maxRate);
        addParameter(suicide);

        // Conditional visibility configuration
        armorPercent.setVisible(() -> armorBreaker.getValue());
        renderDamage.setVisible(() -> renderPlacement.getValue());
        antiSuicideMinHp.setVisible(() -> antiSuicide.getValue());
        totemMinDamage.setVisible(() -> totemDetection.getValue());
        totemSelfMinHp.setVisible(() -> totemDetection.getValue());
        swapDelay.setVisible(() -> !swapMode.getValue().equals("None"));

        rotateSpeed.setVisible(() -> !rotate.getValue().equals("None"));
        rotateRandomize.setVisible(() -> !rotate.getValue().equals("None"));
        antiSuicideCheckBreaking.setVisible(() -> antiSuicide.getValue());
        antiSuicideIgnoreWithTotem.setVisible(() -> antiSuicide.getValue());
        totemCheckTarget.setVisible(() -> totemDetection.getValue());
        totemPopHp.setVisible(() -> totemPopSwap.getValue());
        placeUnderHp.setVisible(() -> !placeMode.getValue().equals("Smart"));
        swapSwitchBack.setVisible(() -> !swapMode.getValue().equals("None"));
        swapNoGap.setVisible(() -> !swapMode.getValue().equals("None"));
        swapInventory.setVisible(() -> !swapMode.getValue().equals("None"));

        jitterDelay.setVisible(() -> !speedMode.getValue().equals("Aggressive"));
        strictRotation.setVisible(() -> !rotate.getValue().equals("None"));
        maxRate.setVisible(() -> !speedMode.getValue().equals("Legit"));
    }

    public static float silentYaw = 0;
    public static float silentPitch = 0;
    private static boolean hasSilentRotations = false;
    private int originalSlot = -1;

    private float lastSilentYaw = 0f;
    private float lastSilentPitch = 0f;
    private boolean lastSilentInit = false;

    public static boolean hasSilentRotations() {
        return hasSilentRotations;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        hasSilentRotations = false;

        // Auto-Totem offhand swap
        if (totemPopSwap.getValue()) {
            double selfHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (selfHp <= totemPopHp.getValue()) {
                ItemStack offhandItem = mc.player.getOffhandItem();
                if (offhandItem.getItem() != Items.TOTEM_OF_UNDYING) {
                    int totemSlot = -1;
                    for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
                        if (mc.player.getInventory().getItem(i).getItem() == Items.TOTEM_OF_UNDYING) {
                            totemSlot = i;
                            break;
                        }
                    }
                    if (totemSlot != -1) {
                        int containerSlot = totemSlot;
                        if (totemSlot < 9) {
                            containerSlot = 36 + totemSlot;
                        }
                        mc.gameMode.handleInventoryMouseClick(
                            mc.player.inventoryMenu.containerId,
                            containerSlot,
                            0,
                            net.minecraft.world.inventory.ClickType.PICKUP,
                            mc.player
                        );
                        mc.gameMode.handleInventoryMouseClick(
                            mc.player.inventoryMenu.containerId,
                            45,
                            0,
                            net.minecraft.world.inventory.ClickType.PICKUP,
                            mc.player
                        );
                        mc.gameMode.handleInventoryMouseClick(
                            mc.player.inventoryMenu.containerId,
                            containerSlot,
                            0,
                            net.minecraft.world.inventory.ClickType.PICKUP,
                            mc.player
                        );
                    }
                }
            }
        }

        // ── Выбор цели ────────────────────────────────────────────────────────
        LivingEntity target = findTarget(mc);
        if (target == null) {
            currentPlacementBlock = null;
            return;
        }

        // ── Сбор данных ───────────────────────────────────────────────────────
        Vec3 playerPos = mc.player.position();
        double pHp  = mc.player.getHealth();
        double pAbs = mc.player.getAbsorptionAmount();
        Vec3 targetPos = target.position();
        double tHp  = target.getHealth();
        double tAbs = target.getAbsorptionAmount();

        // Блоки в радиусе действия (обсидиан + бедрок)
        double[] blockData  = collectValidBlocks(mc, playerPos);
        // Активные кристаллы
        double[] crystalData = collectCrystals(mc, playerPos);

        double[] pStats = getEntityStats(mc.player);
        double[] tStats = getEntityStats(target);

        // ── Расчёт ────────────────────────────────────────────────────────────
        double[] result;
        if (nativeAvailable) {
            result = nativeTick(
                    playerPos.x, playerPos.y, playerPos.z,
                    pHp, pAbs, pStats,
                    targetPos.x, targetPos.y, targetPos.z,
                    tHp, tAbs, tStats,
                    blockData, crystalData,
                    placeRange.getValue(), placeWallRange.getValue(),
                    breakRange.getValue(), breakWallRange.getValue(),
                    minDamage.getValue(), maxSelfDmg.getValue(),
                    1.2, antiSuicide.getValue(),
                    antiSuicideCheckBreaking.getValue(), antiSuicideIgnoreWithTotem.getValue(),
                    armorBreaker.getValue(), armorPercent.getValue(),
                    predictTicks.getValue(), totemDetection.getValue(),
                    totemCheckTarget.getValue(), placeAirPlace.getValue(),
                    placeMultiPlace.getValue(), suicide.getValue()
            );
        } else {
            result = javaFallbackTick(
                    playerPos, pHp, pAbs,
                    targetPos, tHp, tAbs,
                    blockData, crystalData
            );
        }

        if (result == null || result.length < 12) {
            currentPlacementBlock = null;
            return;
        }

        boolean shouldPlace = result[0] > 0.5;
        boolean shouldBreak = result[6] > 0.5;

        // Anti-Suicide HP Check (Ignore if totem is held/present and antiSuicideIgnoreWithTotem is enabled)
        if (shouldPlace && antiSuicide.getValue()) {
            boolean ignoreSuicide = antiSuicideIgnoreWithTotem.getValue() && pStats[14] > 0.0;
            if (!ignoreSuicide) {
                double selfDmg = result[5];
                if (pHp + pAbs - selfDmg < antiSuicideMinHp.getValue()) {
                    shouldPlace = false;
                }
            }
        }

        if (shouldPlace) {
            currentPlacementBlock = new BlockPos((int) result[1], (int) result[2], (int) result[3]);
            currentTargetDamage = result[4];
            currentSelfDamage = result[5];
            currentTargetTotems = (int) tStats[14];
        } else {
            currentPlacementBlock = null;
        }

        long now = System.currentTimeMillis();

        // ── Расчёт цели вращения ──────────────────────────────────────────────
        Vec3 rotationTarget = null;
        if (shouldBreak) {
            int entityId = (int) result[7];
            Entity crystal = mc.level.getEntity(entityId);
            if (crystal instanceof EndCrystal) {
                rotationTarget = crystal.position();
            }
        }
        if (rotationTarget == null && shouldPlace) {
            rotationTarget = new Vec3(result[1] + 0.5, result[2] + 1.0, result[3] + 0.5);
        }

        if (rotationTarget != null) {
            rotateTo(mc, rotationTarget);
        }

        // ── Проверка выравнивания ротации (Strict Rotation) ───────────────────
        boolean isStrict = strictRotation.getValue() || speedMode.getValue().equals("Legit");
        boolean aligned = true;
        if (isStrict && rotationTarget != null) {
            aligned = isRotationAligned(mc, rotationTarget);
        }

        // ── Ограничение скорости (Max Rate) ──────────────────────────────────
        int limit = maxRate.getValue().intValue();
        if (speedMode.getValue().equals("Legit")) {
            limit = 1;
        }
        int actionsThisTick = 0;

        // ── Подрыв ────────────────────────────────────────────────────────────
        boolean checkBreakDelay = true;
        if (speedMode.getValue().equals("Aggressive")) {
            checkBreakDelay = false;
        }

        if (shouldBreak && aligned && actionsThisTick < limit) {
            if (!checkBreakDelay || now - lastBreakTime >= currentBreakDelay) {
                int entityId = (int) result[7];
                if (entityId != lastBreakId) { // не подрываем один и тот же кристалл дважды подряд
                    Entity crystal = mc.level.getEntity(entityId);
                    if (crystal instanceof EndCrystal) {
                        mc.gameMode.attack(mc.player, crystal);
                        mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        lastBreakTime = now;
                        lastBreakId   = entityId;
                        actionsThisTick++;

                        // Рассчитываем следующую задержку подрыва с учётом джиттера
                        double base = breakDelay.getValue();
                        double jitter = (Math.random() - 0.5) * jitterDelay.getValue();
                        currentBreakDelay = Math.max(0, (long)(base + jitter));
                    }
                }
            }
        }

        // ── Размещение ────────────────────────────────────────────────────────
        boolean checkPlaceDelay = true;
        if (speedMode.getValue().equals("Aggressive")) {
            checkPlaceDelay = false;
        } else if (placeMode.getValue().equals("Smart")) {
            if (currentTargetDamage < minDamage.getValue() && currentTargetDamage < target.getHealth() + target.getAbsorptionAmount()) {
                shouldPlace = false;
            }
        }

        if (target != null) {
            double targetEffHp = target.getHealth() + target.getAbsorptionAmount();
            if (targetEffHp <= placeUnderHp.getValue()) {
                checkPlaceDelay = false;
            }
        }

        if (shouldPlace && aligned && actionsThisTick < limit) {
            if (!checkPlaceDelay || now - lastPlaceTime >= currentPlaceDelay) {
                BlockPos placePos = new BlockPos(
                        (int) result[1], (int) result[2], (int) result[3]);

                // Убеждаемся, что в руке End Crystal
                boolean hasItem = switchToCrystal(mc);
                if (hasItem) {
                    // Отправляем пакет размещения
                    net.minecraft.world.phys.Vec3 hitVec = new net.minecraft.world.phys.Vec3(
                            result[1] + 0.5, result[2] + 1.0, result[3] + 0.5);
                    net.minecraft.core.Direction face = net.minecraft.core.Direction.UP;
                    BlockHitResult hitResult = new BlockHitResult(hitVec, face, placePos, false);

                    mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hitResult);
                    mc.player.swing(mc.player.getUsedItemHand());

                    // If silent swap was performed, restore the original slot!
                    if (swapMode.getValue().equals("Silent") && swapSwitchBack.getValue() && originalSlot != -1) {
                        if (mc.player.connection != null) {
                            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(originalSlot));
                        }
                        originalSlot = -1;
                    }

                    lastPlaceTime = now;
                    actionsThisTick++;

                    // Рассчитываем следующую задержку установки с учётом джиттера
                    double base = placeDelay.getValue();
                    double jitter = (Math.random() - 0.5) * jitterDelay.getValue();
                    currentPlaceDelay = Math.max(0, (long)(base + jitter));
                }
            }
        }

        // Multi-placement support (place second crystal if returned by JNI)
        boolean shouldPlace2 = placeMultiPlace.getValue() && result.length >= 16 && result[12] > 0.5;
        if (shouldPlace2 && antiSuicide.getValue()) {
            boolean ignoreSuicide2 = antiSuicideIgnoreWithTotem.getValue() && pStats[14] > 0.0;
            if (!ignoreSuicide2) {
                double selfDmg2 = result[17];
                if (pHp + pAbs - selfDmg2 < antiSuicideMinHp.getValue()) {
                    shouldPlace2 = false;
                }
            }
        }

        if (shouldPlace2 && aligned && actionsThisTick < limit) {
            if (!checkPlaceDelay || now - lastPlaceTime >= currentPlaceDelay) {
                BlockPos placePos2 = new BlockPos((int) result[13], (int) result[14], (int) result[15]);

                boolean hasItem = switchToCrystal(mc);
                if (hasItem) {
                    net.minecraft.world.phys.Vec3 hitVec2 = new net.minecraft.world.phys.Vec3(
                            result[13] + 0.5, result[14] + 1.0, result[15] + 0.5);
                    net.minecraft.core.Direction face = net.minecraft.core.Direction.UP;
                    BlockHitResult hitResult2 = new BlockHitResult(hitVec2, face, placePos2, false);

                    mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hitResult2);
                    mc.player.swing(mc.player.getUsedItemHand());

                    if (swapMode.getValue().equals("Silent") && swapSwitchBack.getValue() && originalSlot != -1) {
                        if (mc.player.connection != null) {
                            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(originalSlot));
                        }
                        originalSlot = -1;
                    }

                    lastPlaceTime = now;
                    actionsThisTick++;

                    double base = placeDelay.getValue();
                    double jitter = (Math.random() - 0.5) * jitterDelay.getValue();
                    currentPlaceDelay = Math.max(0, (long)(base + jitter));
                }
            }
        }

        // Update silent rotation init flag if no rotations were done
        if (!hasSilentRotations) {
            lastSilentInit = false;
        }
    }

    // ── Выбор цели ────────────────────────────────────────────────────────────
    private LivingEntity findTarget(Minecraft mc) {
        LivingEntity closest = null;
        double bestMetric = Double.MAX_VALUE;

        double maxDist = Math.max(placeRange.getValue(), breakRange.getValue()) + 2.0;
        String mode = targetMode.getValue();
        String typeFilter = targetType.getValue();

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le == mc.player) continue;
            if (le.isDeadOrDying()) continue;

            // Apply type filtering
            if (typeFilter.equals("Players")) {
                if (!(le instanceof Player)) continue;
            } else if (typeFilter.equals("Monsters")) {
                if (!(le instanceof net.minecraft.world.entity.monster.Monster || le instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || le instanceof net.minecraft.world.entity.boss.wither.WitherBoss)) continue;
            } else if (typeFilter.equals("Passives")) {
                if (le instanceof Player || le instanceof net.minecraft.world.entity.monster.Monster || le instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || le instanceof net.minecraft.world.entity.boss.wither.WitherBoss) continue;
            }

            double dist = mc.player.distanceTo(le);
            if (dist > maxDist) continue;

            double metric = switch (mode) {
                case "Closest"        -> dist;
                case "Lowest HP"      -> le.getHealth();
                case "Highest Damage" -> -calcQuickDamage(mc, le);
                default               -> dist;
            };

            if (metric < bestMetric) {
                bestMetric = metric;
                closest = le;
            }
        }
        return closest;
    }

    // ── Сбор валидных блоков (обсидиан + бедрок) ──────────────────────────────
    private double[] collectValidBlocks(Minecraft mc, Vec3 playerPos) {
        List<Double> data = new ArrayList<>();
        int r = (int) Math.ceil(placeRange.getValue()) + 1;
        BlockPos origin = BlockPos.containing(playerPos);

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK)) {
                        // Блок над должен быть пустым
                        BlockState above = mc.level.getBlockState(pos.above());
                        BlockState above2 = mc.level.getBlockState(pos.above(2));
                        if (above.isAir() && above2.isAir()) {
                            data.add((double) pos.getX());
                            data.add((double) pos.getY());
                            data.add((double) pos.getZ());
                        }
                    }
                }
            }
        }

        // AutoCrystalSync prediction integration
        if (BasePlace.INSTANCE.getEnabled() && BasePlace.INSTANCE.autoCrystalSync.getValue() && BasePlace.lastPlacedBase != null) {
            long msLimit = (long) (BasePlace.INSTANCE.syncPredictTicks.getValue() * 50);
            if (System.currentTimeMillis() - BasePlace.lastPlacedTime <= msLimit) {
                BlockPos predictedPos = BasePlace.lastPlacedBase;
                double dist = Math.sqrt(predictedPos.distToCenterSqr(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ()));
                if (dist <= placeRange.getValue()) {
                    boolean alreadyAdded = false;
                    for (int i = 0; i < data.size(); i += 3) {
                        if (data.get(i) == predictedPos.getX() &&
                            data.get(i+1) == predictedPos.getY() &&
                            data.get(i+2) == predictedPos.getZ()) {
                            alreadyAdded = true;
                            break;
                        }
                    }
                    if (!alreadyAdded) {
                        data.add((double) predictedPos.getX());
                        data.add((double) predictedPos.getY());
                        data.add((double) predictedPos.getZ());
                    }
                }
            }
        }


        double[] arr = new double[data.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = data.get(i);
        return arr;

    }

    // ── Сбор активных кристаллов ─────────────────────────────────────────────
    private double[] collectCrystals(Minecraft mc, Vec3 playerPos) {
        List<Double> data = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof EndCrystal)) continue;
            if (mc.player.distanceTo(e) > breakRange.getValue() + 2.0) continue;
            data.add((double) e.getId());
            data.add(e.getX());
            data.add(e.getY());
            data.add(e.getZ());
        }
        double[] arr = new double[data.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = data.get(i);
        return arr;
    }

    // ── Переключение на End Crystal в руке ───────────────────────────────────
    private boolean switchToCrystal(Minecraft mc) {
        ItemStack mainHand = mc.player.getMainHandItem();
        if (mainHand.getItem() == Items.END_CRYSTAL) return true;

        String mode = swapMode.getValue();
        if (mode.equals("None")) return false;

        // swapNoGap check
        if (swapNoGap.getValue() && mc.player.isUsingItem()) {
            ItemStack usingItem = mc.player.getUseItem();
            if (usingItem.getItem() == Items.GOLDEN_APPLE || usingItem.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                return false;
            }
        }

        // Ищем в хотбаре
        for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getItem(slot).getItem() == Items.END_CRYSTAL) {
                if (mode.equals("Normal")) {
                    mc.player.getInventory().setSelectedSlot(slot);
                } else if (mode.equals("Silent")) {
                    originalSlot = mc.player.getInventory().getSelectedSlot();
                    if (mc.player.connection != null) {
                        mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(slot));
                    }
                }
                return true;
            }
        }

        // Ищем во всем инвентаре, если включено swapInventory
        if (swapInventory.getValue()) {
            for (int slot = 9; slot < 36; slot++) {
                if (mc.player.getInventory().getItem(slot).getItem() == Items.END_CRYSTAL) {
                    // Свапаем с 0-м слотом хотбара (InventoryMenu slot index = 36)
                    int targetHotbarSlot = 0;
                    mc.gameMode.handleInventoryMouseClick(
                        mc.player.containerMenu.containerId,
                        slot,
                        targetHotbarSlot,
                        net.minecraft.world.inventory.ClickType.SWAP,
                        mc.player
                    );

                    if (mode.equals("Normal")) {
                        mc.player.getInventory().setSelectedSlot(targetHotbarSlot);
                    } else if (mode.equals("Silent")) {
                        originalSlot = mc.player.getInventory().getSelectedSlot();
                        if (mc.player.connection != null) {
                            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(targetHotbarSlot));
                        }
                    }
                    return true;
                }
            }
        }

        return false;
    }

    // ── Поворот к цели ────────────────────────────────────────────────────────
    private void rotateTo(Minecraft mc, Vec3 target) {
        String mode = rotate.getValue();
        if (mode.equals("None")) return;

        Vec3 eyes = mc.player.getEyePosition();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx*dx + dz*dz);
        float targetYaw   = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();

        if (mode.equals("Silent")) {
            if (!lastSilentInit) {
                lastSilentYaw = currentYaw;
                lastSilentPitch = currentPitch;
                lastSilentInit = true;
            }
            currentYaw = lastSilentYaw;
            currentPitch = lastSilentPitch;
        }

        float maxSpeed = rotateSpeed.getValue().floatValue();
        float diffYaw = net.minecraft.util.Mth.wrapDegrees(targetYaw - currentYaw);
        float diffPitch = targetPitch - currentPitch;

        if (Math.abs(diffYaw) > maxSpeed) {
            diffYaw = Math.signum(diffYaw) * maxSpeed;
        }
        if (Math.abs(diffPitch) > maxSpeed) {
            diffPitch = Math.signum(diffPitch) * maxSpeed;
        }

        float finalYaw = currentYaw + diffYaw;
        float finalPitch = currentPitch + diffPitch;

        if (rotateRandomize.getValue() > 0.0) {
            float rand = rotateRandomize.getValue().floatValue();
            finalYaw += (float) ((Math.random() - 0.5) * rand);
            finalPitch += (float) ((Math.random() - 0.5) * rand);
        }

        if (mode.equals("Normal")) {
            mc.player.setYRot(finalYaw);
            mc.player.setXRot(finalPitch);
        } else if (mode.equals("Silent")) {
            silentYaw = finalYaw;
            silentPitch = finalPitch;
            hasSilentRotations = true;
            lastSilentYaw = finalYaw;
            lastSilentPitch = finalPitch;
        } else if (mode.equals("Packet")) {
            if (mc.player.connection != null) {
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(finalYaw, finalPitch, mc.player.onGround(), mc.player.horizontalCollision));
            }
        }
    }

    private long currentPlaceDelay = 0;
    private long currentBreakDelay = 0;

    private boolean isRotationAligned(Minecraft mc, Vec3 target) {
        if (rotate.getValue().equals("None")) return true;

        Vec3 eyes = mc.player.getEyePosition();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx*dx + dz*dz);
        float targetYaw   = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();

        if (rotate.getValue().equals("Silent")) {
            if (lastSilentInit) {
                currentYaw = lastSilentYaw;
                currentPitch = lastSilentPitch;
            }
        }

        float diffYaw = Math.abs(net.minecraft.util.Mth.wrapDegrees(targetYaw - currentYaw));
        float diffPitch = Math.abs(targetPitch - currentPitch);

        return diffYaw <= 10.0f && diffPitch <= 10.0f;
    }

    // ── Быстрая оценка урона (для выбора цели) ────────────────────────────────
    private double calcQuickDamage(Minecraft mc, LivingEntity target) {
        Vec3 playerPos = mc.player.position();
        Vec3 targetPos = target.position();
        // Оцениваем позицию прямо над ногами цели
        Vec3 crystalPos = targetPos.add(0, 1, 0);
        double dist = playerPos.distanceTo(crystalPos);
        if (dist > 12.0) return 0;
        // Упрощённая оценка без блоков
        double impact = Math.max(0, (1.0 - dist / 12.0));
        return (impact * impact + impact) / 2.0 * 84.0 + 1.0;
    }

    // ── Java Fallback (когда native не загружен) ──────────────────────────────
    private double[] javaFallbackTick(
            Vec3 playerPos, double pHp, double pAbs,
            Vec3 targetPos, double tHp, double tAbs,
            double[] blockData, double[] crystalData) {
        double[] result = new double[12];

        // Простейшая реализация — ищем ближайший кристалл для подрыва
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return result;

        double bestBreakDmg = 0;
        int bestId = -1;
        Vec3 bestPos = Vec3.ZERO;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof EndCrystal)) continue;
            double dist = mc.player.distanceTo(e);
            if (dist > breakRange.getValue()) continue;

            // Упрощённый расчёт урона
            Vec3 cp = e.position();
            double tdist = cp.distanceTo(targetPos);
            double sdist = cp.distanceTo(playerPos);
            if (tdist > 12 || sdist > 12) continue;

            double tImpact = Math.max(0, (1.0 - tdist / 12.0));
            double sImpact = Math.max(0, (1.0 - sdist / 12.0));
            double tDmg = (tImpact * tImpact + tImpact) / 2.0 * 84.0 + 1.0;
            double sDmg = (sImpact * sImpact + sImpact) / 2.0 * 84.0 + 1.0;

            if (tDmg < minDamage.getValue()) continue;
            if (!suicide.getValue()) {
                if (sDmg > maxSelfDmg.getValue()) continue;
                if (antiSuicide.getValue() && pHp + pAbs - sDmg <= 0) continue;
            }

            double score = suicide.getValue() ? (sDmg * 100.0 + tDmg) : tDmg;
            if (score > bestBreakDmg) {
                bestBreakDmg = score;
                bestId = e.getId();
                bestPos = e.position();
            }
        }

        if (bestId != -1) {
            result[6] = 1.0;
            result[7] = bestId;
            result[8] = bestPos.x;
            result[9] = bestPos.y;
            result[10] = bestPos.z;
            result[11] = bestBreakDmg;
        }

        return result;
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }

    private double[] getEntityStats(LivingEntity player) {
        int protectionEpf = 0;
        int blastProtectionEpf = 0;
        net.minecraft.world.entity.EquipmentSlot[] armorSlots = {
            net.minecraft.world.entity.EquipmentSlot.FEET,
            net.minecraft.world.entity.EquipmentSlot.LEGS,
            net.minecraft.world.entity.EquipmentSlot.CHEST,
            net.minecraft.world.entity.EquipmentSlot.HEAD
        };
        for (net.minecraft.world.entity.EquipmentSlot slot : armorSlots) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty()) continue;
            ItemEnchantments enchants = armor.get(DataComponents.ENCHANTMENTS);
            if (enchants != null) {
                for (var enchantment : enchants.keySet()) {
                    String id = enchantment.getRegisteredName().toLowerCase();
                    int level = enchants.getLevel(enchantment);
                    if (id.contains("blast_protection")) {
                        blastProtectionEpf += level * 2;
                    } else if (id.equals("minecraft:protection") || id.endsWith(":protection")) {
                        protectionEpf += level;
                    }
                }
            }
        }

        // Позиция 14: количество тотемов
        int totems = 0;
        if (player.getMainHandItem().getItem() == Items.TOTEM_OF_UNDYING) totems++;
        if (player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) totems++;
        if (player instanceof Player p) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                if (p.getInventory().getItem(i).getItem() == Items.TOTEM_OF_UNDYING) {
                    totems++;
                }
            }
        }
        
        double[] stats = new double[15];
        stats[0] = player.getArmorValue();
        var attrToughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        stats[1] = attrToughness != null ? attrToughness.getValue() : 0.0;
        stats[2] = blastProtectionEpf;
        stats[3] = protectionEpf;
        
        var resEffect = player.getEffect(MobEffects.RESISTANCE);
        stats[4] = resEffect != null ? resEffect.getAmplifier() + 1 : 0;
        
        var weakEffect = player.getEffect(MobEffects.WEAKNESS);
        stats[5] = weakEffect != null ? weakEffect.getAmplifier() + 1 : 0;
        
        var strEffect = player.getEffect(MobEffects.STRENGTH);
        stats[6] = strEffect != null ? strEffect.getAmplifier() + 1 : 0;

        int idx = 7;
        for (net.minecraft.world.entity.EquipmentSlot slot : armorSlots) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty()) {
                stats[idx++] = 0.0;
            } else if (!armor.isDamageableItem()) {
                stats[idx++] = 100.0;
            } else {
                double dur = (1.0 - (double) armor.getDamageValue() / armor.getMaxDamage()) * 100.0;
                stats[idx++] = dur;
            }
        }
        
        net.minecraft.world.phys.Vec3 motion = player.getDeltaMovement();
        if (motion != null) {
            stats[11] = motion.x;
            stats[12] = motion.y;
            stats[13] = motion.z;
        } else {
            stats[11] = 0.0;
            stats[12] = 0.0;
            stats[13] = 0.0;
        }
        stats[14] = totems;
        
        return stats;
    }
}
