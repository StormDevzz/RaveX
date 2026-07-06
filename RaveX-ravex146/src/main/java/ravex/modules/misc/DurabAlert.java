package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DurabAlert extends Module {
    public static final DurabAlert INSTANCE = new DurabAlert();

    public final ModeParameter mode = new ModeParameter("Mode", "Own", List.of("Own", "Enemy", "Both"));
    public final NumberParameter threshold = new NumberParameter("Threshold%", 10.0, 1.0, 100.0, 1.0);
    public final BooleanParameter sound = new BooleanParameter("Sound", true);

    private static final long COOLDOWN_MS = 30000;
    private final Map<String, Long> cooldowns = new HashMap<>();

    private DurabAlert() {
        super("DurabAlert", Category.MISC);
        addParameter(mode);
        addParameter(threshold);
        addParameter(sound);

    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        String curMode = mode.getValue();
        double thresh = threshold.getValue();

        if (curMode.equals("Own") || curMode.equals("Both")) {
            checkOwnArmor(mc, thresh);
        }
        if (curMode.equals("Enemy") || curMode.equals("Both")) {
            checkEnemyArmor(mc, thresh);
        }
    }

    private void checkOwnArmor(Minecraft mc, double thresh) {
        EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        String[] names = {"Boots", "Leggings", "Chestplate", "Helmet"};
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = mc.player.getItemBySlot(slots[i]);
            if (stack.isEmpty() || !stack.isDamageableItem()) continue;
            int maxDmg = stack.getMaxDamage();
            int curDmg = stack.getDamageValue();
            double pct = (double) (maxDmg - curDmg) / maxDmg * 100.0;
            if (pct < thresh) {
                alert(mc, "Own " + names[i], "Your " + names[i] + " is at " + String.format("%.0f", pct) + "% durability!");
            }
        }
    }

    private void checkEnemyArmor(Minecraft mc, double thresh) {
        if (mc.crosshairPickEntity instanceof LivingEntity living && !living.equals(mc.player)) {
            EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
            String[] names = {"Boots", "Leggings", "Chestplate", "Helmet"};
            for (int i = 0; i < slots.length; i++) {
                ItemStack stack = living.getItemBySlot(slots[i]);
                if (stack.isEmpty() || !stack.isDamageableItem()) continue;
                int maxDmg = stack.getMaxDamage();
                int curDmg = stack.getDamageValue();
                double pct = (double) (maxDmg - curDmg) / maxDmg * 100.0;
                if (pct < thresh) {
                    String entityName = living.getName().getString();
                    alert(mc, "Enemy " + names[i] + "@" + entityName,
                            entityName + "'s " + names[i] + " is at " + String.format("%.0f", pct) + "% durability!");
                }
            }
        }
    }

    private void alert(Minecraft mc, String cooldownKey, String message) {
        long now = System.currentTimeMillis();
        Long lastAlert = cooldowns.get(cooldownKey);
        if (lastAlert != null && (now - lastAlert) < COOLDOWN_MS) return;
        cooldowns.put(cooldownKey, now);

        ravex.utility.notification.NotificationManager.add("§e" + message, 0xFFFFCC33, 3000);
        if (sound.getValue()) {
            ravex.utility.sound.SoundUtility.playFailure();
        }
    }
}
