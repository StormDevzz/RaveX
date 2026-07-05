package ravex.modules.combat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.core.component.DataComponents;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import java.util.ArrayList;
import java.util.List;
public class Quiver extends Module {
    public static final Quiver INSTANCE = new Quiver();
    public final ModeParameter arrowType = new ModeParameter("Arrow Type", "Speed", List.of("Healing", "Speed", "Strength", "Fire Resistance"));
    public final ModeParameter rotate = new ModeParameter("Rotate", "Silent", List.of("Silent", "Normal"));
    public final NumberParameter chargeDuration = new NumberParameter("Charge Ticks", 3.0, 2.0, 10.0, 1.0);
    public final BooleanParameter autoSwapBow = new BooleanParameter("Auto Swap Bow", true);
    public static float silentYaw = 0;
    public static float silentPitch = 0;
    public static boolean hasSilentRotations = false;
    private int state = 0; 
    private int ticksHolding = 0;
    private int cooldownTicks = 0;
    private int arrowInvSlot = -1;
    private int originalBowHotbarSlot = -1;
    private int previousSelectedSlot = -1;
    private float savedClientYaw = 0.0f;
    private float savedClientPitch = 0.0f;
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_quiver");
    static {
        NATIVE.load();
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (state == 1 && mc.player != null && mc.gameMode != null) {
            mc.player.releaseUsingItem();
            mc.gameMode.releaseUsingItem(mc.player);
            mc.options.keyUse.setDown(false);
            if (rotate.getValue().equals("Normal")) {
                mc.player.setYRot(savedClientYaw);
                mc.player.setXRot(savedClientPitch);
            }
            restoreOffhandAndBow(mc);
        }
        state = 0;
        ticksHolding = 0;
        cooldownTicks = 0;
        arrowInvSlot = -1;
        originalBowHotbarSlot = -1;
        previousSelectedSlot = -1;
        hasSilentRotations = false;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            onDisable();
            return;
        }
        if (state == 2) {
            cooldownTicks--;
            if (cooldownTicks <= 0) {
                state = 0;
            }
            return;
        }
        if (state == 1) {
            if (rotate.getValue().equals("Normal")) {
                mc.player.setXRot(-90.0f);
            } else {
                silentYaw = mc.player.getYRot();
                silentPitch = -90.0f;
                hasSilentRotations = true;
            }
            mc.options.keyUse.setDown(true);
            ticksHolding++;
            if (ticksHolding >= chargeDuration.getValue().intValue()) {
                mc.options.keyUse.setDown(false);
                mc.player.releaseUsingItem();
                mc.gameMode.releaseUsingItem(mc.player);
                restoreOffhandAndBow(mc);
                state = 2;
                cooldownTicks = 20;
                hasSilentRotations = false;
            }
            return;
        }
        int bowSlot = findBowSlot(mc);
        if (bowSlot == -1) {
            mc.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§7[§cQuiver§7] §cNo bow found in hotbar! Disabling..."),
                false
            );
            setEnabled(false);
            return;
        }
        int bestArrowIndex = findBestArrowIndex(mc);
        if (bestArrowIndex == -1) {
            mc.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§7[§cQuiver§7] §cNo arrows of type " + arrowType.getValue() + " found! Disabling..."),
                false
            );
            setEnabled(false);
            return;
        }
        arrowInvSlot = bestArrowIndex;
        previousSelectedSlot = mc.player.getInventory().getSelectedSlot();
        if (autoSwapBow.getValue() && previousSelectedSlot != bowSlot) {
            mc.player.getInventory().setSelectedSlot(bowSlot);
        }
        int containerSlot = arrowInvSlot < 9 ? arrowInvSlot + 36 : arrowInvSlot;
        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, mc.player);
        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 45, 0, ClickType.PICKUP, mc.player);
        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, mc.player);
        savedClientYaw = mc.player.getYRot();
        savedClientPitch = mc.player.getXRot();
        if (rotate.getValue().equals("Normal")) {
            mc.player.setXRot(-90.0f);
        } else {
            silentYaw = mc.player.getYRot();
            silentPitch = -90.0f;
            hasSilentRotations = true;
        }
        mc.options.keyUse.setDown(true);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        state = 1;
        ticksHolding = 0;
    }
    private void restoreOffhandAndBow(Minecraft mc) {
        if (arrowInvSlot != -1) {
            int containerSlot = arrowInvSlot < 9 ? arrowInvSlot + 36 : arrowInvSlot;
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 45, 0, ClickType.PICKUP, mc.player);
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, mc.player);
            arrowInvSlot = -1;
        }
        if (previousSelectedSlot != -1) {
            mc.player.getInventory().setSelectedSlot(previousSelectedSlot);
            previousSelectedSlot = -1;
        }
        if (rotate.getValue().equals("Normal") && mc.player != null) {
            mc.player.setYRot(savedClientYaw);
            mc.player.setXRot(savedClientPitch);
        }
    }
    private int findBowSlot(Minecraft mc) {
        if (mc.player.getMainHandItem().is(Items.BOW)) {
            return mc.player.getInventory().getSelectedSlot();
        }
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.BOW)) {
                return i;
            }
        }
        return -1;
    }
    private int findBestArrowIndex(Minecraft mc) {
        List<String> activeEffects = new ArrayList<>();
        List<Integer> activeAmps = new ArrayList<>();
        List<Double> activeDurs = new ArrayList<>();
        for (net.minecraft.world.effect.MobEffectInstance inst : mc.player.getActiveEffects()) {
            String id = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(inst.getEffect().value()).toString();
            activeEffects.add(id);
            activeAmps.add(inst.getAmplifier());
            activeDurs.add((double) inst.getDuration() / 20.0);
        }
        List<Integer> inventorySlots = new ArrayList<>();
        List<String> arrowEffects = new ArrayList<>();
        List<Integer> arrowAmplifiers = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.TIPPED_ARROW)) {
                net.minecraft.world.item.alchemy.PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
                if (contents != null) {
                    if (contents.potion().isPresent()) {
                        var potion = contents.potion().get().value();
                        for (net.minecraft.world.effect.MobEffectInstance inst : potion.getEffects()) {
                            inventorySlots.add(i);
                            arrowEffects.add(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(inst.getEffect().value()).toString());
                            arrowAmplifiers.add(inst.getAmplifier());
                        }
                    }
                    for (net.minecraft.world.effect.MobEffectInstance inst : contents.customEffects()) {
                        inventorySlots.add(i);
                        arrowEffects.add(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(inst.getEffect().value()).toString());
                        arrowAmplifiers.add(inst.getAmplifier());
                    }
                }
            }
        }
        if (arrowEffects.isEmpty()) return -1;
        String[] activeEffArr = activeEffects.toArray(new String[0]);
        int[] activeAmpArr = activeAmps.stream().mapToInt(Integer::intValue).toArray();
        double[] activeDurArr = activeDurs.stream().mapToDouble(Double::doubleValue).toArray();
        String[] arrowEffArr = arrowEffects.toArray(new String[0]);
        int[] arrowAmpArr = arrowAmplifiers.stream().mapToInt(Integer::intValue).toArray();
        int resultIdx;
        if (NATIVE.isLoaded()) {
            resultIdx = nativeSelectBestArrow(
                activeEffArr, activeAmpArr, activeDurArr,
                arrowEffArr, arrowAmpArr,
                arrowType.getValue()
            );
        } else {
            resultIdx = javaSelectBestArrow(
                activeEffects, activeAmps, activeDurs,
                arrowEffects, arrowAmplifiers,
                arrowType.getValue()
            );
        }
        if (resultIdx >= 0 && resultIdx < inventorySlots.size()) {
            return inventorySlots.get(resultIdx);
        }
        return -1;
    }
    private int javaSelectBestArrow(
        List<String> activeEffects,
        List<Integer> activeAmplifiers,
        List<Double> activeDurations,
        List<String> arrowEffects,
        List<Integer> arrowAmplifiers,
        String preferredType
    ) {
        int bestIndex = -1;
        double bestScore = -999.0;
        String pref = preferredType.toLowerCase();
        for (int i = 0; i < arrowEffects.size(); i++) {
            String eName = arrowEffects.get(i).toLowerCase();
            int amp = arrowAmplifiers.get(i);
            boolean match = false;
            double typeScore = 0.0;
            if (pref.equals("strength") && eName.contains("strength")) {
                match = true;
                typeScore = 1000.0;
            } else if (pref.equals("speed") && (eName.contains("swiftness") || eName.contains("speed"))) {
                match = true;
                typeScore = 800.0;
            } else if (pref.equals("healing") && (eName.contains("instant_health") || eName.contains("healing") || eName.contains("regeneration") || eName.contains("regen"))) {
                match = true;
                typeScore = 600.0;
            } else if (pref.equals("fire resistance") && (eName.contains("fire_resistance") || eName.contains("fireres"))) {
                match = true;
                typeScore = 400.0;
            }
            if (!match) continue;
            double score = typeScore + amp * 10.0;
            for (int j = 0; j < activeEffects.size(); j++) {
                String actEff = activeEffects.get(j).toLowerCase();
                int actAmp = activeAmplifiers.get(j);
                double actDur = activeDurations.get(j);
                boolean effMatch = actEff.equals(eName) || actEff.contains(eName) || eName.contains(actEff);
                if (effMatch && actAmp >= amp && actDur > 3.0) {
                    score -= 200.0;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex;
    }
    public static boolean hasSilentRotations() {
        return hasSilentRotations;
    }
    private static native int nativeSelectBestArrow(
        String[] activeEffects,
        int[] activeAmplifiers,
        double[] activeDurations,
        String[] arrowEffects,
        int[] arrowAmplifiers,
        String preferredType
    );
}
