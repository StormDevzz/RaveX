package ravex.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

public class AutoApple extends Module {
    public static final AutoApple INSTANCE = new AutoApple();

    public final ModeParameter appleType = new ModeParameter("Apple Type", "Both", 
            java.util.List.of("Golden", "Enchanted", "Both"));
    public final ModeParameter swapMode = new ModeParameter("Swap Mode", "Silent", 
            java.util.List.of("Silent", "Normal"));
    public final NumberParameter healthThreshold = new NumberParameter("Health Threshold", 10.0, 1.0, 20.0, 0.5);

    private int originalSlot = -1;
    private boolean isEating = false;
    private int eatingSlot = -1;
    private int eatTicks = 0;

    private static boolean nativeAvailable = false;

    static {
        try {
            System.loadLibrary("ravex_autoapple");
            nativeAvailable = true;
        } catch (UnsatisfiedLinkError e) {
            try {
                String libName = System.getProperty("os.name").toLowerCase().contains("win")
                        ? "ravex_autoapple.dll" : "libravex_autoapple.so";
                java.io.InputStream is = AutoApple.class.getResourceAsStream(
                        "/assets/ravex/natives/" + libName);
                if (is != null) {
                    java.nio.file.Path tmp = java.nio.file.Files.createTempFile("ravex_aa", "");
                    java.nio.file.Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.load(tmp.toAbsolutePath().toString());
                    tmp.toFile().deleteOnExit();
                    nativeAvailable = true;
                }
            } catch (Throwable ignored) {}
        }
    }

    private AutoApple() {
        super("AutoApple", Category.COMBAT);
        addParameter(appleType);
        addParameter(swapMode);
        addParameter(healthThreshold);
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

        // If currently eating, update/handle progress
        if (isEating) {
            eatTicks++;
            ItemStack currentStack = mc.player.getInventory().getItem(eatingSlot);
            if (!isApple(currentStack)) {
                stopEating(mc);
                return;
            }

            // In normal swap mode, we can monitor mc.player.isUsing().
            // In silent swap mode, we count ticks or look for stack decrease.
            boolean finished = false;
            if (swapMode.getValue().equals("Normal")) {
                if (!mc.player.isUsingItem() && eatTicks > 5) {
                    finished = true;
                }
            } else {
                // Silent swap: usually takes 32 ticks. Let's add buffer.
                if (eatTicks >= 33) {
                    finished = true;
                }
            }

            if (finished) {
                stopEating(mc);
                return;
            }

            // Keep eating
            if (swapMode.getValue().equals("Normal")) {
                mc.options.keyUse.setDown(true);
            } else {
                if (mc.player.connection != null) {
                    mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
                }
            }
            return;
        }

        // Check dangerous circumstances
        boolean shouldEat = false;
        if (nativeAvailable) {
            shouldEat = nativeShouldEat(
                mc.player.getHealth(),
                mc.player.getAbsorptionAmount(),
                healthThreshold.getValue()
            );
        } else {
            shouldEat = javaFallbackShouldEat(
                mc.player.getHealth(),
                mc.player.getAbsorptionAmount(),
                healthThreshold.getValue()
            );
        }

        if (shouldEat) {
            int appleSlot = findAppleSlot(mc);
            if (appleSlot != -1) {
                startEating(mc, appleSlot);
            }
        }
    }

    private void startEating(Minecraft mc, int slot) {
        originalSlot = mc.player.getInventory().getSelectedSlot();
        eatingSlot = slot;
        isEating = true;
        eatTicks = 0;

        if (swapMode.getValue().equals("Normal")) {
            mc.player.getInventory().setSelectedSlot(slot);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.options.keyUse.setDown(true);
        } else {
            // Silent swap: send slot and start using packet
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
                mc.player.getInventory().setSelectedSlot(originalSlot);
            }
        } else {
            // Silent swap restore
            if (mc.player.connection != null && originalSlot != -1 && originalSlot >= 0 && originalSlot < 9) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
            }
        }

        isEating = false;
        eatingSlot = -1;
        originalSlot = -1;
        eatTicks = 0;
    }

    private boolean isApple(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String mode = appleType.getValue();
        if (mode.equals("Golden")) {
            return stack.is(Items.GOLDEN_APPLE);
        } else if (mode.equals("Enchanted")) {
            return stack.is(Items.ENCHANTED_GOLDEN_APPLE);
        } else {
            return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
        }
    }

    private int findAppleSlot(Minecraft mc) {
        // High danger: prioritize enchanted gold apple if health is below 6.0
        boolean highDanger = (mc.player.getHealth() + mc.player.getAbsorptionAmount()) <= 6.0;
        
        if (highDanger && (appleType.getValue().equals("Both") || appleType.getValue().equals("Enchanted"))) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                    return i;
                }
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (isApple(stack)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }

    public static native boolean nativeShouldEat(
        double health,
        double absorption,
        double healthThreshold
    );

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
