package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.PlayerUtility;
import ravex.utility.player.SwingUtility;

import ravex.utility.misc.food.FoodUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.network.NetworkUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "AutoApple", category = "Combat")
public class AutoApple {
    @Parameter(name = "Mode", modes = {"Default", "Grim"})
    public String mode = "Default";
    @Parameter(name = "AppleType", modes = {"Golden", "Enchanted", "Both"})
    public String appleType = "Both";
    @Parameter(name = "SwapMode", modes = {"Silent", "Normal"})
    public String swapMode = "Silent";
    @Parameter(name = "HealthThreshold", min = 1.0, max = 20.0, step = 0.5)
    public double healthThreshold = 10.0;
    @Parameter(name = "GrimDelay", min = 1.0, max = 20.0, step = 0.5, visible = "mode=Grim")
    public double grimDelay = 5.0;
    @Parameter(name = "GrimRandom", visible = "mode=Grim")
    public boolean grimRandom = true;
    private int originalSlot = -1;
    private boolean isEating = false;
    private int eatingSlot = -1;
    private int eatTicks = 0;
    private int grimDelayTicks = 0;

    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (isEating && mc.getPlayer() != null) {
            stopEating(mc);
        }
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        
        // Grim mode: add delay before eating
        if ("Grim".equals(mode) && !isEating) {
            grimDelayTicks++;
            int delayTicks = (int)(grimDelay * 20);
            if (grimRandom) {
                delayTicks += (int)(Math.random() * 10 - 5);
            }
            if (grimDelayTicks < delayTicks) {
                return;
            }
            grimDelayTicks = 0;
        }
        
        if (isEating) {
            eatTicks++;
            var currentStack = InventoryUtility.getItem(mc.getPlayer(), eatingSlot);
            if (!InventoryUtility.isItem(currentStack, "golden_apple") && !InventoryUtility.isItem(currentStack, "enchanted_golden_apple")) {
                stopEating(mc);
                return;
            }
            boolean finished = false;
            if (swapMode.equals("Normal")) {
                if (!PlayerUtility.isUsingItem(mc.getPlayer()) && eatTicks > 5) {
                    finished = true;
                }
            } else {
                if (eatTicks >= 33) {
                    finished = true;
                }
            }
            if (finished) {
                stopEating(mc);
                return;
            }
            if (swapMode.equals("Normal")) {
                mc.getOptions().keyUse.setDown(true);
            } else {
                NetworkUtility.sendUseItem(net.minecraft.world.InteractionHand.MAIN_HAND, mc.getPlayer().getYRot(), mc.getPlayer().getXRot());
            }
            return;
        }
        boolean shouldEat = javaFallbackShouldEat(
            PlayerUtility.getHealth(mc.getPlayer()),
            mc.getPlayer().getAbsorptionAmount(),
            healthThreshold
        );
        if (shouldEat) {
            int appleSlot = findAppleSlot(mc);
            if (appleSlot != -1) {
                startEating(mc, appleSlot);
            }
        }
    }
    private void startEating(MinecraftWrapper mc, int slot) {
        originalSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        eatingSlot = slot;
        isEating = true;
        eatTicks = 0;
        if (swapMode.equals("Normal")) {
            InventoryUtility.selectSlot(mc.getPlayer(), slot);
            mc.getGameMode().useItem(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
            mc.getOptions().keyUse.setDown(true);
        } else {
            if (mc.getPlayer().connection != null) {
                NetworkUtility.sendSetCarriedItem(slot);
                NetworkUtility.sendUseItem(net.minecraft.world.InteractionHand.MAIN_HAND, mc.getPlayer().getYRot(), mc.getPlayer().getXRot());
            }
        }
    }
    private void stopEating(MinecraftWrapper mc) {
        if (!isEating) return;
        if (swapMode.equals("Normal")) {
            mc.getOptions().keyUse.setDown(false);
            if (originalSlot != -1 && originalSlot >= 0 && originalSlot < 9) {
                InventoryUtility.selectSlot(mc.getPlayer(), originalSlot);
            }
        } else {
            if (mc.getPlayer().connection != null && originalSlot != -1 && originalSlot >= 0 && originalSlot < 9) {
                NetworkUtility.sendSetCarriedItem(originalSlot);
            }
        }
        isEating = false;
        eatingSlot = -1;
        originalSlot = -1;
        eatTicks = 0;
    }
    private int findAppleSlot(MinecraftWrapper mc) {
        boolean highDanger = (PlayerUtility.getHealth(mc.getPlayer()) + mc.getPlayer().getAbsorptionAmount()) <= 6.0;
        if (highDanger && !appleType.equals("Golden")) {
            FoodUtility.Data enchanted = FoodUtility.findEnchantedApple();
            if (enchanted != null) return enchanted.getSlot();
        }
        FoodUtility.Data best = FoodUtility.findApple(appleType);
        return best != null ? best.getSlot() : -1;
    }
    public static boolean javaFallbackShouldEat(
        double health,
        double absorption,
        double healthThreshold
    ) {
        double totalHealth = health + absorption;
        if (totalHealth <= healthThreshold) {
            return true;
        }
        if (totalHealth <= 6.0) {
            return true;
        }
        return false;
    }




}