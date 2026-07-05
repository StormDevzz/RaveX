package ravex.modules.player;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.ElytraUtility;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import java.util.List;
public class ElytraUtils extends Module {
    public static final ElytraUtils INSTANCE = new ElytraUtils();
    public final ModeParameter mode = new ModeParameter("Mode", "Swap", List.of("Swap", "Replace", "Auto"));
    public final ModeParameter swapMode = new ModeParameter("Swap Mode", "Positive1", List.of("Positive1", "Positive2", "Positive3"));
    public final NumberParameter minDurability = new NumberParameter("Min Durability", 10.0, 1.0, 50.0, 1.0);
    public final BooleanParameter preferBetter = new BooleanParameter("Prefer Better", true);
    private int state = 0, targetInvSlot = -1;
    private long lastActionTime = 0;

    private ElytraUtils() {
        super("ElytraUtils");
        swapMode.setVisible(() -> mode.getValue().equals("Swap"));
        minDurability.setVisible(() -> mode.getValue().equals("Replace") || mode.getValue().equals("Auto"));
        preferBetter.setVisible(() -> mode.getValue().equals("Replace") || mode.getValue().equals("Auto"));
    }
    @Override
    protected void onEnable() {
        if ("Swap".equals(mode.getValue())) initSwap();
    }
    private void initSwap() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null) { setEnabled(false); return; }
        boolean hasElytra = ElytraUtility.isElytraEquipped(p);
        int foundSlot = hasElytra ? ElytraUtility.findChestplateSlot(p) : ElytraUtility.findElytraSlot(p);
        if (foundSlot == -1) {
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("§7[§5ElytraUtils§7] §cNo replacement chest item found!"), false);
            setEnabled(false); return;
        }
        targetInvSlot = foundSlot;
        state = 0;
        lastActionTime = System.currentTimeMillis();
        String cm = swapMode.getValue();
        if ("Positive1".equals(cm)) {
            InventoryUtility.clickSlot(mc, p, foundSlot, 0, ClickType.PICKUP);
            clickChestSlot(mc, p, 6, ClickType.PICKUP);
            InventoryUtility.clickSlot(mc, p, foundSlot, 0, ClickType.PICKUP);
            setEnabled(false);
        } else if ("Positive3".equals(cm)) {
            mc.setScreen(new InventoryScreen(p));
        }
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null) { setEnabled(false); return; }
        String m = mode.getValue();
        if ("Swap".equals(m)) tickSwap(mc, p);
        else if ("Replace".equals(m) || "Auto".equals(m)) tickReplace(mc, p);
    }
    private void tickSwap(Minecraft mc, LocalPlayer p) {
        if ("Positive1".equals(swapMode.getValue())) return;
        long now = System.currentTimeMillis();
        if (now - lastActionTime < 100) return;
        if (state == 0) { InventoryUtility.clickSlot(mc, p, targetInvSlot, 0, ClickType.PICKUP); state = 1; lastActionTime = now; }
        else if (state == 1) { clickChestSlot(mc, p, 6, ClickType.PICKUP); state = 2; lastActionTime = now; }
        else if (state == 2) { InventoryUtility.clickSlot(mc, p, targetInvSlot, 0, ClickType.PICKUP); state = 3; lastActionTime = now; }
        else if (state == 3) { if ("Positive3".equals(swapMode.getValue())) mc.setScreen(null); setEnabled(false); }
    }
    private void tickReplace(Minecraft mc, LocalPlayer p) {
        if (!ElytraUtility.isElytraEquipped(p)) return;
        if (ElytraUtility.getElytraDurability(p) > minDurability.getValue().intValue()) return;
        int slot = ElytraUtility.findElytraSlot(p, preferBetter.getValue() ? minDurability.getValue().intValue() : 0);
        if (slot >= 0) {
            InventoryUtility.clickSlot(mc, p, slot, 0, ClickType.PICKUP);
            clickChestSlot(mc, p, 6, ClickType.PICKUP);
            InventoryUtility.clickSlot(mc, p, slot, 0, ClickType.PICKUP);
        }
    }
    private void clickChestSlot(Minecraft mc, LocalPlayer p, int containerSlot, ClickType type) {
        mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, containerSlot, 0, type, p);
    }
}
