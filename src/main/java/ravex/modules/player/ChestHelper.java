package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import ravex.utility.player.ContainerUtility;
import ravex.utility.player.InventoryUtility;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "ChestHelper", category = "Player")
public class ChestHelper {
    @Parameter(name = "Steal")
    public boolean steal = true;
    @Parameter(name = "StealDelay", min = 0.0, max = 20.0, step = 1.0, visible = "steal")
    public double stealDelay = 1.0;
    @Parameter(name = "Dump")
    public boolean dump = true;
    @Parameter(name = "DumpDelay", min = 0.0, max = 20.0, step = 1.0, visible = "dump")
    public double dumpDelay = 1.0;
    @Parameter(name = "Fill")
    public boolean fill = true;
    @Parameter(name = "DropAll")
    public boolean dropAll = true;
    @Parameter(name = "ButtonStyle", modes = {"Classic", "New", "CustomColor"})
    public String buttonStyle = "Classic";
    @Parameter(name = "ButtonColor", color = true, visible = "buttonStyle=CustomColor")
    public int buttonColor = 0xFF6C6C6C;
    private final List<Integer> moveQueue = new ArrayList<>();
    private int moveContainerId = -1;
    private int moveCooldown = 0;
    private double moveDelay = 0.0;

    public void onRenderButtons(AbstractContainerScreen<?> screen, net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        if (!Modules.enabled(ChestHelper.class) || !ContainerUtility.isChestLike(screen.getMenu())) return;
        int startX = ContainerUtility.getButtonStartX(screen), startY = ContainerUtility.getButtonStartY(screen);
        List<ButtonDef> btns = getButtons();
        boolean lmbDown = false;
        try {
            lmbDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(MinecraftWrapper.getWrapper().getWindowHandle(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        } catch (Exception ignored) {}
        boolean styled = "New".equals(buttonStyle);
        boolean custom = "CustomColor".equals(buttonStyle);
        for (int i = 0; i < btns.size(); i++) {
            int bx = startX, by = startY + i * (ContainerUtility.CHEST_BTN_H + ContainerUtility.CHEST_BTN_GAP);
            boolean hovered = ContainerUtility.isMouseOverButton(mouseX, mouseY, bx, by);
            if (styled)
                ContainerUtility.drawChestButtonNew(graphics, btns.get(i).label(), btns.get(i).action(), bx, by, hovered, hovered && lmbDown);
            else if (custom)
                ContainerUtility.drawChestButtonCustom(graphics, btns.get(i).label(), buttonColor, bx, by, hovered, hovered && lmbDown);
            else
                ContainerUtility.drawChestButton(graphics, btns.get(i).label(), bx, by, hovered);
        }
    }
    public boolean onMouseClicked(AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        if (!Modules.enabled(ChestHelper.class) || !ContainerUtility.isChestLike(screen.getMenu())) return false;
        int startX = ContainerUtility.getButtonStartX(screen), startY = ContainerUtility.getButtonStartY(screen);
        List<ButtonDef> btns = getButtons();
        for (int i = 0; i < btns.size(); i++) {
            int by = startY + i * (ContainerUtility.CHEST_BTN_H + ContainerUtility.CHEST_BTN_GAP);
            if (ContainerUtility.isMouseOverButton(mouseX, mouseY, startX, by)) {
                handleAction(screen, btns.get(i).action()); return true;
            }
        }
        return false;
    }
    private void handleAction(AbstractContainerScreen<?> screen, String action) {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        var menu = screen.getMenu();
        switch (action) {
            case "STEAL" -> queueOrMove(menu, ContainerUtility.getContainerSlots(menu), stealDelay);
            case "DUMP"  -> queueOrMove(menu, ContainerUtility.getPlayerSlots(menu), dumpDelay);
            case "FILL"  -> ContainerUtility.fillFromContainer(ravex.mcwrapper.MinecraftWrapper.getWrapper(), player, menu);
            case "DROP"  -> ContainerUtility.throwAll(ravex.mcwrapper.MinecraftWrapper.getWrapper(), player, ContainerUtility.getContainerSlots(menu));
        }
    }
    private void queueOrMove(net.minecraft.world.inventory.AbstractContainerMenu menu, List<net.minecraft.world.inventory.Slot> slots, double delay) {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        if (delay <= 0) {
            ContainerUtility.quickMoveAll(mc, player, slots);
            return;
        }
        moveQueue.clear();
        for (var slot : slots) {
            if (slot.hasItem()) moveQueue.add(slot.index);
        }
        moveContainerId = menu.containerId;
        moveDelay = delay;
        moveCooldown = 0;
    }
    public void onTick() {
        if (moveQueue.isEmpty()) return;
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) { moveQueue.clear(); return; }
        var screen = mc.getCurrentScreen();
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) { moveQueue.clear(); return; }
        if (containerScreen.getMenu().containerId != moveContainerId) { moveQueue.clear(); return; }
        if (moveCooldown > 0) { moveCooldown--; return; }
        int slotIndex = moveQueue.remove(0);
        var slot = containerScreen.getMenu().getSlot(slotIndex);
        if (slot != null && slot.hasItem())
            InventoryUtility.quickMoveSlot(mc, moveContainerId, slotIndex);
        moveCooldown = Math.max(0, (int) moveDelay);
    }
    private List<ButtonDef> getButtons() {
        List<ButtonDef> list = new ArrayList<>();
        if (steal)   list.add(new ButtonDef("Steal", "STEAL"));
        if (dump)    list.add(new ButtonDef("Dump", "DUMP"));
        if (fill)    list.add(new ButtonDef("Fill", "FILL"));
        if (dropAll) list.add(new ButtonDef("Drop", "DROP"));
        return list;
    }
    record ButtonDef(String label, String action) {}




}