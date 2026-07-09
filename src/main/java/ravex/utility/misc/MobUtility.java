package ravex.utility.misc;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class MobUtility {
    public static boolean isHostile(LivingEntity entity) {
        return entity instanceof Monster || entity instanceof EnderDragon || entity instanceof WitherBoss;
    }

    public static boolean isPassive(LivingEntity entity) {
        return entity instanceof Animal || entity instanceof AbstractVillager || entity instanceof AmbientCreature;
    }

    public static boolean isPlayer(LivingEntity entity) {
        return entity instanceof Player;
    }

    public static boolean isSelf(LivingEntity entity) {
        return entity == Minecraft.getInstance().player;
    }

    public static LivingEntity asLivingEntity(Entity entity) {
        return entity instanceof LivingEntity living ? living : null;
    }

    public static boolean isAlive(LivingEntity entity) {
        return entity != null && entity.isAlive();
    }

    public static double getHealth(LivingEntity entity) {
        return entity.getHealth();
    }

    public static double getMaxHealth(LivingEntity entity) {
        return entity.getMaxHealth();
    }

    public static float getHealthPercent(LivingEntity entity) {
        return entity.getHealth() / entity.getMaxHealth();
    }

    public static double distanceTo(LivingEntity from, LivingEntity to) {
        return from.distanceTo(to);
    }

    public static double distanceToPlayer(LivingEntity entity) {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null ? p.distanceTo(entity) : Double.MAX_VALUE;
    }

    public static boolean hasItem(LivingEntity entity, net.minecraft.world.item.Item item) {
        if (entity instanceof Player p) {
            for (int i = 0; i < 36; i++) {
                if (p.getInventory().getItem(i).is(item)) return true;
            }
        }
        return entity.getMainHandItem().is(item) || entity.getOffhandItem().is(item);
    }

    public static boolean isHoldingSword(LivingEntity entity) {
        String main = entity.getMainHandItem().getItem().toString().toLowerCase();
        String off = entity.getOffhandItem().getItem().toString().toLowerCase();
        return main.contains("sword") || off.contains("sword");
    }

    public static boolean isHoldingTool(LivingEntity entity) {
        String main = entity.getMainHandItem().getItem().toString().toLowerCase();
        return main.contains("pickaxe") || main.contains("axe") || main.contains("shovel") || main.contains("hoe");
    }

    public static boolean isHoldingTotem(LivingEntity entity) {
        return entity.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
            || entity.getMainHandItem().is(Items.TOTEM_OF_UNDYING);
    }

    public static boolean isArmorStand(LivingEntity entity) {
        return entity instanceof ArmorStand;
    }

    public static boolean isWearingArmor(LivingEntity entity) {
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.HUMANOID_ARMOR) {
                if (!entity.getItemBySlot(slot).isEmpty()) return true;
            }
        }
        return false;
    }
<<<<<<< HEAD

    public static boolean isMountable(Entity entity) {
        if (entity instanceof net.minecraft.world.entity.animal.equine.Horse) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.Donkey) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.Mule) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.SkeletonHorse) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.ZombieHorse) return true;
        if (entity instanceof net.minecraft.world.entity.animal.equine.Llama) return true;
        if (entity instanceof net.minecraft.world.entity.animal.pig.Pig pig && pig.isSaddled()) return true;
        if (entity instanceof net.minecraft.world.entity.monster.Strider strider && strider.isSaddled()) return true;
        return false;
    }

    public static boolean isVehicle(Entity entity) {
        return entity != null && entity.isVehicle();
    }

    public static void interact(Minecraft mc, Entity target) {
        if (mc.player != null && mc.gameMode != null) {
            mc.gameMode.interact(mc.player, target, net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    public static boolean isShearable(net.minecraft.world.entity.animal.sheep.Sheep sheep) {
        return sheep.isAlive() && !sheep.isBaby() && !sheep.isSheared();
    }

    public static boolean isAdultSheep(Entity entity) {
        return entity instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep && isShearable(sheep);
    }

    public static boolean isVillager(Entity entity) {
        return entity instanceof net.minecraft.world.entity.npc.villager.AbstractVillager v && v.isAlive() && !v.isBaby();
    }

    public static boolean isNameable(Entity entity) {
        return entity instanceof LivingEntity living && !(entity instanceof Player)
            && !(entity instanceof net.minecraft.world.entity.decoration.ArmorStand) && living.isAlive();
    }

    public static boolean hasName(Entity entity, String name) {
        var cn = entity.getCustomName();
        return cn != null && cn.getString().equals(name);
    }

    public static boolean isDead(LivingEntity entity) {
        return entity == null || entity.isDeadOrDying();
    }

    public static double getAbsorption(LivingEntity entity) {
        return entity.getAbsorptionAmount();
    }

    public static double getHealthWithAbsorption(LivingEntity entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    public static void attack(Minecraft mc, Entity target) {
        if (mc.player != null && mc.gameMode != null) {
            mc.gameMode.attack(mc.player, target);
        }
    }

    public static void swingHand(Minecraft mc) {
        if (mc.player != null) {
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    public static boolean isMob(Entity entity) {
        return entity instanceof net.minecraft.world.entity.Mob;
    }

    public static double distanceToPlayer(Entity entity) {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null ? p.distanceTo(entity) : Double.MAX_VALUE;
    }

    public static String getOwnerName(Entity entity, boolean displayUuid) {
        if (!(entity instanceof net.minecraft.world.entity.OwnableEntity owned)) return null;
        java.util.UUID uuid = null;
        try {
            for (var m : owned.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType() == java.util.UUID.class) {
                    uuid = (java.util.UUID) m.invoke(owned);
                    if (uuid != null) break;
                }
            }
        } catch (Exception ignored) {}
        if (uuid == null) {
            try {
                var owner = owned.getOwner();
                if (owner != null) uuid = owner.getUUID();
            } catch (Exception ignored) {}
        }
        if (uuid == null) return null;
        if (displayUuid) return uuid.toString();
        try {
            var owner = owned.getOwner();
            if (owner != null) return owner.getScoreboardName();
        } catch (Exception ignored) {}
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var conn = mc.getConnection();
            if (conn != null) {
                var info = conn.getClass().getMethod("getPlayerInfo", java.util.UUID.class).invoke(conn, uuid);
                if (info != null) {
                    var profile = info.getClass().getMethod("getProfile").invoke(info);
                    if (profile != null)
                        return (String) profile.getClass().getMethod("getName").invoke(profile);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
