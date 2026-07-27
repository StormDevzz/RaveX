package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import ravex.utility.player.ContainerUtility;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "ChestHelper", category = "net.minecraft.world.entity.player.Player")
public class ChestHelper {
    @Parameter(name = "Steal")
    public boolean steal = true;
    @Parameter(name = "Dump")
    public boolean dump = true;
    @Parameter(name = "Fill")
    public boolean fill = true;
    @Parameter(name = "DropAll")
    public boolean dropAll = true;

    public void onRenderButtons(AbstractContainerScreen<?> screen, net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        if (!Modules.enabled(ChestHelper.class) || !ContainerUtility.isChestLike(screen.getMenu())) return;
        int startX = ContainerUtility.getButtonStartX(screen), startY = ContainerUtility.getButtonStartY(screen);
        List<ButtonDef> btns = getButtons();
        for (int i = 0; i < btns.size(); i++) {
            int bx = startX, by = startY + i * (ContainerUtility.CHEST_BTN_H + ContainerUtility.CHEST_BTN_GAP);
            ContainerUtility.drawChestButton(graphics, btns.get(i).label(), bx, by, ContainerUtility.isMouseOverButton(mouseX, mouseY, bx, by));
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
        var mc = MinecraftWrapper.getInstance();
        var player = mc.player;
        if (player == null) return;
        var menu = screen.getMenu();
        switch (action) {
            case "STEAL" -> ContainerUtility.quickMoveAll(ravex.mcwrapper.MinecraftWrapper.getWrapper(), player, ContainerUtility.getContainerSlots(menu));
            case "DUMP"  -> ContainerUtility.quickMoveAll(ravex.mcwrapper.MinecraftWrapper.getWrapper(), player, ContainerUtility.getPlayerSlots(menu));
            case "FILL"  -> ContainerUtility.fillFromContainer(ravex.mcwrapper.MinecraftWrapper.getWrapper(), player, menu);
            case "DROP"  -> ContainerUtility.throwAll(ravex.mcwrapper.MinecraftWrapper.getWrapper(), player, ContainerUtility.getContainerSlots(menu));
        }
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