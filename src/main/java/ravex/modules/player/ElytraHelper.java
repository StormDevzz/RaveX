package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.ElytraUtility;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.Minecraft;
import java.util.List;
@ModuleInfo(name = "ElytraHelper", category = "net.minecraft.world.entity.player.Player")
public class ElytraHelper extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Swap", List.of("Swap", "Replace", "Auto"));
    public final ModeParameter swapMode = new ModeParameter("SwapMode", "Positive1", List.of("Positive1", "Positive2", "Positive3"));
    public final NumberParameter minDurability = new NumberParameter("MinDurability", 10.0, 1.0, 50.0, 1.0);
    public final BooleanParameter preferBetter = new BooleanParameter("PreferBetter", true);

    public final ModeParameter rocketMode = new ModeParameter("RocketMode", "Off", List.of("Off", "Auto", "Boost"));
    public final NumberParameter rocketSpeed = new NumberParameter("RocketSpeed", 0.3, 0.05, 2.0, 0.05);
    public final NumberParameter rocketDelay = new NumberParameter("RocketDelay", 1500.0, 500.0, 5000.0, 100.0);

    public final BooleanParameter autoPitch = new BooleanParameter("AutoPitch", false);
    public final NumberParameter pitchAngle = new NumberParameter("PitchAngle", -45.0, -90.0, 90.0, 5.0);

    public final BooleanParameter chestSwapOnLand = new BooleanParameter("ChestSwapOnLand", false);
    private int state = 0, targetInvSlot = -1;
    private long lastActionTime = 0;
    private long lastRocketTime = 0;

    private ElytraHelper() {
        
        swapMode.setVisible(() -> mode.getValue().equals("Swap"));
        minDurability.setVisible(() -> mode.getValue().equals("Replace") || mode.getValue().equals("Auto"));
        preferBetter.setVisible(() -> mode.getValue().equals("Replace") || mode.getValue().equals("Auto"));
        rocketSpeed.setVisible(() -> !rocketMode.getValue().equals("Off"));
        rocketDelay.setVisible(() -> rocketMode.getValue().equals("Auto"));
        pitchAngle.setVisible(() -> autoPitch.getValue());
    }
    protected void onEnable() {
        if ("Swap".equals(mode.getValue())) initSwap();
    }
    private void initSwap() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.gameMode == null) { enabled = false; return; }
        boolean hasElytra = ElytraUtility.isElytraEquipped(p);
        int foundSlot = hasElytra ? ElytraUtility.findChestplateSlot(p) : ElytraUtility.findElytraSlot(p);
        if (foundSlot == -1) {
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("§7[§5ElytraHelper§7] §cNo replacement chest item found!"), false);
            enabled = false; return;
        }
        targetInvSlot = foundSlot;
        state = 0;
        lastActionTime = System.currentTimeMillis();
        String cm = swapMode.getValue();
        if ("Positive1".equals(cm)) {
            InventoryUtility.clickSlot(mc, p, foundSlot, 0, net.minecraft.world.inventory.ClickType.PICKUP);
            InventoryUtility.clickChestSlot(mc, p, 6, net.minecraft.world.inventory.ClickType.PICKUP);
            InventoryUtility.clickSlot(mc, p, foundSlot, 0, net.minecraft.world.inventory.ClickType.PICKUP);
            enabled = false;
        } else if ("Positive3".equals(cm)) {
            InventoryUtility.openInventoryScreen(p);
        }
    }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.gameMode == null) { enabled = false; return; }
        String m = mode.getValue();
        if ("Swap".equals(m)) tickSwap(mc, p);
        else if ("Replace".equals(m) || "Auto".equals(m)) tickReplace(mc, p);

        if (!rocketMode.getValue().equals("Off") && ElytraUtility.isFallFlying(p)) {
            double accel = rocketSpeed.getValue();
            net.minecraft.world.phys.Vec3 look = p.getLookAngle();
            net.minecraft.world.phys.Vec3 motion = p.getDeltaMovement();
            p.setDeltaMovement(
                motion.x + look.x * accel,
                motion.y + Math.abs(look.y) * accel * 0.5,
                motion.z + look.z * accel
            );
            if (rocketMode.getValue().equals("Auto")) {
                long now = System.currentTimeMillis();
                if (now - lastRocketTime >= rocketDelay.getValue().longValue()) {
                    if (ElytraUtility.useFirework(p)) lastRocketTime = now;
                }
            }
        }
        if (autoPitch.getValue() && ElytraUtility.isFallFlying(p)) {
            ElytraUtility.setPitch(p, pitchAngle.getValue().floatValue());
        }
        if (chestSwapOnLand.getValue() && !p.onGround() && ElytraUtility.isElytraEquipped(p)) {

        }
        if (chestSwapOnLand.getValue() && p.onGround() && !p.isFallFlying() && ElytraUtility.isElytraEquipped(p)) {
            ElytraUtility.swapToChestplate(p);
        }
    }
    private void tickSwap(Minecraft mc, net.minecraft.client.player.LocalPlayer p) {
        if ("Positive1".equals(swapMode.getValue())) return;
        long now = System.currentTimeMillis();
        if (now - lastActionTime < 100) return;
        if (state == 0) { InventoryUtility.clickSlot(mc, p, targetInvSlot, 0, net.minecraft.world.inventory.ClickType.PICKUP); state = 1; lastActionTime = now; }
        else if (state == 1) { InventoryUtility.clickChestSlot(mc, p, 6, net.minecraft.world.inventory.ClickType.PICKUP); state = 2; lastActionTime = now; }
        else if (state == 2) { InventoryUtility.clickSlot(mc, p, targetInvSlot, 0, net.minecraft.world.inventory.ClickType.PICKUP); state = 3; lastActionTime = now; }
        else if (state == 3) { if ("Positive3".equals(swapMode.getValue())) mc.setScreen(null); enabled = false; }
    }
    private void tickReplace(Minecraft mc, net.minecraft.client.player.LocalPlayer p) {
        if (!ElytraUtility.isElytraEquipped(p)) return;
        if (ElytraUtility.getElytraDurability(p) > minDurability.getValue().intValue()) return;
        int slot = ElytraUtility.findElytraSlot(p, preferBetter.getValue() ? minDurability.getValue().intValue() : 0);
        if (slot >= 0) {
            InventoryUtility.clickSlot(mc, p, slot, 0, net.minecraft.world.inventory.ClickType.PICKUP);
            InventoryUtility.clickChestSlot(mc, p, 6, net.minecraft.world.inventory.ClickType.PICKUP);
            InventoryUtility.clickSlot(mc, p, slot, 0, net.minecraft.world.inventory.ClickType.PICKUP);
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ElytraHelper").getEnabled();
    }
    public static ElytraHelper itz() {
        return ravex.manager.ModuleManager.delegate(ElytraHelper.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}