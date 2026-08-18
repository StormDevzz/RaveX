package ravex.utility.misc;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
public class PotionUtility {
    public static String getPotionName(ItemStack stack) {
        if (stack.isEmpty()) return "Unknown";
        PotionContents contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (contents == null) {
            var name = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (name != null) {
                String path = name.getPath();
                if (path.equals("potion")) return "\u00a7fWater Bottle";
                if (path.equals("splash_potion")) return "\u00a7fSplash Water Bottle";
                if (path.equals("lingering_potion")) return "\u00a7fLingering Water Bottle";
                return "\u00a7f" + path.replace("_", " ");
            }
            return "\u00a7fUnknown";
        }
        var potionHolder = contents.potion();
        if (potionHolder != null && potionHolder.isPresent()) {
            Holder<Potion> holder = potionHolder.get();
            Identifier id = BuiltInRegistries.POTION.getKey(holder.value());
            String path = id != null ? id.getPath() : "unknown";
            String name = path.replace("_", " ");
            if (stack.is(Items.SPLASH_POTION)) return "\u00a7dSplash " + name;
            if (stack.is(Items.LINGERING_POTION)) return "\u00a7dLingering " + name;
            return "\u00a7d" + name;
        }
        return "\u00a7fUnknown Potion";
    }

    public static boolean hasSpeed(LivingEntity entity) {
        return entity.hasEffect(MobEffects.SPEED);
    }

    public static int getSpeedAmplifier(LivingEntity entity) {
        var effect = entity.getEffect(MobEffects.SPEED);
        return effect != null ? effect.getAmplifier() : 0;
    }

    public static boolean hasSlowness(LivingEntity entity) {
        return entity.hasEffect(MobEffects.SLOWNESS);
    }

    public static int getSlownessAmplifier(LivingEntity entity) {
        var effect = entity.getEffect(MobEffects.SLOWNESS);
        return effect != null ? effect.getAmplifier() : 0;
    }

    public static boolean hasBlindness(LivingEntity entity) {
        return entity.hasEffect(MobEffects.BLINDNESS);
    }

    public static int getResistanceAmplifier(LivingEntity entity) {
        var effect = entity.getEffect(MobEffects.RESISTANCE);
        return effect != null ? effect.getAmplifier() + 1 : 0;
    }

    public static int getWeaknessAmplifier(LivingEntity entity) {
        var effect = entity.getEffect(MobEffects.WEAKNESS);
        return effect != null ? effect.getAmplifier() + 1 : 0;
    }

    public static int getStrengthAmplifier(LivingEntity entity) {
        var effect = entity.getEffect(MobEffects.STRENGTH);
        return effect != null ? effect.getAmplifier() + 1 : 0;
    }

    public static double getArmorToughness(LivingEntity entity) {
        var attr = entity.getAttribute(Attributes.ARMOR_TOUGHNESS);
        return attr != null ? attr.getValue() : 0.0;
    }

    public static void setStepHeight(LivingEntity entity, double height) {
        var attr = entity.getAttribute(Attributes.STEP_HEIGHT);
        if (attr != null) {
            attr.setBaseValue(height);
        }
    }

    public static void resetStepHeight(LivingEntity entity) {
        setStepHeight(entity, 0.6);
    }

    public static void setEntityInteractionRange(LivingEntity entity, double range) {
        var attr = entity.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (attr != null) {
            attr.setBaseValue(range);
        }
    }

    public static void resetEntityInteractionRange(LivingEntity entity) {
        setEntityInteractionRange(entity, 3.0);
    }

    public static void setBlockInteractionRange(LivingEntity entity, double range) {
        var attr = entity.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (attr != null) {
            attr.setBaseValue(range);
        }
    }

    public static void resetBlockInteractionRange(LivingEntity entity) {
        setBlockInteractionRange(entity, 4.5);
    }

    public static int getArmorDefense(ItemAttributeModifiers modifiers, EquipmentSlot slot) {
        return modifiers != null ? (int) modifiers.compute(Attributes.ARMOR, 0.0, slot) : 0;
    }
}
