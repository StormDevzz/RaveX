package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "Quiver", category = "Combat")
public class Quiver {
    @Parameter(name = "ArrowType", modes = {"Healing", "Speed", "Strength", "FireResistance"})
    public String arrowType = "Speed";
    @Parameter(name = "Rotate", modes = {"Silent", "Normal"})
    public String rotate = "Silent";
    @Parameter(name = "ChargeTicks", min = 2.0, max = 10.0, step = 1.0)
    public double chargeDuration = 3.0;
    @Parameter(name = "AutoSwapBow")
    public boolean autoSwapBow = true;
    public static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    private int state = 0;
    private int ticksHolding = 0;
    private int cooldownTicks = 0;
    private int arrowInvSlot = -1;
    private int originalBowHotbarSlot = -1;
    private int previousSelectedSlot = -1;
    private float savedClientYaw = 0.0f;
    private float savedClientPitch = 0.0f;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_quiver");
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (state == 1 && mc.getPlayer() != null && mc.getGameMode() != null) {
            mc.getPlayer().releaseUsingItem();
            mc.getGameMode().releaseUsingItem(mc.getPlayer());
            mc.getOptions().keyUse.setDown(false);
            if (rotate.equals("Normal")) {
                mc.getPlayer().setYRot(savedClientYaw);
                mc.getPlayer().setXRot(savedClientPitch);
            }
            restoreOffhandAndBow(mc);
        }
        state = 0;
        ticksHolding = 0;
        cooldownTicks = 0;
        arrowInvSlot = -1;
        originalBowHotbarSlot = -1;
        previousSelectedSlot = -1;
        silentRotation.hasRotation = false;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) {
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
            if (rotate.equals("Normal")) {
                mc.getPlayer().setXRot(-90.0f);
            } else {
                silentRotation.set(mc.getPlayer().getYRot(), -90.0f);
            }
            mc.getOptions().keyUse.setDown(true);
            ticksHolding++;
            if (ticksHolding >= (int) chargeDuration) {
                mc.getOptions().keyUse.setDown(false);
                mc.getPlayer().releaseUsingItem();
                mc.getGameMode().releaseUsingItem(mc.getPlayer());
                restoreOffhandAndBow(mc);
                state = 2;
                cooldownTicks = 20;
                silentRotation.hasRotation = false;
            }
            return;
        }
        int bowSlot = findBowSlot(mc);
        if (bowSlot == -1) {
            mc.getPlayer().displayClientMessage(
                net.minecraft.network.chat.Component.literal("§7[§cQuiver§7] §cNo bow found in hotbar! Disabling..."),
                false
            );
            Modules.setEnabled(Quiver.class, false);
            return;
        }
        int bestArrowIndex = findBestArrowIndex(mc);
        if (bestArrowIndex == -1) {
            mc.getPlayer().displayClientMessage(
                net.minecraft.network.chat.Component.literal("§7[§cQuiver§7] §cNo arrows of type " + arrowType + " found! Disabling..."),
                false
            );
            Modules.setEnabled(Quiver.class, false);
            return;
        }
        arrowInvSlot = bestArrowIndex;
        previousSelectedSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        if (autoSwapBow && previousSelectedSlot != bowSlot) {
            InventoryUtility.selectSlot(mc.getPlayer(), bowSlot);
        }
        InventoryUtility.swapToOffhand(mc, mc.getPlayer(), arrowInvSlot);
        savedClientYaw = mc.getPlayer().getYRot();
        savedClientPitch = mc.getPlayer().getXRot();
        if (rotate.equals("Normal")) {
            mc.getPlayer().setXRot(-90.0f);
        } else {
            silentRotation.set(mc.getPlayer().getYRot(), -90.0f);
        }
        mc.getOptions().keyUse.setDown(true);
        mc.getGameMode().useItem(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
        state = 1;
        ticksHolding = 0;
    }
    private void restoreOffhandAndBow(MinecraftWrapper mc) {
        if (arrowInvSlot != -1) {
            InventoryUtility.swapToOffhand(mc, mc.getPlayer(), arrowInvSlot);
            arrowInvSlot = -1;
        }
        if (previousSelectedSlot != -1) {
            InventoryUtility.selectSlot(mc.getPlayer(), previousSelectedSlot);
            previousSelectedSlot = -1;
        }
        if (rotate.equals("Normal") && mc.getPlayer() != null) {
            mc.getPlayer().setYRot(savedClientYaw);
            mc.getPlayer().setXRot(savedClientPitch);
        }
    }
    private int findBowSlot(MinecraftWrapper mc) {
        if (InventoryUtility.isBow(mc.getPlayer().getMainHandItem())) {
            return InventoryUtility.getSelectedSlot(mc.getPlayer());
        }
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.isBow(InventoryUtility.getItem(mc.getPlayer(), i))) {
                return i;
            }
        }
        return -1;
    }
    private int findBestArrowIndex(MinecraftWrapper mc) {
        List<String> activeEffects = new ArrayList<>();
        List<Integer> activeAmps = new ArrayList<>();
        List<Double> activeDurs = new ArrayList<>();
        for (net.minecraft.world.effect.MobEffectInstance inst : mc.getPlayer().getActiveEffects()) {
            String id = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(inst.getEffect().value()).toString();
            activeEffects.add(id);
            activeAmps.add(inst.getAmplifier());
            activeDurs.add((double) inst.getDuration() / 20.0);
        }
        List<Integer> inventorySlots = new ArrayList<>();
        List<String> arrowEffects = new ArrayList<>();
        List<Integer> arrowAmplifiers = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (InventoryUtility.isTippedArrow(stack)) {
                var contents = InventoryUtility.getPotionContents(stack);
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
                arrowType
            );
        } else {
            resultIdx = javaSelectBestArrow(
                activeEffects, activeAmps, activeDurs,
                arrowEffects, arrowAmplifiers,
                arrowType
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
            } else if (pref.equals("fireResistance") && (eName.contains("fire_resistance") || eName.contains("fireres"))) {
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
        return silentRotation.hasRotation;
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