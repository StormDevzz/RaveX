package ravex.modules.combat;
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.misc.food.FoodUtility;
import ravex.utility.player.InventoryUtility;
public class AutoApple extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Default",
            java.util.List.of("Default", "Grim"));
    public final ModeParameter appleType = new ModeParameter("AppleType", "Both",
            java.util.List.of("Golden", "Enchanted", "Both"));
    public final ModeParameter swapMode = new ModeParameter("SwapMode", "Silent",
            java.util.List.of("Silent", "Normal"));
    public final NumberParameter healthThreshold = new NumberParameter("HealthThreshold", 10.0, 1.0, 20.0, 0.5);
    public final NumberParameter grimDelay = new NumberParameter("GrimDelay", 5.0, 1.0, 20.0, 0.5);
    public final BooleanParameter grimRandom = new BooleanParameter("GrimRandom", true);
    private int originalSlot = -1;
    private boolean isEating = false;
    private int eatingSlot = -1;
    private int eatTicks = 0;
    private int grimDelayTicks = 0;

    private AutoApple() {
        super("AutoApple");
        grimDelay.setVisible(() -> "Grim".equals(mode.getValue()));
        grimRandom.setVisible(() -> "Grim".equals(mode.getValue()));
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (isEating && mc.player != null) {
            stopEating(mc);
        }
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        
        // Grim mode: add delay before eating
        if ("Grim".equals(mode.getValue()) && !isEating) {
            grimDelayTicks++;
            int delayTicks = (int)(grimDelay.getValue() * 20);
            if (grimRandom.getValue()) {
                delayTicks += (int)(Math.random() * 10 - 5);
            }
            if (grimDelayTicks < delayTicks) {
                return;
            }
            grimDelayTicks = 0;
        }
        
        if (isEating) {
            eatTicks++;
            var currentStack = InventoryUtility.getItem(mc.player, eatingSlot);
            if (!InventoryUtility.isItem(currentStack, "golden_apple") && !InventoryUtility.isItem(currentStack, "enchanted_golden_apple")) {
                stopEating(mc);
                return;
            }
            boolean finished = false;
            if (swapMode.getValue().equals("Normal")) {
                if (!mc.player.isUsingItem() && eatTicks > 5) {
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
            if (swapMode.getValue().equals("Normal")) {
                mc.options.keyUse.setDown(true);
            } else {
                if (mc.player.connection != null) {
                    mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
                }
            }
            return;
        }
        boolean shouldEat = javaFallbackShouldEat(
            mc.player.getHealth(),
            mc.player.getAbsorptionAmount(),
            healthThreshold.getValue()
        );
        if (shouldEat) {
            int appleSlot = findAppleSlot(mc);
            if (appleSlot != -1) {
                startEating(mc, appleSlot);
            }
        }
    }
    private void startEating(Minecraft mc, int slot) {
        originalSlot = InventoryUtility.getSelectedSlot(mc.player);
        eatingSlot = slot;
        isEating = true;
        eatTicks = 0;
        if (swapMode.getValue().equals("Normal")) {
            InventoryUtility.selectSlot(mc.player, slot);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.options.keyUse.setDown(true);
        } else {
            if (mc.player.connection != null) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
                mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
            }
        }
    }
    private void stopEating(Minecraft mc) {
        if (!isEating) return;
        if (swapMode.getValue().equals("Normal")) {
            mc.options.keyUse.setDown(false);
            if (originalSlot != -1 && originalSlot >= 0 && originalSlot < 9) {
                InventoryUtility.selectSlot(mc.player, originalSlot);
            }
        } else {
            if (mc.player.connection != null && originalSlot != -1 && originalSlot >= 0 && originalSlot < 9) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
            }
        }
        isEating = false;
        eatingSlot = -1;
        originalSlot = -1;
        eatTicks = 0;
    }
    private int findAppleSlot(Minecraft mc) {
        boolean highDanger = (mc.player.getHealth() + mc.player.getAbsorptionAmount()) <= 6.0;
        if (highDanger && !appleType.getValue().equals("Golden")) {
            FoodUtility.Data enchanted = FoodUtility.findEnchantedApple();
            if (enchanted != null) return enchanted.getSlot();
        }
        FoodUtility.Data best = FoodUtility.findApple(appleType.getValue());
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
    public static boolean maybeEnabled() {
        return maybeEnabled(AutoApple.class);
    }
    public static AutoApple itz() {
        return ModuleManager.get(AutoApple.class);
    }

}