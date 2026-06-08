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
    public final BooleanParameter rotate         = new BooleanParameter("Rotate",        true);
    public final BooleanParameter placeSwitch    = new BooleanParameter("Place Switch",  true);
    public final BooleanParameter onlyInRender   = new BooleanParameter("Only Render",   false);
    public final ModeParameter    targetMode     = new ModeParameter("Target", "Closest",
            java.util.List.of("Closest", "Lowest HP", "Highest Damage"));
    public final BooleanParameter renderPlacement = new BooleanParameter("Render Placement", true);
    public final BooleanParameter renderDamage    = new BooleanParameter("Render Damage", true);
    public final BooleanParameter armorBreaker    = new BooleanParameter("Armor Breaker", true);
    public final NumberParameter  armorPercent    = new NumberParameter("Armor Percent", 15.0, 1.0, 50.0, 1.0);
    public final NumberParameter  predictTicks    = new NumberParameter("Predict Ticks", 1.0, 0.0, 4.0, 0.1);
    public final BooleanParameter totemDetection  = new BooleanParameter("Totem Detection", true);

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
            // Пытаемся загрузить нативную библиотеку
            System.loadLibrary("ravex_autocrystal");
            nativeAvailable = true;
        } catch (UnsatisfiedLinkError e) {
            // Попытка загрузить из ресурсов JAR
            try {
                String libName = System.getProperty("os.name").toLowerCase().contains("win")
                        ? "ravex_autocrystal.dll" : "libravex_autocrystal.so";
                java.io.InputStream is = AutoCrystal.class.getResourceAsStream(
                        "/assets/ravex/natives/" + libName);
                if (is != null) {
                    java.nio.file.Path tmp = java.nio.file.Files.createTempFile("ravex_ac", "");
                    java.nio.file.Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.load(tmp.toAbsolutePath().toString());
                    tmp.toFile().deleteOnExit();
                    nativeAvailable = true;
                }
            } catch (Throwable ignored) {}
        }
    }

    // ── JNI методы ────────────────────────────────────────────────────────────
    /**
     * Главный расчёт — возвращает double[12] с результатами.
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
            double placeRange, double breakRange,
            double minTargetDmg, double maxSelfDmg,
            double selfDmgWeight, boolean antiSuicide,
            boolean armorBreaker, double armorPercent,
            double predictTicks, boolean totemDetection
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
        addParameter(rotate);
        addParameter(placeSwitch);
        addParameter(onlyInRender);
        addParameter(targetMode);
        addParameter(renderPlacement);
        addParameter(renderDamage);
        addParameter(armorBreaker);
        addParameter(armorPercent);
        addParameter(predictTicks);
        addParameter(totemDetection);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        // ── Выбор цели ────────────────────────────────────────────────────────
        Player target = findTarget(mc);
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
                    placeRange.getValue(), breakRange.getValue(),
                    minDamage.getValue(), maxSelfDmg.getValue(),
                    1.2, antiSuicide.getValue(),
                    armorBreaker.getValue(), armorPercent.getValue(),
                    predictTicks.getValue(), totemDetection.getValue()
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

        if (shouldPlace) {
            currentPlacementBlock = new BlockPos((int) result[1], (int) result[2], (int) result[3]);
            currentTargetDamage = result[4];
            currentSelfDamage = result[5];
            currentTargetTotems = (int) tStats[14];
        } else {
            currentPlacementBlock = null;
        }

        long now = System.currentTimeMillis();

        // ── Подрыв ────────────────────────────────────────────────────────────
        if (shouldBreak && now - lastBreakTime >= breakDelay.getValue()) {
            int entityId = (int) result[7];
            if (entityId != lastBreakId) { // не подрываем один и тот же кристалл дважды подряд
                Entity crystal = mc.level.getEntity(entityId);
                if (crystal instanceof EndCrystal) {
                    if (rotate.getValue()) lookAt(mc, crystal.position());
                    mc.gameMode.attack(mc.player, crystal);
                    mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    lastBreakTime = now;
                    lastBreakId   = entityId;
                }
            }
        }

        // ── Размещение ────────────────────────────────────────────────────────
        if (shouldPlace && now - lastPlaceTime >= placeDelay.getValue()) {
            BlockPos placePos = new BlockPos(
                    (int) result[1], (int) result[2], (int) result[3]);

            // Убеждаемся, что в руке End Crystal
            boolean hasItem = switchToCrystal(mc);
            if (!hasItem) return;

            // Направление взгляда на блок
            if (rotate.getValue()) {
                lookAt(mc, new Vec3(result[1] + 0.5, result[2] + 1.0, result[3] + 0.5));
            }

            // Отправляем пакет размещения
            net.minecraft.world.phys.Vec3 hitVec = new net.minecraft.world.phys.Vec3(
                    result[1] + 0.5, result[2] + 1.0, result[3] + 0.5);
            net.minecraft.core.Direction face = net.minecraft.core.Direction.UP;
            BlockHitResult hitResult = new BlockHitResult(hitVec, face, placePos, false);

            mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hitResult);
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            lastPlaceTime = now;
        }
    }

    // ── Выбор цели ────────────────────────────────────────────────────────────
    private Player findTarget(Minecraft mc) {
        Player closest = null;
        double bestMetric = Double.MAX_VALUE;

        double maxDist = Math.max(placeRange.getValue(), breakRange.getValue()) + 2.0;
        String mode = targetMode.getValue();

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof Player p)) continue;
            if (p == mc.player) continue;
            if (p.isDeadOrDying()) continue;

            double dist = mc.player.distanceTo(p);
            if (dist > maxDist) continue;

            double metric = switch (mode) {
                case "Closest"        -> dist;
                case "Lowest HP"      -> p.getHealth();
                case "Highest Damage" -> -calcQuickDamage(mc, p);
                default               -> dist;
            };

            if (metric < bestMetric) {
                bestMetric = metric;
                closest = p;
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

        if (!placeSwitch.getValue()) return false;

        // Ищем в хотбаре
        for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getItem(slot).getItem() == Items.END_CRYSTAL) {
                mc.player.getInventory().setSelectedSlot(slot);
                return true;
            }
        }
        return false;
    }

    // ── Поворот к цели ────────────────────────────────────────────────────────
    private void lookAt(Minecraft mc, Vec3 target) {
        Vec3 eyes = mc.player.getEyePosition();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx*dx + dz*dz);
        float yaw   = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
    }

    // ── Быстрая оценка урона (для выбора цели) ────────────────────────────────
    private double calcQuickDamage(Minecraft mc, Player target) {
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
            if (sDmg > maxSelfDmg.getValue()) continue;
            if (antiSuicide.getValue() && pHp + pAbs - sDmg <= 0) continue;

            if (tDmg > bestBreakDmg) {
                bestBreakDmg = tDmg;
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

    private double[] getEntityStats(Player player) {
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
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() == Items.TOTEM_OF_UNDYING) {
                totems++;
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
