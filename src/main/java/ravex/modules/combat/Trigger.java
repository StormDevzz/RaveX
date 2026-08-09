package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.network.NetworkUtility;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "Trigger", category = "Combat")
public class Trigger {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "CPS", min = 1, max = 20, step = 1)
    public double cps = 10;

    @Parameter(name = "Targets", options = {"Players", "Monsters", "Passives", "Invisibles"})
    public List<String> targets = new ArrayList<>(List.of("Players"));

    @Parameter(name = "SmartCrits")
    public boolean smartCrits = false;

    @Parameter(name = "AutoWeapon")
    public boolean autoWeapon = false;
    @Parameter(name = "Swap", modes = {"Silent", "Normal", "None"})
    public String swapMode = "Silent";

    @Parameter(name = "ClickMode", modes = {"Hold", "Toggle"})
    public String clickMode = "Hold";

    private boolean toggled = false;
    private long lastAttackTime = 0;
    private net.minecraft.world.entity.LivingEntity currentTarget = null;

    public net.minecraft.world.entity.LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    public void onDisable() {
        toggled = false;
        currentTarget = null;
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) {
            currentTarget = null;
            return;
        }

        String clickModeVal = clickMode;
        boolean shouldAttack;
        if (clickModeVal.equals("Toggle")) {
            if (mc.getOptions().keyAttack.consumeClick()) toggled = !toggled;
            shouldAttack = toggled;
        } else {
            shouldAttack = mc.getOptions().keyAttack.isDown();
        }
        if (!shouldAttack) {
            currentTarget = null;
            return;
        }

        var target = EntityUtility.asLivingEntity(InventoryUtility.getHitEntity(mc));
        if (target == null || !EntityUtility.isAlive(target) || EntityUtility.isSelf(target) || EntityUtility.isArmorStand(target)) {
            currentTarget = null;
            return;
        }

        if (EntityUtility.distanceToPlayer(target) > range) {
            currentTarget = null;
            return;
        }

        if (!targets.contains("Invisibles") && target.isInvisible()) {
            currentTarget = null;
            return;
        }
        if (!targets.contains("Players") && EntityUtility.isPlayer(target)) {
            currentTarget = null;
            return;
        }
        if (!targets.contains("Monsters") && EntityUtility.isHostile(target)) {
            currentTarget = null;
            return;
        }
        if (!targets.contains("Passives") && EntityUtility.isPassive(target)) {
            currentTarget = null;
            return;
        }

        currentTarget = target;

        if (smartCrits && !mc.getPlayer().onGround()) {
            double velY = mc.getPlayer().getDeltaMovement().y;
            if (velY > -0.08) return;
        }

        long interval = 1000L / (long) (double) cps;
        if (System.currentTimeMillis() - lastAttackTime < interval) return;

        attack(mc, target);
        lastAttackTime = System.currentTimeMillis();
    }

    private void attack(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target) {
        if (autoWeapon) {
            int bestSlot = -1;
            double bestDmg = -1.0;
            for (int i = 0; i < 9; i++) {
                var stack = InventoryUtility.getItem(mc.getPlayer(), i);
                double dmg = getWeaponDamage(stack);
                if (dmg > bestDmg) {
                    bestDmg = dmg;
                    bestSlot = i;
                }
            }
            int originalSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
            if (bestSlot != -1 && bestSlot != originalSlot && bestDmg > 1.0) {
                if (swapMode.equals("Silent")) {
                    NetworkUtility.sendSetCarriedItem(bestSlot);
                } else if (swapMode.equals("Normal")) {
                    InventoryUtility.selectSlot(mc.getPlayer(), bestSlot);
                }
            }
            InventoryUtility.attackEntity(mc, target, "Server");
            if (bestSlot != -1 && bestSlot != originalSlot && bestDmg > 1.0) {
                if (swapMode.equals("Silent")) {
                    NetworkUtility.sendSetCarriedItem(originalSlot);
                } else if (swapMode.equals("Normal")) {
                    InventoryUtility.selectSlot(mc.getPlayer(), originalSlot);
                }
            }
        } else {
            InventoryUtility.attackEntity(mc, target, "Server");
        }
    }

    private double getWeaponDamage(net.minecraft.world.item.ItemStack stack) {
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
        return dmg;
    }


}
