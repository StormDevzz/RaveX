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
}
