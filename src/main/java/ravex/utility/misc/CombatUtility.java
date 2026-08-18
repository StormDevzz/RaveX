package ravex.utility.misc;

import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class CombatUtility {

    public static double[] getEntityStats(LivingEntity entity) {
        int protectionEpf = 0;
        int blastProtectionEpf = 0;
        EquipmentSlot[] armorSlots = {
            EquipmentSlot.FEET,
            EquipmentSlot.LEGS,
            EquipmentSlot.CHEST,
            EquipmentSlot.HEAD
        };
        for (EquipmentSlot slot : armorSlots) {
            var armor = entity.getItemBySlot(slot);
            if (armor.isEmpty()) continue;
            var enchants = InventoryUtility.getEnchantments(armor);
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
        int totems = 0;
        if (InventoryUtility.isTotem(entity.getMainHandItem())) totems++;
        if (InventoryUtility.isTotem(entity.getOffhandItem())) totems++;
        if (entity instanceof Player p) {
            totems += InventoryUtility.countItem(p, "totem_of_undying");
        }
        double[] stats = new double[15];
        stats[0] = entity.getArmorValue();
        stats[1] = PotionUtility.getArmorToughness(entity);
        stats[2] = blastProtectionEpf;
        stats[3] = protectionEpf;
        stats[4] = PotionUtility.getResistanceAmplifier(entity);
        stats[5] = PotionUtility.getWeaknessAmplifier(entity);
        stats[6] = PotionUtility.getStrengthAmplifier(entity);
        int idx = 7;
        for (EquipmentSlot slot : armorSlots) {
            var armor = entity.getItemBySlot(slot);
            if (armor.isEmpty()) {
                stats[idx++] = 0.0;
            } else if (!armor.isDamageableItem()) {
                stats[idx++] = 100.0;
            } else {
                double dur = (1.0 - (double) armor.getDamageValue() / armor.getMaxDamage()) * 100.0;
                stats[idx++] = dur;
            }
        }
        Vec3 motion = entity.getDeltaMovement();
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

    public static double getWeaponDamage(ItemStack stack) {
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
        else if (name.contains("golden_axe") || name.contains("wooden_axe")) dmg = 4.0;
        return dmg;
    }

    public static double[] collectSolidBlocks(MinecraftWrapper mc, int range) {
        List<Double> data = new ArrayList<>();
        var playerPos = mc.getPlayer().blockPosition();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    var pos = playerPos.offset(dx, dy, dz);
                    if (mc.getLevel().isLoaded(pos)) {
                        var state = mc.getLevel().getBlockState(pos);
                        if (!state.isAir() && !state.liquid()) {
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

    public static void rotateTo(MinecraftWrapper mc, Vec3 target, float speed, float randomize, SilentRotationUtility silentRotation) {
        float[] targetAngles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), target);
        float currentYaw = mc.getPlayer().getYRot();
        float currentPitch = mc.getPlayer().getXRot();
        if (!silentRotation.initialized) {
            silentRotation.init(currentYaw, currentPitch);
        }
        currentYaw = silentRotation.lastYaw;
        currentPitch = silentRotation.lastPitch;
        float[] limited = AimUtility.limitAngles(currentYaw, targetAngles[0], currentPitch, targetAngles[1], speed);
        float finalYaw = limited[0], finalPitch = limited[1];
        if (randomize > 0.0f) {
            float[] rnd = AimUtility.randomize(finalYaw, finalPitch, randomize);
            finalYaw = rnd[0];
            finalPitch = rnd[1];
        }
        silentRotation.set(finalYaw, finalPitch);
        silentRotation.lastYaw = finalYaw;
        silentRotation.lastPitch = finalPitch;
    }

    public static void rotateToNCPVanillaLegit(MinecraftWrapper mc, Vec3 target, String mode, SilentRotationUtility silentRotation) {
        float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), target);
        float currentYaw = mc.getPlayer().getYRot();
        float currentPitch = mc.getPlayer().getXRot();
        if (mode.equals("NCP")) {
            if (!silentRotation.initialized) {
                silentRotation.init(currentYaw, currentPitch);
            }
            currentYaw = silentRotation.lastYaw;
            currentPitch = silentRotation.lastPitch;
            float[] limited = AimUtility.limitAngles(currentYaw, RotationUtility.fixAngle(angles[0]), currentPitch, RotationUtility.fixAngle(angles[1]), 180.0f);
            silentRotation.set(limited[0], limited[1]);
            silentRotation.lastYaw = limited[0];
            silentRotation.lastPitch = limited[1];
        } else if (mode.equals("Vanilla")) {
            mc.getPlayer().setYRot(angles[0]);
            mc.getPlayer().setXRot(angles[1]);
        } else if (mode.equals("Legit")) {
            float maxSpeed = 90.0f;
            float[] limited = AimUtility.limitAngles(currentYaw, angles[0], currentPitch, angles[1], maxSpeed);
            limited = AimUtility.randomize(limited[0], limited[1], 1.5f);
            mc.getPlayer().setYRot(limited[0]);
            mc.getPlayer().setXRot(limited[1]);
        }
    }
}
